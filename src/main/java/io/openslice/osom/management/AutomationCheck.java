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
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
import io.openslice.tmf.common.model.UserPartRoleType;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.common.model.service.ServiceRef;
import io.openslice.tmf.common.model.service.ServiceRelationship;
import io.openslice.tmf.common.model.service.ServiceSpecificationRef;
import io.openslice.tmf.common.model.service.ServiceStateType;
import io.openslice.tmf.prm669.model.RelatedParty;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristic;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristicValue;
import io.openslice.tmf.scm633.model.ServiceSpecRelationship;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.EServiceStartMode;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceCreate;
import io.openslice.tmf.sim638.model.ServiceOrderRef;
import io.openslice.tmf.sim638.model.ServiceUpdate;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderActionType;
import io.openslice.tmf.so641.model.ServiceOrderItem;
import io.openslice.tmf.so641.model.ServiceOrderStateType;
import io.openslice.tmf.so641.model.ServiceOrderUpdate;
import io.openslice.tmf.so641.model.ServiceRestriction;
import liquibase.change.core.AddAutoIncrementChange;

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
			logger.debug("Examine service items");
			List<String> servicesHandledManual = new ArrayList<>();
			List<String> servicesHandledByNFVOAutomated = new ArrayList<>();
			List<String> servicesHandledByExternalSP = new ArrayList<>();
			List<String> servicesLocallyAutomated = new ArrayList<>();
			
			

			for (ServiceOrderItem soi : sor.getOrderItem()) {
				logger.debug("Service Item ID:" + soi.getId());
				logger.debug("Service spec ID:" + soi.getService().getServiceSpecification().getId());
				logger.debug("Service Item Action:" + soi.getAction().toString() );
				
				if ( soi.getAction().equals(  ServiceOrderActionType.ADD   ) ) {					
					// get service spec by id from model via bus, find if bundle and analyze its
					// related services
					ServiceSpecification spec = serviceOrderManager
							.retrieveServiceSpec(soi.getService().getServiceSpecification().getId());
					
					logger.debug("Retrieved Service ID:" + spec.getId());
					logger.debug("Retrieved Service Name:" + spec.getName());
					

					//this is a main underlying service for the requested service (restriction)					
					Service createdUnderlService = addServicesToVariables( spec, sor, soi, servicesHandledByExternalSP, servicesHandledManual, servicesHandledByNFVOAutomated, servicesLocallyAutomated, null );
					
					//List<Service> createdServices = new ArrayList<>();
					
					logger.debug("<--------------- related specs -------------->");
					for (ServiceSpecRelationship specRels : spec.getServiceSpecRelationship()) {
						logger.debug("\tService specRelsId:" + specRels.getId());
						
						ServiceSpecification specrel = serviceOrderManager.retrieveServiceSpec(specRels.getId());
						addServicesToVariables(specrel, sor, soi, servicesHandledByExternalSP, servicesHandledManual, servicesHandledByNFVOAutomated, servicesLocallyAutomated, createdUnderlService );
						

					}
					

					soi.getService().setState( ServiceStateType.RESERVED );
					soi.setState(ServiceOrderStateType.INPROGRESS);
					logger.debug("<--------------- /related specs -------------->");					
				}else if ( soi.getAction().equals(  ServiceOrderActionType.MODIFY    ) ) {	
					
					
					ServiceRestriction refservice = soi.getService();
					
					if ( soi.getState().equals(  ServiceOrderStateType.ACKNOWLEDGED    ) ) {
						
						
						if ( refservice.getState().equals(  ServiceStateType.INACTIVE) 
								||  refservice.getState().equals(  ServiceStateType.TERMINATED)) {
						
							
							for (ServiceRef sref : soi.getService().getSupportingService() ) {
								ServiceUpdate supd = new ServiceUpdate();
								supd.setState( ServiceStateType.TERMINATED );
								serviceOrderManager.updateService( sref.getId(), supd , true);
							}
						}		
						else {
							

							//na doume to modify (me change characteristics apo to service restriction kai to terminate)
							//copy characteristics values from Service restriction to supporting services.
							for (ServiceRef sref : soi.getService().getSupportingService() ) {
								Service aService = serviceOrderManager.retrieveService( sref.getId() );
								ServiceUpdate supd = new ServiceUpdate();
								
								if ( soi.getService().getServiceCharacteristic() != null ) {
									for (Characteristic serviceChar : aService.getServiceCharacteristic() ) {
										
										for (Characteristic soiCharacteristic : soi.getService().getServiceCharacteristic()) {
											if ( soiCharacteristic.getName().contains( serviceChar.getName() )) { //copy only characteristics that are related from the order
												
												serviceChar.setValue( soiCharacteristic.getValue() );
												supd.addServiceCharacteristicItem( serviceChar );
												
												
											}
										}
									}
									
								}
								

								serviceOrderManager.updateService( aService.getId(), supd , true); //update the service
							}
						 }
						
					}
					
					
					soi.setState(ServiceOrderStateType.INPROGRESS);
					soi.setAction( ServiceOrderActionType.NOCHANGE ); //reset the action to NOCHANGE
					
				}else if ( soi.getAction().equals(  ServiceOrderActionType.DELETE    ) ) {
					
					/**
					 * we will terminate the services
					 */
					if ( soi.getState().equals(  ServiceOrderStateType.ACKNOWLEDGED    ) ) {

						for (ServiceRef sref : soi.getService().getSupportingService() ) {
							ServiceUpdate supd = new ServiceUpdate();
							supd.setState( ServiceStateType.TERMINATED );
							serviceOrderManager.updateService( sref.getId(), supd , true);
						}
						
					}
					
					
					
					soi.setState(ServiceOrderStateType.INPROGRESS);
					soi.setAction( ServiceOrderActionType.NOCHANGE ); //reset the action to NOCHANGE	

					
				}
			}
			
			

			execution.setVariable("servicesHandledByExternalSP", servicesHandledByExternalSP);
			execution.setVariable("servicesHandledManual", servicesHandledManual);
			execution.setVariable("servicesHandledByNFVOAutomated", servicesHandledByNFVOAutomated);
			execution.setVariable("servicesLocallyAutomated", servicesLocallyAutomated);
			
			
			

			logger.debug("servicesHandledManual: " + servicesHandledManual.toString());
			logger.debug("servicesHandledByNFVOAutomated: " + servicesHandledByNFVOAutomated.toString());
			logger.debug("servicesHandledByExternalSP: " + servicesHandledByExternalSP.toString());
			logger.debug("servicesLocallyAutomated: " + servicesLocallyAutomated.toString());
			
			/***
			 * we can update now the serviceorder element in catalog
			 * Update also the related service attributes
			 */
			
			ServiceOrderUpdate serviceOrderUpd = new ServiceOrderUpdate();
			for (ServiceOrderItem orderItemItem : sor.getOrderItem()) {
				orderItemItem.getService().setName( orderItemItem.getService().getServiceSpecification().getName() );
				orderItemItem.getService().setCategory( orderItemItem.getService().getServiceSpecification().getType() );
				//orderItemItem.getService().setState( ServiceStateType.RESERVED );
				//orderItemItem.setAction( ServiceOrderActionType.NOCHANGE ); //reset the action to NOCHANGE	
							
				serviceOrderUpd.addOrderItemItem(orderItemItem);
			}
			
			
			serviceOrderManager.updateServiceOrderOrder( sor.getId(), serviceOrderUpd );
			
		}
	}
	
	
	
	/**
	 * 
	 * This method decides the kind of Automation to be applied for a requested Service 
	 * e.g. Manual, Automated, Handled by NFVO, Handled by External partner.
	 * It creates an underlying service definition that needs to be managed next by various Activities/Processes 
	 * 
	 * 
	 * @param specrel
	 * @param sor
	 * @param soi
	 * @param servicesHandledByExternalSP
	 * @param servicesHandledManual
	 * @param servicesHandledByNFVOAutomated
	 * @param servicesLocallyAutomated
	 * @param parentService 
	 * @return 
	 */
	private Service addServicesToVariables(ServiceSpecification specrel, 
			ServiceOrder sor, ServiceOrderItem soi, 
			List<String> servicesHandledByExternalSP,
			List<String> servicesHandledManual,
			List<String> servicesHandledByNFVOAutomated,
			List<String> servicesLocallyAutomated, Service parentService) {
		
		logger.debug("\tService spec name :" + specrel.getName());
		logger.debug("\tService spec type :" + specrel.getType());
		
		Service createdServ = null;
		RelatedParty partnerOrg = fromPartnerOrganization( specrel );
		
		
		if ( partnerOrg != null  ) {
			createdServ = createServiceByServiceSpec(sor, soi, specrel, EServiceStartMode.AUTOMATICALLY_MANAGED, partnerOrg);
			if ( createdServ!=null ) {
				servicesHandledByExternalSP.add(createdServ.getId());
			}				
			
		} else if ( specrel.getType().equals("CustomerFacingServiceSpecification") && (specrel.isIsBundle()!=null) && specrel.isIsBundle() ) {
			createdServ = createServiceByServiceSpec(sor, soi, specrel, EServiceStartMode.AUTOMATICALLY_MANAGED, null);			
			if ( createdServ!=null ) {
				servicesLocallyAutomated.add(createdServ.getId());
			}
		} else if ( specrel.getType().equals("CustomerFacingServiceSpecification") && (specrel.findSpecCharacteristicByName("OSAUTOMATED") != null )  ) {
			createdServ = createServiceByServiceSpec(sor, soi, specrel, EServiceStartMode.AUTOMATICALLY_MANAGED, null);			
			if ( createdServ!=null ) {
				servicesLocallyAutomated.add(createdServ.getId());
			}
		}	
		else if (specrel.getType().equals("ResourceFacingServiceSpecification")) {
			createdServ = createServiceByServiceSpec(sor, soi, specrel, EServiceStartMode.AUTOMATICALLY_MANAGED, null);
			if ( createdServ!=null ) {
				if ( specrel.findSpecCharacteristicByName( "NSDID" ) != null ){
					servicesHandledByNFVOAutomated.add(createdServ.getId());						
				} else {
					servicesLocallyAutomated.add(createdServ.getId());
				}
				
			}
		}		
		else {
			createdServ = createServiceByServiceSpec(sor, soi, specrel, EServiceStartMode.MANUALLY_BY_SERVICE_PROVIDER, null);
			if ( createdServ!=null ) {
				servicesHandledManual.add(createdServ.getId());							
			}
		}		
		
		
		
		
		//add now the serviceRef
		if ( createdServ!=null ) {
			ServiceRef supportingServiceRef = new ServiceRef();
			supportingServiceRef.setId( createdServ.getId() );
			supportingServiceRef.setReferredType( createdServ.getName() );
			supportingServiceRef.setName( createdServ.getName()  );
			soi.getService().addSupportingServiceItem(supportingServiceRef );			
			
			if ( parentService!= null) {
				addCreatedServiceAsSupportingServiceToParent( parentService, supportingServiceRef );				
			}
			
			
			return createdServ;
			
		} else {
			logger.error("Service was not created for spec: " + specrel.getName());
		}
		
		return null;
		
	}

	/**
	 * 
	 * Adds a supportingServiceRef to the Supporting Services of createdUnderlService
	 * 
	 * @param createdUnderlService
	 * @param supportingServiceRef
	 */
	private void addCreatedServiceAsSupportingServiceToParent(Service createdUnderlService,
			ServiceRef supportingServiceRef) {


		createdUnderlService.addSupportingServiceItem(supportingServiceRef);
		
		ServiceUpdate supd = new ServiceUpdate();
		
		for (ServiceRef existingSupportingServices : createdUnderlService.getSupportingService()  ) {
			supd.addSupportingServiceItem(existingSupportingServices);			
		}
		
		serviceOrderManager.updateService( createdUnderlService.getId() , supd, false);
		
	}



	private RelatedParty fromPartnerOrganization(ServiceSpecification specrel) {
		if ( specrel.getRelatedParty() != null ) {
			for (RelatedParty rp : specrel.getRelatedParty()) {
				if ( rp.getRole().equals( UserPartRoleType.ORGANIZATION.getValue() )) {
					return rp;					
				}				
			}			
		}
		return null;
	}

	/**
	 * @param sor
	 * @param soi 
	 * @param spec
	 * @return 
	 */
	private Service createServiceByServiceSpec(ServiceOrder sor, ServiceOrderItem soi,
			ServiceSpecification spec, EServiceStartMode startMode, RelatedParty partnerOrg) {

		ServiceCreate s = new ServiceCreate();
		String servicename = spec.getName();
		s.setDescription("A Service for " + spec.getName());
		if ( partnerOrg!= null ) {
			servicename = partnerOrg.getName() + "::" +  servicename;
			s.setDescription("A Service for " + spec.getName() + " offered by external partner: " + partnerOrg.getName());
		}

		s.setName( servicename );
		s.setCategory(spec.getType());
		s.setType(spec.getType());
		s.setServiceDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
		s.setStartDate( OffsetDateTime.now(ZoneOffset.UTC).toString()  );
		s.setEndDate( sor.getExpectedCompletionDate()  );
		s.hasStarted(false);
		s.setIsServiceEnabled(false);
		s.setStartMode( startMode.getValue() );
		
		Note noteItem = new Note();
		noteItem.setText("Service Created by AutomationCheck");
		noteItem.setAuthor("OSOM");
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
				rp.setUuid(null); 
				s.addRelatedPartyItem(rp);
			}			
		}
		
		if (soi.getService().getServiceCharacteristic() != null ) {
			for (ServiceSpecCharacteristic c : spec.getServiceSpecCharacteristic()) {
				
				boolean characteristicFound = false;
				for (Characteristic orderCharacteristic : soi.getService().getServiceCharacteristic()) {
					String specCharacteristicToSearch = spec.getName() + "::" +c.getName();
					 if ( orderCharacteristic.getName().equals( specCharacteristicToSearch )) { //copy only characteristics that are related from the order
						s.addServiceCharacteristicItem( addServiceCharacteristicItem(c, orderCharacteristic) );
						characteristicFound = true;
						break;
					}
				}
				
				if (!characteristicFound) { //fallback to find simple name (i.e. not starting with service spec name)
					for (Characteristic orderCharacteristic : soi.getService().getServiceCharacteristic()) {
						String specCharacteristicToSearch = c.getName();
						 if ( orderCharacteristic.getName().equals( specCharacteristicToSearch )) { //copy only characteristics that are related from the order							 
							
							s.addServiceCharacteristicItem( addServiceCharacteristicItem(c, orderCharacteristic) );
							characteristicFound = true;
							break;
						}
					}
					
				}
			}
			
		}
		
		
		Service createdService = serviceOrderManager.createService(s, sor, spec);
		return createdService;
	}

	private Characteristic addServiceCharacteristicItem(ServiceSpecCharacteristic c, Characteristic orderCharacteristic) {
		Characteristic serviceCharacteristicItem =  new Characteristic();
		serviceCharacteristicItem.setName( c.getName() );
		serviceCharacteristicItem.setValueType( c.getValueType() );
					
		Any val = new Any();
		val.setValue( orderCharacteristic.getValue().getValue() );
		val.setAlias( orderCharacteristic.getValue().getAlias() );
		
		serviceCharacteristicItem.setValue( val );
		
		return serviceCharacteristicItem;
	}
}
