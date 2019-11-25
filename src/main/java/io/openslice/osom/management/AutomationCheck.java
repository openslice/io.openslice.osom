package io.openslice.osom.management;

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

		if (execution.getVariable("orderid") instanceof String) {
			
//			try {
//				long completionTime = RandomUtils.nextLong(5000, 10000);
//
//				logger.info("Will process/orchestrate order with id = " + execution.getVariable("orderid") + ". Completion in: " + completionTime);
//				Thread.sleep( completionTime  );
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}

			logger.info("Will process/orchestrate order with id = " + execution.getVariable("orderid") );
		}
	}

}
