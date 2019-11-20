package io.openslice.osom;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doCallRealMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import io.openslice.osom.management.AddServiceOrderToScheduler;
import io.openslice.osom.management.RejectServiceOrder;


@ExtendWith(FlowableSpringExtension.class)
@SpringBootTest
public class ServiceOrderProcessIntegrationTest {

	private static final transient Log logger = LogFactory.getLog( ServiceOrderProcessIntegrationTest.class.getName());

	@Autowired
	RepositoryService repositoryService;
	
	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;
	

    @Autowired
    private ProducerTemplate template;

    @Autowired
    private CamelContext context;
    
    @MockBean(name = "rejectServiceOrderBean" )
    @Autowired
    private systemTaskMocked rejectServiceOrderBean;
    
    @MockBean(name = "addServiceOrderToScheduler" )
    @Autowired
    private systemTaskMocked addServiceOrderToScheduler;

	@Test
	//@Deployment(resources = { "processes/ServiceOrder.bpmn" })
	public void startProcess() throws Exception {

		doCallRealMethod().when( rejectServiceOrderBean).execute( Mockito.any()  ) ;
		doCallRealMethod().when( addServiceOrderToScheduler).execute( Mockito.any()  ) ;
		
//		File sspec = new File( "src/main/resources/ServiceOrder.bpmn" );
//		InputStream in = new FileInputStream( sspec );

		File sspec = new File( "src/test/resources/TestExServiceOrder.json" );
		InputStream in = new FileInputStream( sspec );
		String sspectext = IOUtils.toString(in, "UTF-8");

		assertThat( repositoryService.createProcessDefinitionQuery().count()  ).isEqualTo( 2 );
		assertThat( taskService.createTaskQuery().count()  ).isEqualTo( 0 );

		//send two orders
		template.sendBody( "jms:queue:OSOM.IN.NEW_SERVICEORDER_PROCESS", sspectext);
		template.sendBody( "jms:queue:OSOM.IN.NEW_SERVICEORDER_PROCESS", sspectext);
		
        Thread.sleep(1000); //wait

		assertThat( repositoryService.createProcessDefinitionQuery().count()  ).isEqualTo( 2 );
		assertThat( taskService.createTaskQuery().count()  ).isEqualTo( 2 );
		
		Object response = template.requestBody( "jms:queue:OSOM.IN.NEW_SERVICEORDER_PROCESS.LIST_PENDING", "admin");
        Thread.sleep(1000); //wait

		assertThat( response  ).isInstanceOf(List.class);
		assertThat( ((List<?>) response).size()  ).isEqualTo(2);
		
		//reject one order
		template.sendBody( "jms:queue:OSOM.IN.ACK_SERVICEORDER_PROCESS", "{\n" + 
				"		  \"id\": \"b0661e27-020f-4026-84ab-5c265bac47e7\",\n" + 
				"		  \"status\": \"false\",\n" + 
				"		 \"assignee\": \"admin\"\n" + 
				"		}");

        Thread.sleep(1000); //wait
		assertThat( taskService.createTaskQuery().count()  ).isEqualTo( 1 );
        
		//accept the last order
		template.sendBody( "jms:queue:OSOM.IN.ACK_SERVICEORDER_PROCESS", "{\n" + 
				"		  \"id\": \"b0661e27-020f-4026-84ab-5c265bac47e7\",\n" + 
				"		  \"status\": \"true\",\n" + 
				"		 \"assignee\": \"admin\"\n" + 
				"		}");

        Thread.sleep(1000); //wait
		assertThat( taskService.createTaskQuery().count()  ).isEqualTo( 0 );
		
		
		
//		Map<String, Object> variableMap = new HashMap<String, Object>();
//		variableMap.put("orderid", "10007");
//		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskAckServiceOrder",
//				variableMap);
//		assertNotNull(processInstance.getId());
//		System.out.println("id " + processInstance.getId() + " " + processInstance.getProcessDefinitionId());

//		 {
//		  "id": "b0661e27-020f-4026-84ab-5c265bac47e7",
//		  "status": "true",
//		 "assignee": "admin"
//		}
		
		
//		Task task = taskService.createTaskQuery().singleResult();
//		  
//        assertEquals("Review the submitted tutorial", task.getName());
//  
//        variables.put("approved", true);
//        taskService.complete(task.getId(), variables);
//  
//        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
		
		//mock.assertIsSatisfied();
        logger.info("WAIT SHUTDOWN");
        context.stop();
        logger.info("EXIT TEST CASE");
	}
}