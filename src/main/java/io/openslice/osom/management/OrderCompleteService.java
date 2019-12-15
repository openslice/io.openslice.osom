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

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.openslice.tmf.common.model.service.ResourceRef;
import io.openslice.tmf.common.model.service.ServiceRef;
import io.openslice.tmf.common.model.service.ServiceStateType;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderItem;
import io.openslice.tmf.so641.model.ServiceOrderStateType;
import io.openslice.tmf.so641.model.ServiceOrderUpdate;


@Component(value = "orderCompleteService") //bean name
public class OrderCompleteService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(OrderCompleteService.class.getName());


    @Autowired
    private ServiceOrderManager serviceOrderManager;
    
	public void execute(DelegateExecution execution) {

		logger.info("OrderCompleteService:" + execution.getVariableNames().toString() );

		if (execution.getVariable("orderId")!=null) {
			logger.info("Will check status of services of orderid:" + execution.getVariable("orderId") );
			
			ServiceOrder sOrder = serviceOrderManager.retrieveServiceOrder((String) execution.getVariable("orderId") );
			
			if (sOrder == null) {
				logger.error("Cannot retrieve Service Order details from catalog.");
				return;
			}


			boolean allCompletedItemsInOrder= true;
			
			logger.info("ServiceOrder id:" + sOrder.getId());
			for (ServiceOrderItem soi : sOrder.getOrderItem()) {
				boolean existsReserved=false;
				boolean existsInactive=false;
				boolean allTerminated= ( soi.getService().getSupportingService() != null) || ( soi.getService().getSupportingResource() != null);
				boolean existsDesigned=false;
				boolean allActive= ( soi.getService().getSupportingService() != null) || ( soi.getService().getSupportingResource() != null);
				
				
				if ( soi.getService().getSupportingService() != null) {
					for (ServiceRef sr : soi.getService().getSupportingService()) {
						Service srv = serviceOrderManager.retrieveService( sr.getId() );
						existsReserved = existsReserved || srv.getState().equals(ServiceStateType.RESERVED );
						existsInactive = existsInactive || srv.getState().equals(ServiceStateType.INACTIVE );
						existsDesigned = existsDesigned || srv.getState().equals(ServiceStateType.DESIGNED );
						allTerminated = allTerminated && srv.getState().equals(ServiceStateType.TERMINATED );
						allActive = allActive && srv.getState().equals(ServiceStateType.ACTIVE );
					}						
				}
				
				
				if ( soi.getService().getSupportingResource() != null) {
					for (ResourceRef rr : soi.getService().getSupportingResource()) {
						Service srv = serviceOrderManager.retrieveService( rr.getId() );
						existsReserved = existsReserved || srv.getState().equals(ServiceStateType.RESERVED );
						existsInactive = existsInactive || srv.getState().equals(ServiceStateType.INACTIVE );
						existsDesigned = existsDesigned || srv.getState().equals(ServiceStateType.DESIGNED );
						allTerminated = allTerminated && srv.getState().equals(ServiceStateType.TERMINATED );
						allActive = allActive && srv.getState().equals(ServiceStateType.ACTIVE );
					}					
				}
				
				@Valid
				ServiceStateType sserviceState = soi.getService().getState();
				if (allActive) {
					sserviceState = ServiceStateType.ACTIVE;
					soi.setState( ServiceOrderStateType.COMPLETED );		
				} else if (allTerminated) {
					sserviceState = ServiceStateType.TERMINATED;					
				} else if (existsInactive) {
					sserviceState = ServiceStateType.INACTIVE;		
					soi.setState( ServiceOrderStateType.INPROGRESS );				
				} else if (existsDesigned) {
					sserviceState = ServiceStateType.DESIGNED;	
					soi.setState( ServiceOrderStateType.INPROGRESS );					
				} else if (existsReserved) {
					sserviceState = ServiceStateType.RESERVED;	
					soi.setState( ServiceOrderStateType.INPROGRESS );						
				}
				
				soi.getService().setState(sserviceState);	

				allCompletedItemsInOrder = allCompletedItemsInOrder && soi.getState().equals( ServiceOrderStateType.COMPLETED );
			}
			
			   
			if (allCompletedItemsInOrder) {
				sOrder.setState( ServiceOrderStateType.COMPLETED );				
			}
			
			ServiceOrderUpdate serviceOrderUpd = new ServiceOrderUpdate();
			serviceOrderUpd.setState( sOrder.getState());
			
			for (ServiceOrderItem orderItemItem : sOrder.getOrderItem()) {
				serviceOrderUpd.addOrderItemItem(orderItemItem);
			}
			serviceOrderManager.updateServiceOrderOrder( sOrder.getId() , serviceOrderUpd);
			
		}
		
	}

}
