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

import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderItem;
import io.openslice.tmf.so641.model.ServiceRelationship;

@Component(value = "automationCheck") // bean name
public class AutomationCheck implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(AutomationCheck.class.getName());

    @Autowired
    private ProducerTemplate template;
    
	@Value("${ENDPOINT_CATALOG_GET_SERVICEORDER_BY_ID}")
	private String ENDPOINT_CATALOG_GET_SERVICEORDER_BY_ID = "";
	
	public void execute(DelegateExecution execution) {

		logger.info("Process Orders by Orchetrator:" + execution.getVariableNames().toString() );
    	
		if (execution.getVariable("orderid") instanceof String) {
			logger.info("Will process/orchestrate order with id = " + execution.getVariable("orderid") );
			Object response = null;
			ServiceOrder sor = null;
			try {
				response = template.
						requestBody( ENDPOINT_CATALOG_GET_SERVICEORDER_BY_ID, execution.getVariable("orderid"));

				if ( !(response instanceof String)) {
					logger.error("Service Order object is wrong.");
					return;
				}
				sor = toJsonObj( (String)response, ServiceOrder.class); 
				
				
			}catch (Exception e) {
				logger.error("Cannot retrieve Service Order details from catalog. " + e.getMessage());
				return;
			}
			
			logger.info("ServiceOrder id" + sor.getId() );
			logger.info("ServiceOrder Description" + sor.getDescription());
			
			for (ServiceOrderItem soi : sor.getOrderItem()) {
				logger.info("Service Item ID:" + soi.getId()  );
				logger.info("Service spec ID:" + soi.getService().getServiceSpecification().getId()   );
				logger.info("Service Name:" + soi.getService().getServiceSpecification().getName()    );
				
				for (ServiceRelationship rels : soi.getService().getServiceRelationship() ) {
					logger.info("Service rels:" + rels.getService().getName()    );
					
				}
				
			}
			
		}
	}
	
	static <T> T toJsonObj(String content, Class<T> valueType)  throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.readValue( content, valueType);
    }
	

}
