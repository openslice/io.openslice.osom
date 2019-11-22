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

@Component(value = "fetchAcknowledgedOrders") //bean name
public class FetchAcknowledgedOrders  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(FetchAcknowledgedOrders.class.getName());


    @Autowired
    private ProducerTemplate template;
	
    public void execute(DelegateExecution execution) {
    	logger.info("fetchAcknowledgedOrders by Service Order Repository");
    	
    	// set the defautlEndPoint
//    	template.setDefaultEndpointUri("direct:start");
//    	try {
//        	Object response = template.requestBody( "jms:queue:SC.IN.SERVICEORDERS.LIST_ACK_PAST");
//        	logger.info("fetchAcknowledgedOrders by Service Order Repository response = " + response);    		
//    	} catch (Exception e) {
//
//        	logger.error("fetchAcknowledgedOrders by Service Order Repository");
//		}
    	
    	 if( execution.getVariable("ordersToBeProcessed") instanceof ArrayList) {
         	
         	List<String> ordersFromPrevious =  (ArrayList<String>) execution.getVariable("ordersToBeProcessed");
         	for (String orderid : ordersFromPrevious) {
         		logger.info("ordersFromPrevious = " + orderid);
         	}
    	 }
    	
    	List<String> ordersToBeProcessed = new ArrayList<>();
    	ordersToBeProcessed.add("ORDER-ID-A" + execution.getId()   );
    	ordersToBeProcessed.add("ORDER-ID-B" + execution.getId() );
    	
    	execution.setVariable("ordersToBeProcessed", ordersToBeProcessed);

    }
}
