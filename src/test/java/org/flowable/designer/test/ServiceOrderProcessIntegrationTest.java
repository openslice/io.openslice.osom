package org.flowable.designer.test;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@ExtendWith(FlowableSpringExtension.class)
@SpringBootTest
public class ServiceOrderProcessIntegrationTest {

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;

	@Test
	@Deployment(resources = { "processes/ServiceOrder.bpmn" })
	public void startProcess() throws Exception {
//		File sspec = new File( "src/main/resources/ServiceOrder.bpmn" );
//		InputStream in = new FileInputStream( sspec );

		
		Map<String, Object> variableMap = new HashMap<String, Object>();
		variableMap.put("orderid", "10007");
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskAckServiceOrder",
				variableMap);
		assertNotNull(processInstance.getId());
		System.out.println("id " + processInstance.getId() + " " + processInstance.getProcessDefinitionId());

//		Task task = taskService.createTaskQuery().singleResult();
//		  
//        assertEquals("Review the submitted tutorial", task.getName());
//  
//        variables.put("approved", true);
//        taskService.complete(task.getId(), variables);
//  
//        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
	}
}