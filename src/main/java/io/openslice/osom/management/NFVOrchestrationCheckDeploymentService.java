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


@Component(value = "nfvOrchestrationCheckDeploymentService") //bean name
public class NFVOrchestrationCheckDeploymentService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(NFVOrchestrationCheckDeploymentService.class.getName());


	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	public void execute(DelegateExecution execution) {

		logger.info( "NFVOrchestrationCheckDeploymentService" );
		logger.info( execution.getVariableNames().toString() );
		Long deploymentId = (Long) execution.getVariable("deploymentId") ;
		if ( deploymentId == null) {

			logger.error( "Variable deploymentId is NULL!" );
			execution.setVariable("serviceDeploymentFinished", Boolean.TRUE );
			return;
		}
		Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("serviceId") );
		
		//retrieve Status from NFVO (OSM?) scheduler
		logger.info("Checking Deployment Status of deployment Request id: " + deploymentId );
		execution.setVariable("serviceDeploymentFinished", Boolean.FALSE );

		DeploymentDescriptor dd =serviceOrderManager.retrieveNFVODeploymentRequestById( deploymentId );
		logger.info("Operational Status of deployment Request id: " + dd.getOperationalStatus() );
		logger.info("Status of deployment Request id: " + dd.getStatus() );
		ServiceUpdate supd = new ServiceUpdate();
		if ( aService.getServiceCharacteristic() != null ) {
			for (Characteristic c : aService.getServiceCharacteristic()) {
				if ( c.getName().equals("Status")) {
					c.setValue( new Any( dd.getStatus() + "" ));
				} else if ( c.getName().equals("OperationalStatus")) {
					c.setValue( new Any( dd.getOperationalStatus() + "" ));
				} else if ( c.getName().equals("ConstituentVnfrIps")) {
					c.setValue( new Any( dd.getConstituentVnfrIps() + "" ));
				} else if ( c.getName().equals("ConfigStatus")) {
					c.setValue( new Any( dd.getConfigStatus() + "" ));
				}  else if ( c.getName().equals("InstanceId")) {
					c.setValue( new Any( dd.getInstanceId() + "" ));
				} 
				

//				else if ( c.getName().equals("NSR")) {
//					c.setValue( new Any( dd.getNsr() + "" ));
//				} else if ( c.getName().equals("NSLCM_details")) {
//					c.setValue( new Any( dd.getNs_nslcm_details() + "" ));
//				}
				supd.addServiceCharacteristicItem( c );					
			} 
		} else {
			logger.error("Service has no characteristics!" );
			
		}
		
		if ( dd.getStatus().equals( DeploymentDescriptorStatus.RUNNING) ) {
			supd.setState( ServiceStateType.ACTIVE);
		} else if ( dd.getStatus().equals( DeploymentDescriptorStatus.FAILED) ) {
			supd.setState( ServiceStateType.INACTIVE );
		} else if ( dd.getStatus().equals( DeploymentDescriptorStatus.REJECTED) 
				|| dd.getStatus().equals( DeploymentDescriptorStatus.FAILED_OSM_REMOVED)
				|| dd.getStatus().equals( DeploymentDescriptorStatus.COMPLETED)
				|| dd.getStatus().equals( DeploymentDescriptorStatus.TERMINATED) 
				|| dd.getStatus().equals( DeploymentDescriptorStatus.TERMINATION_FAILED) ) {
			supd.setState( ServiceStateType.TERMINATED );
		}
		
		Service serviceResult = serviceOrderManager.updateService( aService.getId(), supd, false );
		
		if ( serviceResult!= null ) {
			if ( serviceResult.getState().equals(ServiceStateType.ACTIVE)
					|| serviceResult.getState().equals(ServiceStateType.TERMINATED)) {

				logger.info("Deployment Status OK. Service state = " + serviceResult.getState() );
				execution.setVariable("serviceDeploymentFinished", Boolean.TRUE);
				return;
			}			
		}
		logger.info("Wait For Deployment Status. ");
		
		

		
		
	}


	
}
