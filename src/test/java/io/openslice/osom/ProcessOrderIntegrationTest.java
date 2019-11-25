package io.openslice.osom;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(FlowableSpringExtension.class)
@SpringBootTest
@ActiveProfiles("testing")
public class ProcessOrderIntegrationTest {
	private static final transient Log logger = LogFactory.getLog( ProcessOrderIntegrationTest.class.getName());

	@Autowired
	RepositoryService repositoryService;
	
	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;
	
	@Test
	//@Deployment(resources = { "processes/ServiceOrder.bpmn" })
	public void startProcess() throws Exception {
		File sspec = new File( "src/test/resources/TestExServiceOrder.json" );
		InputStream in = new FileInputStream( sspec );
		String sspectext = IOUtils.toString(in, "UTF-8");
		

		assertThat( repositoryService.createProcessDefinitionQuery().count()  ).isEqualTo(3 );
		assertThat( taskService.createTaskQuery().count()  ).isEqualTo( 0 );
		repositoryService.suspendProcessDefinitionByKey("OrderSchedulerProcess");
		

		logger.info("waiting 4secs");
        Thread.sleep(4000); //wait
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderid", "b0661e27-020f-4026-84ab-5c265bac47e7" );
        runtimeService.startProcessInstanceByKey("StartOrderProcess", variables);
		
	}
}
