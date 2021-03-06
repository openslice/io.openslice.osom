/*-
 * ========================LICENSE_START=================================
 * io.openslice.osom
 * %%
 * Copyright (C) 2019 - 2020 openslice.io
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.openslice.osom.partnerservices.PartnerOrganizationServicesManager;
import io.openslice.tmf.common.model.UserPartRoleType;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.common.model.service.ServiceRef;
import io.openslice.tmf.common.model.service.ServiceStateType;
import io.openslice.tmf.pm632.model.Organization;
import io.openslice.tmf.prm669.model.RelatedParty;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristic;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceUpdate;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderItem;
import io.openslice.tmf.so641.model.ServiceOrderStateType;


@Component(value = "localSoCheckDeployment") //bean name
public class LocalSOCheckDeployment  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( LocalSOCheckDeployment.class.getName());


	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	@Autowired
	private PartnerOrganizationServicesManager partnerOrganizationServicesManager;
	
	
	public void execute(DelegateExecution execution) {

		logger.info( "LocalSOCheckDeployment" );
		logger.debug( execution.getVariableNames().toString() );

		execution.setVariable("lsoServiceDeploymentFinished",   false );

		ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder( execution.getVariable("orderid").toString() );
		Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("serviceId") );
		logger.debug("Check LocalSOCheckDeploymentfor Service name:" + aService.getName() );
		logger.debug("Check LocalSOCheckDeployment  Service state:" + aService.getState()  );			
		logger.debug("Request for Service id: " + aService.getId() );

		ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aService.getServiceSpecificationRef().getId() );
		


		ServiceUpdate supd = new ServiceUpdate();
		
		supd.setState( ServiceStateType.ACTIVE);
		
		
		if  (spec.getName().contains("DUMMYSERVICE") ) {
			logger.info("DUMMYSERVICE status" );
			ServiceSpecCharacteristic charc = spec.findSpecCharacteristicByName( "FINALSTATUS" );
			if ( (charc!=null ) && ( charc.getServiceSpecCharacteristicValue() !=null ) ) {
				try {
					String val = charc.getServiceSpecCharacteristicValue().stream().findFirst().get().getValue().getValue();
					if (val.equals("TERMINATED") ) {
						supd.setState( ServiceStateType.TERMINATED );						
					} else if (val.equals("INACTIVE") ) {
						supd.setState( ServiceStateType.INACTIVE );						
					} else if (val.equals("RESERVED") ) {
						supd.setState( ServiceStateType.RESERVED );						
					} else if (val.equals("ACTIVE") ) {
						supd.setState( ServiceStateType.ACTIVE );						
					}
				}catch (Exception e) {

				}				
			}
		}
		
		if ( aService.getState() != supd.getState()) {			
			Note noteItem = new Note();
			noteItem.setText("Update Service Order State to: " + supd.getState() + ". ");
			noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
			noteItem.setAuthor("OSOM");
			supd.addNoteItem( noteItem );
			Service serviceResult = serviceOrderManager.updateService( aService.getId(), supd, false );
			if ( serviceResult.getState().equals(ServiceStateType.ACTIVE)
					|| serviceResult.getState().equals(ServiceStateType.TERMINATED)) {

				logger.info("Deployment Status OK. Service state = " + serviceResult.getState() );
				execution.setVariable("lsoServiceDeploymentFinished", true);
				return;
			}
		}
		logger.info("Wait For Local SO Service Status. ");
		
		
	}
}
