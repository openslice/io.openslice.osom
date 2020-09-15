package io.openslice.osom.serviceactions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.tmf.pm632.model.Organization;
import io.openslice.tmf.sim638.model.ServiceActionQueueItem;

@Component(value = "fetchServiceQueueItems") // bean name
public class FetchServiceQueueItems implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(FetchServiceQueueItems.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;


	public void execute(DelegateExecution execution) {
		logger.info("FetchServiceQueueItems by Service Inventory Repository");
		

		List<ServiceActionQueueItem> itemsToBeProcessed = serviceOrderManager.retrieveServiceQueueItems();
		if ( itemsToBeProcessed!= null ) {
			for (ServiceActionQueueItem serviceActionQueueItem : itemsToBeProcessed) {
				logger.info("FetchServiceQueueItems serviceActionQueueItem getServiceRefId = " + serviceActionQueueItem.getServiceRefId());
				
			}			
		} 
		
		
		List<String> itemListAsString = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			if ( itemsToBeProcessed!=null) {			
				for (ServiceActionQueueItem item : itemsToBeProcessed) {
					String o = mapper.writeValueAsString(item);
					itemListAsString.add(o);
				}	
			}

			execution.setVariable("serviceActionsToBeProcessed", itemListAsString);

		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
