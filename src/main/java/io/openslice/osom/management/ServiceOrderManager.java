package io.openslice.osom.management;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.openslice.osom.configuration.OSOMRouteBuilder;
import io.openslice.tmf.so641.model.ServiceOrder;

/**
 * @author ctranoris
 *
 */
@Service
public class ServiceOrderManager {

	private static final transient Log logger = LogFactory.getLog(ServiceOrderManager.class.getName());

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;

	@Transactional
	public void processOrder(ServiceOrder serviceOrder) {

		logger.info("Received order to process serviceOrder id : " + serviceOrder.getId());
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderid", serviceOrder.getId() );
        

        runtimeService.startProcessInstanceByKey("InitialServiceOrderAckProcess", variables);
	}
	
	@Transactional
    public List<String> getTasks(String assignee) {
		logger.info("Received order to getTasks, assignee : " + assignee);
        List<Task> tasks = taskService.createTaskQuery()
          .taskCandidateGroup(assignee)
          .list();
        
        List<String> orders = tasks.stream()
          .map(task -> {
              Map<String, Object> variables = taskService.getVariables(task.getId());
              return (String) variables.get("orderid") ;
          })
          .collect(Collectors.toList());

		logger.info("orderid(s) : " + orders.toString());
        return orders;
    }
	
	 @Transactional
	public void submitReview(OrderApproval approval) {
//		 {
//		  "id": "b0661e27-020f-4026-84ab-5c265bac47e7",
//		  "status": "true",
//		 "assignee": "admin"
//		}
		 List<Task> tasks = taskService.createTaskQuery()
		          .taskCandidateGroup(approval.getAssignee())
		          .list();
		 String taskId = null;
		 for (Task t : tasks) {
			 Map<String, Object> variables = taskService.getVariables( t.getId() );
			 if ( variables.get("orderid").equals(approval.getId())) {
				 taskId =   t.getId() ;
				 break;
             }
		 }
		
		 if ( taskId!=null ) {

				logger.info("Received OrderApproval for orderid="+ approval.getId() + " status= " + approval.isStatus() );
		        Map<String, Object> variables = new HashMap<String, Object>();
		        variables.put("approved", approval.isStatus());
		        taskService.complete( taskId , variables);			 
		 } else {

				logger.error("Task ID cannot be found for received OrderApproval for orderid="+ approval.getId() );
		 }
	 
	    }
	
}
