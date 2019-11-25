package io.openslice.osom;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.osom.management.AddServiceOrderToScheduler;
import io.openslice.osom.management.RejectServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderStateType;


@ExtendWith(FlowableSpringExtension.class)
@SpringBootTest
@ActiveProfiles("testing")
public class InitialServiceOrderProcessIntegrationTest {

	private static final transient Log logger = LogFactory.getLog( InitialServiceOrderProcessIntegrationTest.class.getName());

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

		assertThat( repositoryService.createProcessDefinitionQuery().count()  ).isEqualTo(3 );
		assertThat( taskService.createTaskQuery().count()  ).isEqualTo( 0 );

		//send two orders
		template.sendBody( "jms:queue:OSOM.IN.NEW_SERVICEORDER_PROCESS", sspectext);
		template.sendBody( "jms:queue:OSOM.IN.NEW_SERVICEORDER_PROCESS", sspectext);
		
        Thread.sleep(1000); //wait

		assertThat( repositoryService.createProcessDefinitionQuery().count()  ).isEqualTo( 3 );
		assertThat( taskService.createTaskQuery().count()  ).isEqualTo( 2 );
		
		Object response = template.requestBody( "jms:queue:OSOM.IN.NEW_SERVICEORDER_PROCESS.LIST_PENDING", "admin");
        Thread.sleep(1000); //wait

		assertThat( response  ).isInstanceOf(List.class);
		assertThat( ((List<?>) response).size()  ).isEqualTo(2);
		

		ServiceOrder responseSO = toJsonObj(sspectext,  ServiceOrder.class);
		responseSO.setState( ServiceOrderStateType.REJECTED  );
		
		//reject one order
		template.sendBody( "jms:queue:OSOM.IN.ACK_SERVICEORDER_PROCESS", toJsonString( responseSO ));

        Thread.sleep(1000); //wait
		assertThat( taskService.createTaskQuery().count()  ).isEqualTo( 1 );
        

		ServiceOrder accSO = toJsonObj(sspectext,  ServiceOrder.class);
		accSO.setState( ServiceOrderStateType.ACKNOWLEDGED  );
		
		//accept the last order
		template.sendBody( "jms:queue:OSOM.IN.ACK_SERVICEORDER_PROCESS", toJsonString( accSO ));

        Thread.sleep(1000); //wait
		assertThat( taskService.createTaskQuery().count()  ).isEqualTo( 0 );

		verify( rejectServiceOrderBean, times(1)).execute(Mockito.any());
		verify( addServiceOrderToScheduler, times(1)).execute(Mockito.any());
		

        logger.info("WAIT SHUTDOWN");
        context.stop();
        logger.info("EXIT TEST CASE");
	}
	
	static <T> T toJsonObj(String content, Class<T> valueType)  throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.readValue( content, valueType);
    }
	
	 static String toJsonString(Object object) throws IOException {
	        ObjectMapper mapper = new ObjectMapper();
	        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	        return mapper.writeValueAsString(object);
	    }
	
}