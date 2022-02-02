package io.openslice.osom.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceUpdate;
import io.openslice.tmf.so641.model.ServiceOrder;

@Component(value = "initializeServiceTestOrchestration") //bean name
public class InitializeServiceTestOrchestration  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( CheckServiceTestDeployment.class.getName());
	
	@Autowired
	private ServiceOrderManager serviceOrderManager;

	@Value("${spring.application.name}")
	private String compname;
	
	@Override
	public void execute(DelegateExecution execution) {
		
		
		logger.info( "initializeServiceTestOrchestration" );
		logger.info( "VariableNames:" + execution.getVariableNames().toString() );
		logger.info("orderid:" + execution.getVariable("orderid").toString() );
		logger.info("contextServiceId:" + execution.getVariable("contextServiceId").toString() );
				

		if (execution.getVariableLocal("contextServiceId") instanceof String) {

//			ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder( execution.getVariable("orderid").toString() );
//			Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("contextServiceId") );
//			logger.info("Service name:" + aService.getName() );
//			logger.info("Service state:" + aService.getState()  );			
//			logger.info("Request to External Service Partner for Service: " + aService.getId() );


		} else {
			logger.error( "Cannot retrieve variable contextServiceId"  );
		}

	}
}
