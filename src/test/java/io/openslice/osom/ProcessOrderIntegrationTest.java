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
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
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
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.so641.model.ServiceOrder;

@ExtendWith(FlowableSpringExtension.class)
@SpringBootTest(properties = { "CATALOG_GET_SERVICEORDER_BY_ID = direct:get_mocked_order",
		"CATALOG_GET_SERVICESPEC_BY_ID = direct:get_mocked_spec", "CATALOG_ADD_SERVICE = direct:get_mocked_add_service",
		"CATALOG_UPD_SERVICEORDER_BY_ID = direct:get_mocked_upd_order",
		"CATALOG_GET_SERVICE_BY_ID = direct:get_mocked_service_id",
		"CATALOG_SERVICE_QUEUE_ITEMS_GET: direct:get_mocked_service_id",
		"CATALOG_SERVICE_QUEUE_ITEM_UPD: direct:get_mocked_service_id",
		"CATALOG_SERVICE_QUEUE_ITEM_DELETE: direct:get_mocked_service_id",
		"CATALOG_UPD_SERVICE = direct:get_mocked_upd_service", "NFV_CATALOG_DEPLOY_NSD_REQ = direct:req_deploy_nsd",
		
		"NFV_CATALOG_GET_DEPLOYMENT_BY_ID = direct:req_deployment_id", 
		"NFV_CATALOG_UPD_DEPLOYMENT_BY_ID = direct:req_deployment_id", 
		"uri.to   = mock:output" })
@ActiveProfiles("testing")
public class ProcessOrderIntegrationTest {
	private static final transient Log logger = LogFactory.getLog(ProcessOrderIntegrationTest.class.getName());

	@Autowired
	RepositoryService repositoryService;

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private CamelContext camelContext;

//    @MockBean(name = "orchestrationService" )
//    @Autowired
//    private OrchestrationServiceMocked orchestrationServiceMocked;

	@Autowired
	private ServiceOrderManager serviceOrderManager;

	SCMocked scmocked = new SCMocked();

	@Test
	// @Deployment(resources = { "processes/ServiceOrder.bpmn" })
	public void startProcess() throws Exception {
		// doCallRealMethod().when( orchestrationServiceMocked).execute( Mockito.any() )
		// ;

		/**
		 * configure here the mocked routes
		 */
		RoutesBuilder builder = new RouteBuilder() {
			@Override
			public void configure() {
				from("direct:get_mocked_order").bean(scmocked, "getOrderById");
				from("direct:get_mocked_spec").bean(scmocked, "getSpecById");
				from("direct:get_mocked_add_service").bean(scmocked, "getMockedService");
				from("direct:get_mocked_upd_service").bean(scmocked, "getMockedService");
				from("direct:get_mocked_upd_order").bean(scmocked, "updateServiceOrder");
				from("direct:get_mocked_service_id").bean(scmocked, "getServiceById");
				from("direct:req_deploy_nsd").bean(scmocked, "req_deploy_nsd");
				from("direct:req_deployment_id").bean(scmocked, "req_deployment_id");

			};
		};

		camelContext.addRoutes(builder);

		logger.info("waiting 1secs");
		Thread.sleep(1000); // wait

		assertThat(serviceOrderManager.retrieveServiceOrder("b0661e27-020f-4026-84ab-5c265bac47e7"))
				.isInstanceOf(ServiceOrder.class);
		assertThat(serviceOrderManager.retrieveServiceOrder("93b9928c-de35-4495-a157-1100f6e71c92"))
				.isInstanceOf(ServiceOrder.class);
		assertThat(serviceOrderManager.retrieveServiceSpec("59d08753-e1b1-418b-9e3e-d3a3bb573051"))
				.isInstanceOf(ServiceSpecification.class);

		assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(11);
		assertThat(taskService.createTaskQuery().count()).isEqualTo(0);

		assertThat( scmocked.getRequeestedDescriptor() ).isNull();
		
		repositoryService.suspendProcessDefinitionByKey("OrderSchedulerProcess"); // this is to stop the timer

		Map<String, Object> variables = new HashMap<>();
		variables.put("orderid", "93b9928c-de35-4495-a157-1100f6e71c92");
		runtimeService.startProcessInstanceByKey("StartOrderProcess", variables);
		logger.info("waiting 1sec");
		Thread.sleep(2000); // wait

		for (ProcessInstance pi : runtimeService.createProcessInstanceQuery().list()) {
			logger.info(" pi.id " + pi.toString());
		}

		for (Task task : taskService.createTaskQuery().list()) {
			logger.info(" task.name " + task.getName());
		}

//		assertThat( scmocked.getRequeestedDescriptor() ).isNotNull();
//		assertThat( scmocked.getRequeestedDescriptor().getId() ).isEqualTo( 123456789 );
//		assertThat( scmocked.getRequeestedDescriptor().getConfigStatus() ).contains("cirros_ue_uplink=192.0");
//		assertThat( scmocked.getRequeestedDescriptor().getConfigStatus() ).contains("cirros_slice_uplink=1024.0");

		

		assertThat(taskService.createTaskQuery().count()).isEqualTo(0);

		logger.info("waiting 1secs");
		Thread.sleep(1000); // wait

	}

