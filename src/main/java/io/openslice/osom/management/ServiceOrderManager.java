package io.openslice.osom.management;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.osom.configuration.OSOMRouteBuilder;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderStateType;

/**
 * @author ctranoris
 *
 */
@Service
public class ServiceOrderManager {

	private static final transient Log logger = LogFactory.getLog(ServiceOrderManager.class.getName());

	private static final String ORDER_ASSIGNEE = "admin";

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;

    @Autowired
    private ProducerTemplate template;
    


	@Value("${ENDPOINT_CATALOG_GET_SERVICEORDER_BY_ID}")
	private String ENDPOINT_CATALOG_GET_SERVICEORDER_BY_ID = "";
	

	@Value("${ENDPOINT_CATALOG_GET_SERVICESPEC_BY_ID}")
	private String ENDPOINT_CATALOG_GET_SERVICESPEC_BY_ID = "";
	
	@Value("${ENDPOINT_CATALOG_GET_INITIAL_SERVICEORDERS}")
	private String ENDPOINT_CATALOG_GET_INITIAL_SERVICEORDERS = "";
	
	

	@Transactional
	public void processOrder(ServiceOrder serviceOrder) {

		logger.info("Received order to process serviceOrder id : " + serviceOrder.getId());
		Map<String, Object> variables = new HashMap<>();
		variables.put("orderid", serviceOrder.getId());

		runtimeService.startProcessInstanceByKey("InitialServiceOrderAckProcess", variables);
	}

	@Transactional
	public List<String> getTasks(String assignee) {
		/**
		 * we ignore for now the assignee
		 */
		String assign = ORDER_ASSIGNEE;
		logger.info("Received order to getTasks, assignee : " + assign);
		List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(assign).list();

		List<String> orders = tasks.stream().map(task -> {
			Map<String, Object> variables = taskService.getVariables(task.getId());
			return (String) variables.get("orderid");
		}).collect(Collectors.toList());

		logger.info("orderid(s) : " + orders.toString());
		return orders;
	}

	@Transactional
	public void submitReview(ServiceOrder serviceOrder) {
		/**
		 * we ignore for now the assignee
		 */
		String assignee = ORDER_ASSIGNEE;

//		 {
//		  "id": "b0661e27-020f-4026-84ab-5c265bac47e7",
//		  "status": "true",
//		 "assignee": "admin"
//		}
		List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(assignee).list();
		String taskId = null;
		for (Task t : tasks) {
			Map<String, Object> variables = taskService.getVariables(t.getId());
			if (variables.get("orderid").equals(serviceOrder.getId())) {
				taskId = t.getId();
				break;
			}
		}

		if (taskId != null) {
			boolean approve = serviceOrder.getState().equals(ServiceOrderStateType.ACKNOWLEDGED);
			logger.info("Received OrderApproval for orderid=" + serviceOrder.getId() + " status= " + approve);
			Map<String, Object> variables = new HashMap<String, Object>();
			variables.put("approved", approve);
			taskService.complete(taskId, variables);
		} else {

			logger.error("Task ID cannot be found for received OrderApproval for orderid=" + serviceOrder.getId());
		}

	}

	@Transactional
	public void humanComplete(String id) {
		logger.info("Received Order manual complete for orderid=" +id );
		/**
		 * we ignore for now the assignee
		 */
		String assignee = ORDER_ASSIGNEE;

		List<Task> tasks = taskService.createTaskQuery()
				.taskDefinitionKey("usertaskManualCompleteOrder")
				// .taskCandidateGroup( assignee )
				.list();
		String taskId = null;

		for (Task t : tasks) {
			logger.info("PENDING humanComplete t.id=" + t.getId() + "" + "orderid=" + taskService.getVariables(t.getId()).get("orderid") );
			String orderid=  (String) taskService.getVariables(t.getId()).get("orderid");
			if ( orderid.contains( id )) {
				taskId = t.getId();
				if (taskId != null) {

					logger.info("will complete orderid=" +id );
					taskService.complete(taskId);
				} else {

					logger.error("Task ID cannot be found for received OrderApproval for orderid=" + id);
				}
			}
			

		}

	}

	/**
	 * Request orders to be processed
	 * @return a string list of Order IDs
	 */
	public List<String> retrieveOrdersToBeProcessed() {
		logger.info("will retrieve Service Orders to be processed from catalog "   );
		try {
			Object response = template.
					requestBody( ENDPOINT_CATALOG_GET_INITIAL_SERVICEORDERS );

			if ( !(response instanceof List)) {
				logger.error("List  object is wrong.");
				return null;
			}
			ArrayList<String> sor = toJsonObj( (String)response, ArrayList.class ); 
			return sor;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Listof Service Orders to be processed from catalog. " + e.toString());
		}
		return null;
	}

	/**
	 * get  service order by id from model via bus
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public ServiceOrder retrieveServiceOrder( String orderid) {
		logger.info("will retrieve Service Order from catalog orderid=" + orderid   );
		try {
			Object response = template.
					requestBody( ENDPOINT_CATALOG_GET_SERVICEORDER_BY_ID, orderid);

			if ( !(response instanceof String)) {
				logger.error("Service Order object is wrong.");
				return null;
			}
			ServiceOrder sor = toJsonObj( (String)response, ServiceOrder.class); 
			return sor;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Service Order details from catalog. " + e.toString());
		}
		return null;
	}



	/**
	 * get  service spec by id from model via bus
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public ServiceSpecification retrieveSpec(String specid) {
		logger.info("will retrieve Service Specification from catalog orderid=" + specid   );
		
		try {
			Object response = template.
					requestBody( ENDPOINT_CATALOG_GET_SERVICESPEC_BY_ID, specid);

			if ( !(response instanceof String)) {
				logger.error("Service Specification object is wrong.");
				return null;
			}
			ServiceSpecification sor = toJsonObj( (String)response, ServiceSpecification.class); 
			return sor;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Service Specification details from catalog. " + e.toString());
		}
		return null;
	}
	
	
	static <T> T toJsonObj(String content, Class<T> valueType)  throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.readValue( content, valueType);
    }


}
