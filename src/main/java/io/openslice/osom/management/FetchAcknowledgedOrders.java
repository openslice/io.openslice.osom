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

import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "fetchAcknowledgedOrders") // bean name
public class FetchAcknowledgedOrders implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(FetchAcknowledgedOrders.class.getName());


    @Autowired
    private ServiceOrderManager serviceOrderManager;

	public void execute(DelegateExecution execution) {
		logger.info("fetchAcknowledgedOrders by Service Order Repository");

		List<String> ordersToBeProcessed = null;
		if (execution.getVariable("ordersToBeProcessed") instanceof ArrayList) {
			ordersToBeProcessed = (ArrayList<String>) execution.getVariable("ordersToBeProcessed");
			for (String orderid : ordersToBeProcessed) {
				logger.info("ordersFromPrevious = " + orderid);
			}
		} else {
			ordersToBeProcessed = new ArrayList<>();
		}

		List<String> orderlist = serviceOrderManager.retrieveOrdersToBeProcessed();
		
		for (String orderid : orderlist) {
			if ( !ordersToBeProcessed.contains( orderid )  ) {
				ordersToBeProcessed.add( orderid );
				
			}
		}
		
		execution.setVariable("ordersToBeProcessed", ordersToBeProcessed);

	}
}
