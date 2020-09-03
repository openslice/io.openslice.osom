package io.openslice.osom.serviceactions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.openslice.osom.management.ServiceOrderManager;

@Component(value = "NFVODAY2config") //bean name
public class NFVODAY2config implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( NFVODAY2config.class.getName() );


    @Autowired
    private ServiceOrderManager serviceOrderManager;
    
	public void execute(DelegateExecution execution) {
		
		logger.info("NFVODAY2config:" + execution.getVariableNames().toString() );
		
	}

}
