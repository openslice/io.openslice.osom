package io.openslice.osom.serviceactions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.tmf.common.model.service.ResourceRef;
import io.openslice.tmf.common.model.service.ServiceRef;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceActionQueueAction;
import io.openslice.tmf.sim638.model.ServiceActionQueueItem;

@Component(value = "serviceActionCheck") //bean name
public class ServiceActionCheck implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( ServiceActionCheck.class.getName() );


    @Autowired
    private ServiceOrderManager serviceOrderManager;
    
	public void execute(DelegateExecution execution) {

		logger.info("ServiceActionCheck:" + execution.getVariableNames().toString() );
		
		
		
		if (execution.getVariable("serviceActionItem")!=null) {
			logger.info("Will check status of serviceActionItem of ref:" + execution.getVariable("serviceActionItem") );
			ObjectMapper mapper = new ObjectMapper();
			
			ServiceActionQueueItem item;
			try {
				item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
			} catch (JsonMappingException e) {
				e.printStackTrace();
				return;
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return;
			}
			
			
			Service aService = serviceOrderManager.retrieveService( item.getServiceRefId() );
			
			if ( aService.getSupportingService()!=null) {
				for (ServiceRef aSupportingService : aService.getSupportingService() ) {
					logger.info("aSupportingService:" + aSupportingService.getName() );				
				}
			}

			if ( aService.getSupportingResource()!=null) {
				for (ResourceRef aSupportingResource : aService.getSupportingResource() ) {
					logger.info("aSupportingResource:" + aSupportingResource.getName() );				
				}				
				
			}

			if ( aService.getStartMode().equals( "AUTOMATICALLY_MANAGED" ) ) {
				
				if ( (aService.getServiceCharacteristicByName( "externalServiceOrderId" ) != null )){
					execution.setVariable("saction", "ExternalProviderServiceAction");					
				} else  if ( aService.getCategory().equals( "CustomerFacingServiceSpecification") ) {
					execution.setVariable("saction", "AutomaticallyHandleAction");
				} else if ( aService.getCategory().equals( "ResourceFacingServiceSpecification") ) {
					
					if (aService.getServiceCharacteristicByName( "NSDID" ) != null ){
						if ( item.getAction().equals( ServiceActionQueueAction.DEACTIVATE ) || item.getAction().equals( ServiceActionQueueAction.TERMINATE ) ) {
							execution.setVariable("saction", "NFVONSTerminate");
						} else if (  item.getAction().equals( ServiceActionQueueAction.MODIFY ) ) {
							execution.setVariable("saction", "NFVODAY2config");
						} 
					} else {
						execution.setVariable("saction", "AutomaticallyHandleAction");
					}					
					
				}
				
			} else {
				execution.setVariable("saction", "HandleManuallyAction");
			}
			


			try {
				String srv = mapper.writeValueAsString( aService );
				execution.setVariable("Service", srv);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
		}
	}

}
