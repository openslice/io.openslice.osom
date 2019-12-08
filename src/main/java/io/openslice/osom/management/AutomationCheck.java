/*-
 * ========================LICENSE_START=================================
 * io.openslice.osom
 * %%
 * Copyright (C) 2019 openslice.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.openslice.osom.management;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.tmf.common.model.Any;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.common.model.service.ServiceRelationship;
import io.openslice.tmf.common.model.service.ServiceSpecificationRef;
import io.openslice.tmf.common.model.service.ServiceStateType;
import io.openslice.tmf.prm669.model.RelatedParty;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristic;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristicValue;
import io.openslice.tmf.scm633.model.ServiceSpecRelationship;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceCreate;
import io.openslice.tmf.sim638.model.ServiceOrderRef;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderItem;

/**
 * @author ctranoris
 *
 */
@Component(value = "automationCheck") // bean name
public class AutomationCheck implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(AutomationCheck.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;

	public void execute(DelegateExecution execution) {

		logger.info("Process Orders by Orchetrator:" + execution.getVariableNames().toString());

		if (execution.getVariable("orderid") instanceof String) {
			logger.info("Will process/orchestrate order with id = " + execution.getVariable("orderid"));
			ServiceOrder sor = serviceOrderManager.retrieveServiceOrder((String) execution.getVariable("orderid"));

			if (sor == null) {
				logger.error("Cannot retrieve Service Order details from catalog.");
				return;
			}

			logger.debug("ServiceOrder id:" + sor.getId());
			logger.debug("Examin service items");
			List<String> serviceSpecsManual = new ArrayList<>();
			List<String> serviceSpecsAutomated = new ArrayList<>();

			for (ServiceOrderItem soi : sor.getOrderItem()) {
				logger.debug("Service Item ID:" + soi.getId());
				logger.debug("Service spec ID:" + soi.getService().getServiceSpecification().getId());

				// get service spec by id from model via bus, find if bundle and analyse its
				// related services
				ServiceSpecification spec = serviceOrderManager
						.retrieveSpec(soi.getService().getServiceSpecification().getId());
				createServiceByServiceSpec(sor, soi, spec, "5");

				if (spec != null)
					logger.debug("Retrieved Service ID:" + spec.getId());
				logger.debug("Retrieved Service Name:" + spec.getName());

				logger.debug("<--------------- related specs -------------->");
				for (ServiceSpecRelationship specRels : spec.getServiceSpecRelationship()) {
					logger.debug("\tService specRelsId:" + specRels.getId());
					ServiceSpecification specrel = serviceOrderManager.retrieveSpec(specRels.getId());
					logger.debug("\tService spec name :" + specrel.getName());
					logger.debug("\tService spec type :" + specrel.getType());
					if (specrel.getType().equals("CustomerFacingServiceSpecification")) {
						createServiceByServiceSpec(sor, soi, specrel, "4");
						//serviceSpecsManual.add(specrel.getId()); this is wrong..we need to add service IDs not serviceSpecs
					} else {
						createServiceByServiceSpec(sor, soi, specrel, "1");
						//serviceSpecsAutomated.add(specrel.getId());
					}

				}
				logger.debug("<--------------- /related specs -------------->");


			}

			//execution.setVariable("serviceSpecsManual", serviceSpecsManual);

		}
	}

	/**
	 * @param sor
	 * @param soi 
	 * @param spec
	 */
	private void createServiceByServiceSpec(ServiceOrder sor, ServiceOrderItem soi, ServiceSpecification spec, String startMode) {
		ServiceCreate s = new ServiceCreate();
		s.setCategory(spec.getType());
		s.setType(spec.getType());
		s.setDescription("A Service for " + spec.getName());
		s.setServiceDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
		s.hasStarted(false);
		s.setIsServiceEnabled(false);
		s.setName(spec.getName());
		s.setStartMode( startMode );
		
		Note noteItem = new Note();
		noteItem.setText("Service Created by OSOM:AutomationCheck");
		s.addNoteItem(noteItem);
		
		ServiceOrderRef serviceOrderref = new ServiceOrderRef();
		serviceOrderref.setId( sor.getId() );
		serviceOrderref.setServiceOrderItemId( soi.getId() );
		s.addServiceOrderItem(serviceOrderref );
		
		ServiceSpecificationRef serviceSpecificationRef = new ServiceSpecificationRef();
		serviceSpecificationRef.setId( spec.getId());
		serviceSpecificationRef.setName(spec.getName());
		s.setServiceSpecificationRef(serviceSpecificationRef );
		
		s.setServiceType( spec.getName());
		s.setState( ServiceStateType.RESERVED );
		
		if (spec.getRelatedParty()!=null) {
			for (RelatedParty rp : spec.getRelatedParty()) {
				s.addRelatedPartyItem(rp);
			}			
		}
		
		if (soi.getService().getServiceCharacteristic() != null ) {
			for (ServiceSpecCharacteristic c : spec.getServiceSpecCharacteristic()) {
				
				for (Characteristic orderCharacteristic : soi.getService().getServiceCharacteristic()) {
					if ( orderCharacteristic.getName().equals( c.getName()) ) { //copy only characteristics that are related from the order
						Characteristic serviceCharacteristicItem =  new Characteristic();
						serviceCharacteristicItem.setName( c.getName() );
						serviceCharacteristicItem.setValueType( c.getValueType() );
									
						Any val = new Any();
						val.setValue( orderCharacteristic.getValue().getValue() );
						val.setAlias( orderCharacteristic.getValue().getAlias() );
						
						serviceCharacteristicItem.setValue( val );
						s.addServiceCharacteristicItem(serviceCharacteristicItem);
					}
				}
			}
			
		}
		
		
		serviceOrderManager.createService(s, sor, spec);
	}

}
