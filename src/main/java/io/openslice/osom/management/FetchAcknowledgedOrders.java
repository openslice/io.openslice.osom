package io.openslice.osom.management;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "fetchAcknowledgedOrders") // bean name
public class FetchAcknowledgedOrders implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(FetchAcknowledgedOrders.class.getName());


    @Autowired
    private ServiceOrderManager serviceOrderManager;

	public void execute(DelegateExecution execution) {
		logger.info("fetchAcknowledgedOrders by Service Order Repository");

		List<String> ordersToBeProcessed = null;
		if (execution.getVariable("ordersToBeProcessed") instanceof ArrayList) {
			ordersToBeProcessed = (ArrayList<String>) execution.getVariable("ordersToBeProcessed");
			for (String orderid : ordersToBeProcessed) {
				logger.info("ordersFromPrevious = " + orderid);
			}
		} else {
			ordersToBeProcessed = new ArrayList<>();
		}

		List<String> orderlist = serviceOrderManager.retrieveOrdersToBeProcessed();
		
		for (String orderid : orderlist) {
			if ( !ordersToBeProcessed.contains( orderid )  ) {
				ordersToBeProcessed.add( orderid );
				
			}
		}
		
		execution.setVariable("ordersToBeProcessed", ordersToBeProcessed);

	}
}
