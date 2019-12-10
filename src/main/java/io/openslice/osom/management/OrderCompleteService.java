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

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderItem;


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

			logger.debug("ServiceOrder id:" + sOrder.getId());
			for (ServiceOrderItem soi : sOrder.getOrderItem()) {
				
			}
			
		}
		
	}

}
