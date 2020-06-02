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

import io.openslice.model.DeploymentDescriptor;
import io.openslice.model.NetworkServiceDescriptor;
import io.openslice.osom.configuration.OSOMRouteBuilder;
import io.openslice.tmf.pm632.model.Organization;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.ServiceCreate;
import io.openslice.tmf.sim638.model.ServiceUpdate;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderStateType;
import io.openslice.tmf.so641.model.ServiceOrderUpdate;

import static java.util.Arrays.asList;

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
    


	@Value("${CATALOG_GET_SERVICEORDER_BY_ID}")
	private String CATALOG_GET_SERVICEORDER_BY_ID = "";
	

	@Value("${CATALOG_GET_SERVICESPEC_BY_ID}")
	private String CATALOG_GET_SERVICESPEC_BY_ID = "";
	
//	@Value("${CATALOG_GET_INITIAL_SERVICEORDERS_IDS}")
//	private String CATALOG_GET_INITIAL_SERVICEORDERS_IDS = "";

	@Value("${CATALOG_GET_SERVICEORDER_IDS_BY_STATE}")
	private String CATALOG_GET_SERVICEORDER_IDS_BY_STATE = "";
	
	@Value("${CATALOG_UPD_SERVICEORDER_BY_ID}")
	private String CATALOG_UPD_SERVICEORDER_BY_ID = "";
	
	@Value("${CATALOG_ADD_SERVICE}")
	private String CATALOG_ADD_SERVICE = "";

	@Value("${CATALOG_UPD_SERVICE}")
	private String CATALOG_UPD_SERVICE = "";

	@Value("${CATALOG_GET_SERVICE_BY_ID}")
	private String CATALOG_GET_SERVICE_BY_ID = "";

	@Value("${NFV_CATALOG_DEPLOY_NSD_REQ}")
	private String NFV_CATALOG_DEPLOY_NSD_REQ = "";
	

	@Value("${NFV_CATALOG_GET_DEPLOYMENT_BY_ID}")
	private String NFV_CATALOG_GET_DEPLOYMENT_BY_ID = "";
	

	@Value("${NFV_CATALOG_GET_NSD_BY_ID}")
	private String NFV_CATALOG_GET_NSD_BY_ID = "";

	@Value("${CATALOG_GET_PARTNER_ORGANIZATON_BY_ID}")
	private String CATALOG_GET_PARTNER_ORGANIZATON_BY_ID = "";
	
	
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

