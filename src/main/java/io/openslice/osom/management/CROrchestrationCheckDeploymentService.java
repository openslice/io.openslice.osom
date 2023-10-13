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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.openslice.model.DeploymentDescriptor;
import io.openslice.model.DeploymentDescriptorStatus;
import io.openslice.model.DeploymentDescriptorVxFInstanceInfo;
import io.openslice.tmf.common.model.Any;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.common.model.service.ResourceRef;
import io.openslice.tmf.common.model.service.ServiceStateType;
import io.openslice.tmf.ri639.model.Resource;
import io.openslice.tmf.ri639.model.ResourceStatusType;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceUpdate;
import jakarta.validation.Valid;


@Component(value = "crOrchestrationCheckDeploymentService") //bean name
public class CROrchestrationCheckDeploymentService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(CROrchestrationCheckDeploymentService.class.getName());


	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	public void execute(DelegateExecution execution) {

		logger.info( "CROrchestrationCheckDeploymentService" );
		logger.info( execution.getVariableNames().toString() );


		if ( execution.getVariable("contextServiceId") == null) {

			logger.error( "Variable contextServiceId is NULL!" );
			execution.setVariable("serviceDeploymentFinished", Boolean.TRUE );
			return;
		}
		Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("contextServiceId") );

		if ( aService == null ) {
			logger.info( "aService is null for contextServiceId = " +(String) execution.getVariable("contextServiceId") );			
			execution.setVariable("serviceDeploymentFinished", Boolean.TRUE );
			return;
		}


		execution.setVariable("serviceDeploymentFinished", Boolean.FALSE );


		ServiceUpdate supd = new ServiceUpdate();
		boolean propagateToSO = false;

		//retrieve the related supporting resource by id and check its status
		//ResourceRef supresourceRef = aService.getSupportingResource().stream().findFirst().get();//we assume for now we have only one related resource

        @Valid
        ServiceStateType nextState = aService.getState() ;
        boolean allActive = aService.getSupportingResource().size() > 0 ;
        boolean allTerminated = aService.getSupportingResource().size() > 0 ;
        boolean existsInactive=false;
        boolean existsTerminated=false;
        boolean existsReserved=false;
		for ( ResourceRef supresourceRef : aService.getSupportingResource()) {
	        Resource res = serviceOrderManager.retrieveResource( supresourceRef.getId() );
	        if ( res == null ) {
	          supd.setState( ServiceStateType.TERMINATED);
	          execution.setVariable("serviceDeploymentFinished", Boolean.TRUE);
	          Service serviceResult = serviceOrderManager.updateService( aService.getId(), supd, propagateToSO );
	          return;
	        }


	        
	        if ( res.getResourceStatus() != null ) {
	          switch (res.getResourceStatus()) {
	            case AVAILABLE: {
	              nextState = ServiceStateType.ACTIVE;
	              break;
	            }
	            case STANDBY: {
	              nextState = ServiceStateType.RESERVED;
	              break;
	            }
	            case SUSPENDED: {
	              nextState = ServiceStateType.INACTIVE;
	              break;
	            }
	            case RESERVED: {
	              nextState = ServiceStateType.RESERVED;
	              break;
	            }
	            case UNKNOWN: {
	              if (aService.getState().equals( ServiceStateType.ACTIVE  )) {
	                nextState = ServiceStateType.TERMINATED;              
	              }
	              break;
	            }
	            case ALARM: {
	              nextState = ServiceStateType.INACTIVE;
	              break;
	            }
	            default:
	              throw new IllegalArgumentException("Unexpected value: " + res.getResourceStatus());
	          } 
	        }

            allActive = allActive && nextState == ServiceStateType.ACTIVE;
            allTerminated = allTerminated && nextState == ServiceStateType.TERMINATED;
            existsInactive = existsInactive || nextState == ServiceStateType.INACTIVE;
            existsTerminated = existsTerminated || nextState == ServiceStateType.TERMINATED;
            existsReserved = existsReserved || nextState == ServiceStateType.RESERVED;
            
		  
        }
		
		
		if ( allActive ) {
		  supd.setState( ServiceStateType.ACTIVE ); 
		} else if ( allTerminated ) {
          supd.setState( ServiceStateType.TERMINATED ); 
        } else if ( existsInactive ) {
          supd.setState( ServiceStateType.INACTIVE ); 
        } else if ( existsReserved ) {
          supd.setState( ServiceStateType.RESERVED ); 
        } else if ( existsTerminated ) {
          supd.setState( ServiceStateType.INACTIVE ); 
        }
		
		Service serviceResult = serviceOrderManager.updateService( aService.getId(), supd, propagateToSO );
		
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
