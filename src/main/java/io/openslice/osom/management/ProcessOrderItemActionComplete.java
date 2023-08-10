package io.openslice.osom.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.openslice.osom.lcm.LCMRulesController;

@Component(value = "processOrderItemActionComplete") // bean name
public class ProcessOrderItemActionComplete implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( ProcessOrderItemActionComplete.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;


	@Autowired
	private LCMRulesController lcmRulesController;
	
	
	@Value("${spring.application.name}")
	private String compname;
	
	public void execute(DelegateExecution execution) {

		logger.info("ProcessOrderItemActionComplete:" + execution.getVariableNames().toString());


	}

}