//	/**
//	 * Request orders to be processed
//	 * @return a string list of Order IDs
//	 */
//	public List<String> retrieveOrdersToBeProcessed() {
//		logger.info("will retrieve Service Orders to be processed from catalog "   );
//		try {
//			Map<String, Object> map = new HashMap<>();
//			map.put("orderstate", ServiceOrderStateType.ACKNOWLEDGED.toString() );
//			
//			Object response = template.
//					requestBodyAndHeaders( CATALOG_GET_SERVICEORDER_IDS_BY_STATE, "", map );
//
//			logger.info("will retrieve Service OrdersACKNOWLEDGED from catalog response: " + response.getClass()  );
//			if ( !(response instanceof String)) {
//				logger.error("List  object is wrong.");
//				return null;
//			}
//			//String[] sor = toJsonObj( (String)response, String[].class );
//
//			ArrayList<String> sor = toJsonObj( (String)response, ArrayList.class ); 
//			logger.debug("retrieveOrdersToBeProcessed response is: " + response);
//			
////			return asList(sor);
//			return sor;
//			
//		}catch (Exception e) {
//			logger.error("Cannot retrieve Listof Service Orders ACKNOWLEDGED from catalog. " + e.toString());
//		}
//		return null;
//	}
	
	
	/**
	 * @return
	 */
	public List<String> retrieveOrdersByState(ServiceOrderStateType orderState) {
		logger.info("will retrieve Service Orders " + orderState.toString() + " from catalog "   );
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("orderstate", orderState.toString() );
			Object response = template.
					requestBodyAndHeaders( CATALOG_GET_SERVICEORDER_IDS_BY_STATE, "", map );

			logger.debug("will retrieve Service Orders " + orderState.toString() + " from catalog response: " + response.getClass()  );
			if ( !(response instanceof String)) {
				logger.error("List  object is wrong.");
				return null;
			}
			//String[] sor = toJsonObj( (String)response, String[].class );

			ArrayList<String> sor = toJsonObj( (String)response, ArrayList.class ); 
			logger.debug("retrieveOrdersByState response is: " + response);
			
//			return asList(sor);
			return sor;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Listof Service Orders "+ orderState.toString() + " from catalog. " + e.toString());
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
					requestBody( CATALOG_GET_SERVICEORDER_BY_ID, orderid);
			
			if ( !(response instanceof String)) {
				logger.error("Service Order object is wrong.");
				return null;
			}
			logger.debug("retrieveServiceOrder response is: " + response);
			ServiceOrder sor = toJsonObj( (String)response, ServiceOrder.class); 
			
			return sor;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Service Order details from catalog. " + e.toString());
		}
		return null;
	}


	
	public void updateServiceOrderOrder(String orderid, ServiceOrderUpdate serviceOrder) {
		logger.info("will set Service Order in progress orderid=" + orderid   );
		try {

			template.sendBodyAndHeader( CATALOG_UPD_SERVICEORDER_BY_ID, toJsonString(serviceOrder), "orderid", orderid);

			
		}catch (Exception e) {
			logger.error("Cannot set Service Order status from catalog. " + e.toString());
		}
		
	}

	/**
	 * get  service spec by id from model via bus
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public ServiceSpecification retrieveServiceSpec(String specid) {
		logger.info("will retrieve Service Specification from catalog orderid=" + specid   );
		
		try {
			Object response = template.
					requestBody( CATALOG_GET_SERVICESPEC_BY_ID, specid);

			if ( !(response instanceof String)) {
				logger.error("Service Specification object is wrong.");
				return null;
			}
			ServiceSpecification sor = toJsonObj( (String)response, ServiceSpecification.class); 
			//logger.debug("retrieveSpec response is: " + response);
			return sor;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Service Specification details from catalog. " + e.toString());
		}
		return null;
	}
	
	

	public io.openslice.tmf.sim638.model.Service createService( ServiceCreate s, ServiceOrder sor, ServiceSpecification spec) {
		logger.info("will create Service for spec: " + spec.getId() );
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("orderid", sor.getId() );
			map.put("serviceSpecid", spec.getId() );
			Object response = template.requestBodyAndHeaders( CATALOG_ADD_SERVICE, toJsonString(s), map);

			if ( !(response instanceof String)) {
				logger.error("Service Instance object is wrong.");
			}

			io.openslice.tmf.sim638.model.Service serviceInstance = toJsonObj( (String)response, io.openslice.tmf.sim638.model.Service.class); 
			//logger.debug("createService response is: " + response);
			return serviceInstance;
			
			
		}catch (Exception e) {
			logger.error("Cannot create Service for spec " + spec.getId()+ ": " + e.toString());
		}
		return null;
		
	}
	
	public io.openslice.tmf.sim638.model.Service updateService(String serviceId, ServiceUpdate s) {
		logger.info("will update Service : " + serviceId );
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("serviceid", serviceId );
			Object response = template.requestBodyAndHeaders( CATALOG_UPD_SERVICE, toJsonString(s), map);

			if ( !(response instanceof String)) {
				logger.error("Service Instance object is wrong.");
			}

			io.openslice.tmf.sim638.model.Service serviceInstance = toJsonObj( (String)response, io.openslice.tmf.sim638.model.Service.class); 
			//logger.debug("createService response is: " + response);
			return serviceInstance;
			
			
		}catch (Exception e) {
			logger.error("Cannot update Service: " + serviceId + ": " + e.toString());
		}
		return null;
		
	}
	

	
	
	/**
	 * Ger service instance via bus
	 * @param serviceID
	 * @return
	 */
	public io.openslice.tmf.sim638.model.Service retrieveService(String serviceID) {
		logger.info("will retrieve Service instance from catalog serviceID=" + serviceID   );
		try {
			Object response = template.
					requestBody( CATALOG_GET_SERVICE_BY_ID, serviceID);

			if ( !(response instanceof String)) {
				logger.error("Service object is wrong.");
				return null;
			}
			io.openslice.tmf.sim638.model.Service serviceInstance = toJsonObj( (String)response, io.openslice.tmf.sim638.model.Service.class); 
			//logger.debug("retrieveService response is: " + response);
			return serviceInstance;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Service details from catalog. " + e.toString());
		}
		return null;
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

	public DeploymentDescriptor nfvoDeploymentRequestByNSDid( DeploymentDescriptor ddreq ) {
		
		
		logger.info("Will request by NFV Catalog to deploy NSD id= " + ddreq.getExperiment().getId()   );
		
		try {

			String body = toJsonString(ddreq);
			Object response = template.requestBodyAndHeader( NFV_CATALOG_DEPLOY_NSD_REQ, body , "id", ddreq.getExperiment().getId());

			if ( !(response instanceof String)) {
				logger.error("DeploymentDescriptor object is wrong.");
				return null;
			}
			DeploymentDescriptor dd = toJsonObj( (String)response, DeploymentDescriptor.class); 
			logger.debug("nfvoDeploymentRequestByNSDid response is: " + response);
			return dd;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve DeploymentDescriptor details from NFV catalog. " + e.toString());
			e.printStackTrace();
		}
		return null;
	}

	public DeploymentDescriptor retrieveNFVODeploymentRequestById(long deploymentId) {

		logger.info("Will request by NFV Catalog detaile of DeploymentRequest= " + deploymentId );
		
		try {

			Object response = template.requestBody( NFV_CATALOG_GET_DEPLOYMENT_BY_ID, deploymentId );

			if ( !(response instanceof String)) {
				logger.error("DeploymentDescriptor object is wrong.");
				return null;
			}
			DeploymentDescriptor dd = toJsonObj( (String)response, DeploymentDescriptor.class); 
			logger.debug("retriveNFVODeploymentRequestById response is: " + response);
			return dd;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve DeploymentDescriptor details from NFV catalog. " + e.toString());
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * get  service order by id from model via bus
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public NetworkServiceDescriptor retrieveNSD( String nsdID) {
		logger.info("will retrieve NetworkServiceDescriptor from NSD/VNF catalog nsdID=" + nsdID   );
		try {
			Object response = template.
					requestBody( NFV_CATALOG_GET_NSD_BY_ID, nsdID);

			if ( !(response instanceof String)) {
				logger.error("NetworkServiceDescriptor object is wrong.");
				return null;
			}
			NetworkServiceDescriptor sor = toJsonObj( (String)response, NetworkServiceDescriptor.class); 
			//logger.debug("retrieveServiceOrder response is: " + response);
			return sor;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve NetworkServiceDescriptor details from catalog. " + e.toString());
		}
		return null;
	}

	public Organization getExternalPartnerOrganization(String partnerId) {
		
		logger.info("will retrieve External Partner from catalog partnerId=" + partnerId   );
		
		try {
			Object response = template.
					requestBody( CATALOG_GET_PARTNER_ORGANIZATON_BY_ID, partnerId);

			if ( !(response instanceof String)) {
				logger.error("External Partner  object is wrong.");
				return null;
			}
			Organization orgz = toJsonObj( (String)response, Organization.class); 
			//logger.debug("retrieveSpec response is: " + response);
			return orgz;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve External Partner details from catalog. " + e.toString());
		}
		return null;
	}

	



}
