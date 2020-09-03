package io.openslice.osom.serviceactions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.openslice.osom.management.ServiceOrderManager;

@Component(value = "HandleManuallyAction") //bean name
public class HandleManuallyAction  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( HandleManuallyAction.class.getName() );


    @Autowired
    private ServiceOrderManager serviceOrderManager;
    
	public void execute(DelegateExecution execution) {
		
		logger.info("HandleManuallyAction:" + execution.getVariableNames().toString() );
		
	}

}
