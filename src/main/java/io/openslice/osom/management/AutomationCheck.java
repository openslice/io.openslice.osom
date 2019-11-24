package io.openslice.osom.management;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component(value = "automationCheck") // bean name
public class AutomationCheck implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(AutomationCheck.class.getName());

	public void execute(DelegateExecution execution) {

		logger.info("Process Orders by Orchetrator:" + execution.getVariableNames().toString() );
		
//		List<String> ordersToBeProcessed =  (ArrayList<String>) execution.getVariable("ordersToBeProcessed");
//		
//    	for (String o : ordersToBeProcessed) {
//    		logger.info("Will Process Orders by Orchetrator  orderid = " + o);
//		}
    	
		if (execution.getVariable("orderid") instanceof String) {
			logger.info("Will process/orchestrate order with id = " + execution.getVariable("orderid") );
			
		
		}
	}

}