	@Autowired
	private DmnEngine dmnEngine;

	@Autowired
	private DmnRuleService ruleService;

//	@Autowired
//	@Rule
//	public FlowableDmnRule flowableSpringRule;

	@Test
	@DmnDeployment(resources = "dmn/genericdecisions.dmn")
	public void uniqueHitPolicy() {
//		DmnEngine admnEngine = flowableSpringRule.getDmnEngine();
		DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

		ExecuteDecisionBuilder ex = ruleService.createExecuteDecisionBuilder().decisionKey("decisionKJ");

		Map<String, Object> result = ex.variable("Uplink_throughput_per_UE__Guaranteed_uplink_throughput", 8).executeWithSingleResult();

		assertEquals(64.0, result.get("cirros_ue_uplink"));
		assertEquals(512.0, result.get("cirros_slice_uplink"));
	}

	@Test
	public void programmaticallyCreate() throws XMLStreamException, FileNotFoundException {

		try {

			File initialFile = new File("src/test/resources/ondemand_decisions.dmn");
			InputStream targetStream = new FileInputStream(initialFile);

			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			XMLStreamReader xtr = inputFactory.createXMLStreamReader(targetStream);
			DmnDefinition dmnDefinition = new DmnXMLConverter().convertToDmnModel(xtr);

			DmnRepositoryService dmnRepositoryService = dmnEngine.getDmnRepositoryService();
			org.flowable.dmn.api.DmnDeployment dmnDeployment = dmnRepositoryService.createDeployment()
					.name("decision_ONDEMAND").tenantId("abcd").addDmnModel("ondemand_decisions.dmn", dmnDefinition).deploy();
			
//			DmnDecisionTable dmnt = dmnRepositoryService.getDecisionTable( "decision_ONDEMAND" );			
//			assertNotNull(dmnt);
			
			ExecuteDecisionBuilder ex = ruleService.createExecuteDecisionBuilder().decisionKey("decision_ONDEMAND").tenantId("abcd");

			Map<String, Object> variables = new HashMap<>();
			variables.put("cameras", 3);
			variables.put("video_definition", 3);
			Map<String, Object> result = ex.variables(variables).executeWithSingleResult();
			assertEquals("1024", result.get("uplink"));
			assertEquals( 2048.0, result.get("slice_uplink"));
			
			variables = new HashMap<>();
			variables.put("cameras", 3);
			variables.put("video_definition", 2);
			result = ex.variables(variables).executeWithSingleResult();
			assertEquals("256", result.get("uplink"));
			assertEquals( 1024.0, result.get("slice_uplink"));
			
			
		} finally {

		}
	}
	
	
	@Test
	public void testNFVOProcessOrder() throws Exception {


		repositoryService.suspendProcessDefinitionByKey("OrderSchedulerProcess"); // this is to stop the timer
		repositoryService.suspendProcessDefinitionByKey("fetchInRpogressOrdersProcess"); // this is to stop the timer
		
		
		/**
		 * configure here the mocked routes
		 */
		RoutesBuilder builder = new RouteBuilder() {
			@Override
			public void configure() {
				from("direct:get_mocked_order").bean(scmocked, "getOrderById");
				from("direct:get_mocked_spec").bean(scmocked, "getSpecById");
				from("direct:get_mocked_add_service").bean(scmocked, "getMockedService");
				from("direct:get_mocked_upd_service").bean(scmocked, "getMockedService");
				from("direct:get_mocked_upd_order").bean(scmocked, "updateServiceOrder");
				from("direct:get_mocked_service_id").bean(scmocked, "getServiceById");
				from("direct:req_deploy_nsd").bean(scmocked, "req_deploy_nsd");
				from("direct:req_deployment_id").bean(scmocked, "req_deployment_id");

			};
		};

		camelContext.addRoutes(builder);

		logger.info("waiting 1secs");
		Thread.sleep(1000); // wait

		assertThat(serviceOrderManager.retrieveServiceOrder("b0661e27-020f-4026-84ab-5c265bac47e7"))
				.isInstanceOf(ServiceOrder.class);
		
		Map<String, Object> variables = new HashMap<>();
		variables.put("orderid", "93b9928c-de35-4495-a157-1100f6e71c92");
		runtimeService.startProcessInstanceByKey("StartOrderProcess", variables);
		logger.info("waiting 1sec");
		Thread.sleep(1000); // wait

		for (ProcessInstance pi : runtimeService.createProcessInstanceQuery().list()) {
			logger.info(" pi.id " + pi.toString());
		}

		for (Task task : taskService.createTaskQuery().list()) {
			logger.info(" task.name " + task.getName());
		}

		
		logger.info("waiting 10secs");
		Thread.sleep(10000); // wait

	}

}
