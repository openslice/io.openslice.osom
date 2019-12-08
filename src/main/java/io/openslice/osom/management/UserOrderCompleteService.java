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
import org.springframework.stereotype.Component;


@Component(value = "userOrderCompleteService") //bean name
public class UserOrderCompleteService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(UserOrderCompleteService.class.getName());

	public void execute(DelegateExecution execution) {

		logger.info("UserOrderCompleteService:" + execution.getVariableNames().toString() );
		logger.info("UserOrderCompleteService serviceSpec:" + execution.getVariable("serviceSpec").toString()  );
		

		
	}

}
