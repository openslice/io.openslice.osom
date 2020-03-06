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
package io.openslice.osom.partnerservices;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.tmf.pm632.model.Organization;
import io.openslice.tmf.so641.model.ServiceOrderStateType;

@Component(value = "fetchPartnerOrganizations") // bean name
public class FetchPartnerOrganizations implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(FetchPartnerOrganizations.class.getName());

	@Autowired
	private TaskService taskService;

	@Autowired
	PartnerOrganizationServicesManager partnerOrganizationServicesManager;

	public void execute(DelegateExecution execution) {
		logger.info("======================" + execution.getProcessDefinitionId() + "======================================");
		logger.info("FetchPartnerOrganizations by Repository");

		List<Organization> partnerList = partnerOrganizationServicesManager.retrievePartners();

		List<String> partnerListAsString = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			if ( partnerList!=null) {			
				for (Organization organization : partnerList) {
					String o = mapper.writeValueAsString(organization);
					partnerListAsString.add(o);
				}	
			}

			execution.setVariable("partnerOrganizations", partnerListAsString);

		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
