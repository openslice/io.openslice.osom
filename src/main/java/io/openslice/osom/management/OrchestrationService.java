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

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.openslice.tmf.sim638.model.Service;


@Component(value = "orchestrationService") //bean name
public class OrchestrationService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(OrchestrationService.class.getName());


	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	public void execute(DelegateExecution execution) {

		logger.info( execution.getVariableNames().toString() );
		logger.info("serviceId:" + execution.getVariable("serviceId").toString() );
		logger.info("orderid:" + execution.getVariable("orderid").toString() );

		if (execution.getVariable("serviceId") instanceof String) {
			Service s = serviceOrderManager.retrieveService( (String) execution.getVariable("serviceId") );
			logger.info("Service name:" + s.getName() );
			logger.info("Service state:" + s.getState()  );
			
		}
		
		if (execution.getVariable("orderid") instanceof String) {
			
			try {
				long completionTime = RandomUtils.nextLong(30000, 63000);

				logger.info("Orchestration of  order with id = " + execution.getVariable("orderid") + ". WWill be Completed in: " + completionTime);
				Thread.sleep( completionTime  );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			logger.info("Orchestration of order with id = " + execution.getVariable("orderid") + ". FINISHED!");
		}
	}

}
