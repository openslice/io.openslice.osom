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
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceUpdate;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderItem;
import io.openslice.tmf.so641.model.ServiceOrderStateType;

@Component(value = "externalPartnerCheckDeploymentService") //bean name
public class ExternalPartnerCheckDeploymentService  implements JavaDelegate {
	
	private static final transient Log logger = LogFactory.getLog( ExternalPartnerCheckDeploymentService.class.getName());


	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	@Autowired
	private PartnerOrganizationServicesManager partnerOrganizationServicesManager;
	
	
	public void execute(DelegateExecution execution) {

		logger.info( "ExternalPartnerCheckDeploymentService" );
		logger.debug( execution.getVariableNames().toString() );
		String externalServiceOrderId = (String) execution.getVariable("externalServiceOrderId") ;
		execution.setVariable("serviceDeploymentFinished",   false );

		ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder( execution.getVariable("orderid").toString() );
		Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("serviceId") );
		logger.debug("Check external partner for Service name:" + aService.getName() );
		logger.debug("Check external partner for  Service state:" + aService.getState()  );			
		logger.debug("Request to External Service Partner for Service: " + aService.getId() );

		logger.debug("Checking Order Status of Order Request id: " + externalServiceOrderId );
		ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aService.getServiceSpecificationRef().getId() );
		RelatedParty rpOrg = null;
		if ( spec.getRelatedParty() != null ) {
			for (RelatedParty rp : spec.getRelatedParty()) {
				if ( rp.getRole().equals( UserPartRoleType.ORGANIZATION.getValue() )) {
					rpOrg =rp;
					break;
				}				
			}			
		}
		Organization orgz = serviceOrderManager.getExternalPartnerOrganization( rpOrg.getId() );
		
		if ( orgz == null) {
			logger.error("Cannot retrieve partner organization"  );	
			
			return;
		}
		
		logger.debug("External partner organization:" + orgz.getName()  );	
		
		ServiceOrder externalSOrder = partnerOrganizationServicesManager.retrieveServiceOrder( orgz, externalServiceOrderId );
		ServiceUpdate supd = new ServiceUpdate();
		
		
		
		if (externalSOrder != null ) {
			logger.info("External partner organization order state:" + externalSOrder.getState()  );	
			if ( externalSOrder.getState().equals( ServiceOrderStateType.COMPLETED )){
				supd.setState( ServiceStateType.ACTIVE);
			} else if ( externalSOrder.getState().equals( ServiceOrderStateType.ACKNOWLEDGED ) ||
					externalSOrder.getState().equals( ServiceOrderStateType.INPROGRESS )){
				supd.setState( ServiceStateType.RESERVED );
			} else if ( externalSOrder.getState().equals( ServiceOrderStateType.CANCELLED ) ||
					externalSOrder.getState().equals( ServiceOrderStateType.FAILED ) ||
					externalSOrder.getState().equals( ServiceOrderStateType.REJECTED )){
				supd.setState( ServiceStateType.TERMINATED );
			}	else if ( externalSOrder.getState().equals( ServiceOrderStateType.INITIAL ) ||
					externalSOrder.getState().equals( ServiceOrderStateType.PENDING )){
				supd.setState( ServiceStateType.RESERVED );
			}		else if ( externalSOrder.getState().equals( ServiceOrderStateType.PARTIAL )){
				supd.setState( ServiceStateType.INACTIVE );
			}
			
			
			/**
			 * update now service characteristics from the remote Service Inventory
			 */
			
			for (ServiceOrderItem ext_soi : externalSOrder.getOrderItem()) {
				for (ServiceRef serviceRef : ext_soi.getService().getSupportingService()) {
					Service ext_service = partnerOrganizationServicesManager.retrieveServiceFromInventory( orgz, serviceRef.getId() );
					if ( ext_service.getServiceCharacteristic() != null ) {
						for (Characteristic c : ext_service.getServiceCharacteristic()) {
							c.setUuid( null );
							c.setName( orgz.getName()  
									+ "::" 
									+ ext_service.getName() 
									+ "::" 
									+ c.getName());// we prefix here with the Service Name of external partner.
							supd.addServiceCharacteristicItem( c );	
						}
					}
					
				}
			}
			
		}
		
		if ( aService.getState() != supd.getState()) {

			String partnerNotes = "";
			if ( externalSOrder.getNote()!=null) {
				for (Note note : externalSOrder.getNote()) {
					partnerNotes += note.getText() + ".";
				}
				partnerNotes = " Notes from external partner order: " + partnerNotes;
			}			
			
			Note noteItem = new Note();
			noteItem.setText("Update Service Order State to: " + supd.getState() + ". "+  partnerNotes);
			noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
			noteItem.setAuthor("OSOM");
			supd.addNoteItem( noteItem );
			Service serviceResult = serviceOrderManager.updateService( aService.getId(), supd, false );
			if ( serviceResult!=null ) {
				if ( serviceResult.getState().equals(ServiceStateType.ACTIVE)
						|| serviceResult.getState().equals(ServiceStateType.TERMINATED)) {

					logger.info("Deployment Status OK. Service state = " + serviceResult.getState() );
					execution.setVariable("serviceDeploymentFinished", true);
					return;
				}				
			} else {
				logger.error("Deployment Status ERROR from External Parnter with null serviceResult " );
			}
		}
		logger.info("Wait For  External Service Partner Status. ");
		
		
	}
		

}
