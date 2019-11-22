package io.openslice.osom.management;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

public class OrderCompleteService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(OrderCompleteService.class.getName());

	public void execute(DelegateExecution execution) {

		logger.info("OrderCompleteService:" + execution.getVariableNames().toString() );

		
	}

}
