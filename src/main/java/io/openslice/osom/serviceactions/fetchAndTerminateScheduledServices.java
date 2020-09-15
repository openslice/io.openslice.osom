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
import io.openslice.tmf.common.model.service.ServiceStateType;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceActionQueueItem;
import io.openslice.tmf.sim638.model.ServiceUpdate;

@Component(value = "fetchAndTerminateScheduledServices") // bean name
public class fetchAndTerminateScheduledServices implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(FetchServiceQueueItems.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;


	public void execute(DelegateExecution execution) {
		logger.info("fetchAndTerminateScheduledServices by Service Inventory Repository");
		

		List<String> itemsToBeProcessed = serviceOrderManager.retrieveActiveServiceToTerminate();
		
		if ( itemsToBeProcessed != null ) {
			for (String serviceID : itemsToBeProcessed) {
				logger.info("Will TERMINATE service with id: " + serviceID );
				
				Service aService = serviceOrderManager.retrieveService(serviceID);
				ServiceUpdate supd = new ServiceUpdate();
				supd.setState( ServiceStateType.TERMINATED );
				serviceOrderManager.updateService(serviceID, supd , true);
				
			}			
		}


	}

}
