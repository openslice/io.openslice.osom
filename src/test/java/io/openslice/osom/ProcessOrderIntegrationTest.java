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

import io.openslice.tmf.so641.model.ServiceOrder;


@ExtendWith(FlowableSpringExtension.class)
@SpringBootTest(properties = {
	    "ENDPOINT_CATALOG_GET_SERVICEORDER_BY_ID = direct:get_mocked_order",
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
    
    @MockBean(name = "orchestrationService" )
    @Autowired
    private OrchestrationServiceMocked orchestrationServiceMocked;
	
	@Test
	//@Deployment(resources = { "processes/ServiceOrder.bpmn" })
	public void startProcess() throws Exception {
		doCallRealMethod().when( orchestrationServiceMocked).execute( Mockito.any()  ) ;
		
		File sspec = new File( "src/test/resources/TestExServiceOrder.json" );
		InputStream in = new FileInputStream( sspec );
		String sspectext = IOUtils.toString(in, "UTF-8");
		ServiceOrder responseSO = toJsonObj(sspectext,  ServiceOrder.class);
		
		RoutesBuilder builder = new RouteBuilder() {
	        @Override
	        public void configure() {
	          from("direct:get_mocked_order").setBody (  simple(sspectext) );
	        };
		};
		
		camelContext.addRoutes( builder );
		
		MockEndpoint mockSO = camelContext.getEndpoint( "mock:catalog", MockEndpoint.class);
		
		assertThat( repositoryService.createProcessDefinitionQuery().count()  ).isEqualTo(3 );
		assertThat( taskService.createTaskQuery().count()  ).isEqualTo( 0 );
		repositoryService.suspendProcessDefinitionByKey("OrderSchedulerProcess");
		
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderid", "b0661e27-020f-4026-84ab-5c265bac47e7" );
        runtimeService.startProcessInstanceByKey("StartOrderProcess", variables);
        logger.info("waiting 1sec");
        Thread.sleep(1000); //wait
        
        for (ProcessInstance pi : runtimeService.createProcessInstanceQuery().list()) {
        	logger.info(" pi.id " + pi.toString() );
		}
        
        for (Task task : taskService.createTaskQuery().list()) {
        	logger.info(" task.name " + task.getName());
		}
        
        assertThat( taskService.createTaskQuery().count()  ).isEqualTo( 1 );

		// Assert that 1 message will be received
        mockSO.expectedMessageCount(3);
        
		logger.info("waiting 1secs");
        Thread.sleep(1000); //wait
        
        
	}
	
	static <T> T toJsonObj(String content, Class<T> valueType)  throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.readValue( content, valueType);
    }
}
