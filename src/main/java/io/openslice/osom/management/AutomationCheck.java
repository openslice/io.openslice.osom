package io.openslice.osom.management;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.tmf.scm633.model.ServiceSpecRelationship;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderItem;
import io.openslice.tmf.so641.model.ServiceRelationship;

@Component(value = "automationCheck") // bean name
public class AutomationCheck implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(AutomationCheck.class.getName());

    
	

    @Autowired
    private ServiceOrderManager serviceOrderManager;
	
	public void execute(DelegateExecution execution) {

		logger.info("Process Orders by Orchetrator:" + execution.getVariableNames().toString() );
    	
		if (execution.getVariable("orderid") instanceof String) {
			logger.info("Will process/orchestrate order with id = " + execution.getVariable("orderid") );
			ServiceOrder sor = serviceOrderManager.retrieveServiceOrder( (String) execution.getVariable("orderid") );
						
			if ( sor == null) {
				logger.error("Cannot retrieve Service Order details from catalog.");
				return;
			}
			
			logger.debug("ServiceOrder id:" + sor.getId() );
			logger.debug("ServiceOrder Description:" + sor.getDescription());
			logger.debug("Examin service items" );
			
			for (ServiceOrderItem soi : sor.getOrderItem()) {
				logger.debug("Service Item ID:" + soi.getId()  );
				logger.debug("Service spec ID:" + soi.getService().getServiceSpecification().getId()   );
				
				//get service spec by id from model via bus, find if bundle and analyse its related services
				ServiceSpecification spec = serviceOrderManager.retrieveSpec( soi.getService().getServiceSpecification().getId() );
				
				logger.debug("Retrieved Service ID:" + spec.getId()    );
				logger.debug("Retrieved Service Name:" + spec.getName()    );

				logger.debug("<--------------- related specs -------------->");
				for (ServiceSpecRelationship specRels : spec.getServiceSpecRelationship()) {
					logger.debug("\tService specRelsId:" + specRels.getId()   );		
					ServiceSpecification specrel = serviceOrderManager.retrieveSpec( specRels.getId() );
					logger.debug("\tService spec name :" + specrel.getName()  );		
					logger.debug("\tService spec type :" + specrel.getType()   );		
					
				}
				logger.debug("<--------------- /related specs -------------->");
				
//				for (ServiceRelationship rels : soi.getService().getServiceRelationship() ) {
//					logger.info("Service rels:" + rels.getService().getName()    );					
//				}
				
				
			}
			
		}
	}
	
	

}
