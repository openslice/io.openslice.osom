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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.openslice.model.DeploymentDescriptor;
import io.openslice.model.DeploymentDescriptorStatus;
import io.openslice.model.ExperimentMetadata;
import io.openslice.tmf.common.model.Any;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.common.model.service.ServiceStateType;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristic;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristicValue;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceUpdate;
import io.openslice.tmf.so641.model.ServiceOrder;


@Component(value = "orchestrationService") //bean name
public class OrchestrationService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(OrchestrationService.class.getName());


	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	public void execute(DelegateExecution execution) {

		logger.info( execution.getVariableNames().toString() );
		logger.info("orderid:" + execution.getVariable("orderid").toString() );
		logger.info("serviceId:" + execution.getVariable("serviceId").toString() );

		ServiceUpdate su = new ServiceUpdate();//the object to update the service
		
		if (execution.getVariable("serviceId") instanceof String) {

			ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder( execution.getVariable("orderid").toString() );
			Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("serviceId") );
			logger.info("Service name:" + aService.getName() );
			logger.info("Service state:" + aService.getState()  );			
			logger.info("Request to NFVO for Service: " + aService.getId() );
			
			//we need to retrieve here the Service Spec of the service
			
			ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aService.getServiceSpecificationRef().getId() );
			
			if ( spec!=null ) {			
				
				String NSDID = null;
				 
				for (ServiceSpecCharacteristic c : spec.getServiceSpecCharacteristic() ) {
					if (c.getName().equals("NSDID")) {
						for (ServiceSpecCharacteristicValue val : c.getServiceSpecCharacteristicValue()) {
							if (val.isIsDefault()) {
								NSDID = val.getValue().getValue();
							}
						}
					}
				}
				
				if ( NSDID != null) {

					try {
						DeploymentDescriptor dd = createNewDeploymentRequest( NSDID, sorder.getStartDate(), sorder.getExpectedCompletionDate(), sorder.getId() );
						
						su.setState(ServiceStateType.RESERVED );
						Note noteItem = new Note();
						noteItem.setText("Request to NFVO for NSDID: " + NSDID + ". Deployment Request id: " + dd.getId());
						noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
						noteItem.setAuthor("OSOM");
						su.addNoteItem( noteItem );
						Characteristic serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "DeploymentRequestID" );
						serviceCharacteristicItem.setValue( new Any( dd.getId() + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);
						
						serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "Status" );
						serviceCharacteristicItem.setValue( new Any( dd.getStatus() + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);

						serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "OperationalStatus" );
						serviceCharacteristicItem.setValue( new Any( dd.getOperationalStatus() + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);

						serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "ConstituentVnfrIps" );
						serviceCharacteristicItem.setValue( new Any( dd.getConstituentVnfrIps()  + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);
						

						serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "ConfigStatus" );
						serviceCharacteristicItem.setValue( new Any( dd.getConfigStatus() + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);
												
						Service supd = serviceOrderManager.updateService(  execution.getVariable("serviceId").toString(), su);
						logger.info("Request to NFVO for NSDID:" + NSDID + " done! Service: " + supd.getId() );
						
						
						waitForDeploymentStatus( dd.getId(), supd );
						
						
						return;					
					}finally {

						logger.error( "Cannot createNewDeploymentRequest for service :" + spec.getId() );
					}
					
				} else {

					logger.error( "Cannot retrieve NSDID from ServiceSpecification for service :" + spec.getId() );
				}
				
			} else {

				logger.error( "Cannot retrieve ServiceSpecification for service :" + (String) execution.getVariable("serviceId") );
			}
		} else {
			logger.error( "Cannot retrieve variable serviceId"  );
		}

		//if we get here somethign is wrong so we need to terminate the service.
		Note noteItem = new Note();
		noteItem.setText("Request to NFVO FAILED");
		noteItem.setAuthor("OSOM");
		noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
		su.addNoteItem( noteItem );
		su.setState(ServiceStateType.TERMINATED   );
		serviceOrderManager.updateService(  execution.getVariable("serviceId").toString(), su);
		
	}



	private DeploymentDescriptor createNewDeploymentRequest(String nsdId, OffsetDateTime startDate, OffsetDateTime endDate, String orderid) {
		DeploymentDescriptor ddreq = new DeploymentDescriptor();
		ExperimentMetadata expReq = new ExperimentMetadata();
		expReq.setId( Long.parseLong(nsdId));
		ddreq.setName("Service Order " + orderid);
		ddreq.setDescription("Created automatically by OSOM for Service Order " + orderid);
		ddreq.setExperiment( expReq  );
		ddreq.setStartReqDate(  new Date(startDate.toInstant().toEpochMilli()) );
		ddreq.setStartDate( new Date(startDate.toInstant().toEpochMilli()) );
		ddreq.setEndReqDate( new Date(endDate.toInstant().toEpochMilli()) );
		ddreq.setEndDate( new Date(endDate.toInstant().toEpochMilli()) );
		ddreq.setStatus( DeploymentDescriptorStatus.SCHEDULED );
		DeploymentDescriptor dd =serviceOrderManager.nfvoDeploymentRequestByNSDid( ddreq );
		
		
		return dd;
	}

	private void waitForDeploymentStatus(long deploymentId, Service aService) {
		
		int maxTry = 0;
		while ( maxTry < 120) {
			try {
				//retrieve Status from NFVO (OSM?) scheduler
				logger.info("Checking Deployment Status of deployment Request id: " + deploymentId );

				DeploymentDescriptor dd =serviceOrderManager.retriveNFVODeploymentRequestById( deploymentId );
				logger.info("Operational Status of deployment Request id: " + dd.getOperationalStatus() );
				logger.info("Status of deployment Request id: " + dd.getStatus() );
				ServiceUpdate supd = new ServiceUpdate();
				for (Characteristic c : aService.getServiceCharacteristic()) {
					if ( c.getName().equals("Status")) {
						c.setValue( new Any( dd.getStatus() + "" ));
					} else if ( c.getName().equals("OperationalStatus")) {
						c.setValue( new Any( dd.getOperationalStatus() + "" ));
					} else if ( c.getName().equals("ConstituentVnfrIps")) {
						c.setValue( new Any( dd.getConstituentVnfrIps() + "" ));
					} else if ( c.getName().equals("ConfigStatus")) {
						c.setValue( new Any( dd.getConfigStatus() + "" ));
					}
					supd.addServiceCharacteristicItem( c );					
				}
				
				if ( dd.getStatus().equals( DeploymentDescriptorStatus.RUNNING) ) {
					supd.setState( ServiceStateType.ACTIVE);
				} else if ( dd.getStatus().equals( DeploymentDescriptorStatus.REJECTED) 
						|| dd.getStatus().equals( DeploymentDescriptorStatus.FAILED) 
						|| dd.getStatus().equals( DeploymentDescriptorStatus.FAILED_OSM_REMOVED)
						|| dd.getStatus().equals( DeploymentDescriptorStatus.COMPLETED)
						|| dd.getStatus().equals( DeploymentDescriptorStatus.TERMINATED) 
						|| dd.getStatus().equals( DeploymentDescriptorStatus.TERMINATION_FAILED) ) {
					supd.setState( ServiceStateType.TERMINATED);
				}
				
				Service serviceResult = serviceOrderManager.updateService( aService.getId(), supd );
				
				if ( serviceResult.getState().equals(ServiceStateType.ACTIVE)
						|| serviceResult.getState().equals(ServiceStateType.TERMINATED)) {

					logger.info("Deployment Status OK. Service state = " + serviceResult.getState() );
					return;
				}
				logger.info("Wait For Deployment Status. ");
				Thread.sleep( 30000 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}finally {
				
			}
			maxTry++;
		}
		
		
	}
	
	
}
