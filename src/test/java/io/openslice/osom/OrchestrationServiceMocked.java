package io.openslice.osom;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;


public class OrchestrationServiceMocked implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(OrchestrationServiceMocked.class.getName());

	public void execute(DelegateExecution execution) {

		logger.info("OrchestrationServiceMocked:" + execution.getVariableNames() );

		if (execution.getVariable("orderid") instanceof String) {
			
			

			logger.info("MOCKED Orchestration of order with id = " + execution.getVariable("orderid") + ". FINISHED!");
		}
	}

}
