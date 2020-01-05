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
import static org.mockito.Mockito.doCallRealMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.so641.model.ServiceOrder;


@ExtendWith(FlowableSpringExtension.class)
@SpringBootTest(properties = {
	    "CATALOG_GET_SERVICEORDER_BY_ID = direct:get_mocked_order",
	    "CATALOG_GET_SERVICESPEC_BY_ID = direct:get_mocked_spec",
	    "CATALOG_ADD_SERVICE = direct:get_mocked_add_service",
	    "CATALOG_UPD_SERVICEORDER_BY_ID = direct:get_mocked_upd_order",
	    "CATALOG_GET_SERVICE_BY_ID = direct:get_mocked_service_id",
	    "CATALOG_UPD_SERVICE = direct:get_mocked_upd_service",
	    "NFV_CATALOG_DEPLOY_NSD_REQ = direct:req_deploy_nsd",
	    "NFV_CATALOG_GET_DEPLOYMENT_BY_ID = direct:req_deployment_id",
	    "uri.to   = mock:output" })
@ActiveProfiles("testing")
public class ProcessOrderIntegrationTest {
	private static final transient Log logger = LogFactory.getLog( ProcessOrderIntegrationTest.class.getName());

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
	
	@Test
	//@Deployment(resources = { "processes/ServiceOrder.bpmn" })
	public void startProcess() throws Exception {
		//doCallRealMethod().when( orchestrationServiceMocked).execute( Mockito.any()  ) ;
		
		
		
		/**
		 * configure here the mocked routes
		 */
		RoutesBuilder builder = new RouteBuilder() {
	        @Override
	        public void configure() {
		          from("direct:get_mocked_order").bean( SCMocked.class, "getOrderById");
		          from("direct:get_mocked_spec").bean( SCMocked.class, "getSpecById");
		          from("direct:get_mocked_add_service").bean( SCMocked.class, "getMockedService");
		          from("direct:get_mocked_upd_service").bean( SCMocked.class, "getMockedService");		          
		          from("direct:get_mocked_upd_order").bean( SCMocked.class, "updateServiceOrder");
		          from("direct:get_mocked_service_id").bean( SCMocked.class, "getServiceById");
		          from("direct:req_deploy_nsd").bean( SCMocked.class, "req_deploy_nsd");		
		          from("direct:req_deployment_id").bean( SCMocked.class, "req_deployment_id");		
		          
	        };
		};
		
		camelContext.addRoutes( builder );

		logger.info("waiting 1secs");
        Thread.sleep(1000); //wait
        
		assertThat( serviceOrderManager.retrieveServiceOrder( "b0661e27-020f-4026-84ab-5c265bac47e7")).isInstanceOf( ServiceOrder.class );
		assertThat( serviceOrderManager.retrieveServiceOrder( "93b9928c-de35-4495-a157-1100f6e71c92")).isInstanceOf( ServiceOrder.class );
		assertThat( serviceOrderManager.retrieveServiceSpec( "59d08753-e1b1-418b-9e3e-d3a3bb573051")).isInstanceOf( ServiceSpecification.class );
				
		
		
		assertThat( repositoryService.createProcessDefinitionQuery().count()  ).isEqualTo(3);
		assertThat( taskService.createTaskQuery().count()  ).isEqualTo( 0 );
		repositoryService.suspendProcessDefinitionByKey("OrderSchedulerProcess"); //this is to stop the timer
		
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderid", "93b9928c-de35-4495-a157-1100f6e71c92" );
        runtimeService.startProcessInstanceByKey("StartOrderProcess", variables);
        logger.info("waiting 1sec");
        Thread.sleep(1000); //wait
        
        for (ProcessInstance pi : runtimeService.createProcessInstanceQuery().list()) {
        	logger.info(" pi.id " + pi.toString() );
		}
        
        for (Task task : taskService.createTaskQuery().list()) {
        	logger.info(" task.name " + task.getName());
		}

        assertThat( taskService.createTaskQuery().count()  ).isEqualTo( 0 );


        
		logger.info("waiting 1secs");
        Thread.sleep( 1000 ); //wait
        

	}
	
}
