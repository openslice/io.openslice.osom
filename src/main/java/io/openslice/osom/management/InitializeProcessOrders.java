package io.openslice.osom.management;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "initializeProcessOrders") //bean name
public class InitializeProcessOrders  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(InitializeProcessOrders.class.getName());

	@Autowired
	private RuntimeService runtimeService;
	
    public void execute(DelegateExecution execution) {
    	
        logger.info("Push Process Orders for Orchetration");
        if( execution.getVariable("ordersToBeProcessed") instanceof ArrayList) {
        	
        	List<String> ordersToBeProcessed =  (ArrayList<String>) execution.getVariable("ordersToBeProcessed");
        	
        	if ( ordersToBeProcessed.size()>0 ) {
        		logger.info("Will push to SO orderid = " + ordersToBeProcessed.get(0));
        		execution.setVariable("orderid", ordersToBeProcessed.get(0));//get the first one
        	}
        	
        	
    	    
        }
    }
}
