package io.openslice.osom.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.openslice.osom.configuration.ServiceOrderRouteBuilder;
import io.openslice.tmf.so641.model.ServiceOrder;

/**
 * @author ctranoris
 *
 */
public class ServiceOrderManager {


	private static final transient Log logger = LogFactory.getLog(ServiceOrderManager.class.getName());
	
	
	public ServiceOrder processOrder(ServiceOrder serviceOrder ) {
		
		logger.info("Received order to process serviceOrder id : " + serviceOrder.getId() );
		
		return serviceOrder;
	}
}
