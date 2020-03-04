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
package io.openslice.osom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doCallRealMethod;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.api.ExecuteDecisionBuilder;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngines;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.engine.test.FlowableDmnRule;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.xml.converter.DmnXMLConverter;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.flowable.task.api.Task;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.osom.partnerservices.PartnerOrganizationServicesManager;
import io.openslice.tmf.pm632.model.Organization;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.so641.model.ServiceOrder;

@ExtendWith(FlowableSpringExtension.class)
@SpringBootTest(properties = { "CATALOG_GET_SERVICEORDER_BY_ID = direct:get_mocked_order",
		"CATALOG_GET_SERVICESPEC_BY_ID = direct:get_mocked_spec", "CATALOG_ADD_SERVICE = direct:get_mocked_add_service",
		"CATALOG_UPD_SERVICEORDER_BY_ID = direct:get_mocked_upd_order",
		"CATALOG_GET_SERVICE_BY_ID = direct:get_mocked_service_id",
		"CATALOG_UPD_SERVICE = direct:get_mocked_upd_service", "NFV_CATALOG_DEPLOY_NSD_REQ = direct:req_deploy_nsd",
		"NFV_CATALOG_GET_DEPLOYMENT_BY_ID = direct:req_deployment_id", 
		 "CATALOG_GET_EXTERNAL_SERVICE_PARTNERS = direct:get_mocked_partners",
		"uri.to   = mock:output" })
@ActiveProfiles("testing")
public class ProcessPartnerServicesIntegrationTest {
	private static final transient Log logger = LogFactory.getLog(ProcessPartnerServicesIntegrationTest.class.getName());

	@Autowired
	RepositoryService repositoryService;

	@Autowired
	private RuntimeService runtimeService;


	@Autowired
	private ManagementService managementService;
	
	@Autowired
	private TaskService taskService;

	@Autowired
	private CamelContext camelContext;

	@Autowired
	private PartnerOrganizationServicesManager partnerOrganizationServicesManager;

	SPMocked spmocked = new SPMocked();

	@Test
	// @Deployment(resources = { "processes/ServiceOrder.bpmn" })
	public void startProcess() throws Exception {


		repositoryService.suspendProcessDefinitionByKey("OrderSchedulerProcess"); // this is to stop the timer
		repositoryService.suspendProcessDefinitionByKey("fetchInRpogressOrdersProcess"); // this is to stop the timer
		//repositoryService.suspendProcessDefinitionByKey("fetchPartnerServicesProcess"); // this is to stop the timer
		
		/**
		 * configure here the mocked routes
		 */
		RoutesBuilder builder = new RouteBuilder() {
			@Override
			public void configure() {
				from("direct:get_mocked_partners").bean(spmocked, "getPartners");

			};
		};

		camelContext.addRoutes(builder);

		logger.info("waiting 1secs");
		Thread.sleep(1000); // wait

//		List<Organization> orgz = partnerOrganizationServicesManager.retrievePartners();
//		assertThat( orgz ).isInstanceOf( List.class);
//
//		assertThat( orgz ).hasSize(1);
		
		
		//Job timer = managementService.createTimerJobQuery().jobId("timerstarteventFetchPartnerServices").singleResult();
		//repositoryService.activateProcessDefinitionByKey( "fetchPartnerServicesProcess" );
		//runtimeService.startProcessInstanceById("fetchPartnerServicesProcess"  );
		
		
		
		List<Job> jobs = managementService.createTimerJobQuery().list();
		logger.info( "jobs.size() " +   jobs.size());
		for (Job timer : jobs) {
			logger.info( "Timer getExecutionId " +   timer.getExecutionId() );
			logger.info( "Timer getId " + timer.getId()  );
			logger.info( "Timer getJobHandlerConfiguration " + timer.getJobHandlerConfiguration() );
			logger.info( "Timer getElementName " + timer.getElementName()  );
			if ( timer.getJobHandlerConfiguration().contains( "timerstarteventFetchPartnerServices" ) ) {
				managementService.moveTimerToExecutableJob(timer.getId());
				//managementService.executeJob(timer.getId());			
				
			}
			//logger.info( "Timer details" + runtimeService.getActiveActivityIds( timer.getId() ));
		}
		
		
		
		logger.info("waiting 10secs");
		Thread.sleep( 10000); // wait

	}



}
