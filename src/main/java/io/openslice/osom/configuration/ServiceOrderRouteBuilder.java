package io.openslice.osom.configuration;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.tmf.so641.model.ServiceOrder;

@Configuration
//@RefreshScope
@Component
public class ServiceOrderRouteBuilder extends RouteBuilder {

	private static final transient Log logger = LogFactory.getLog(ServiceOrderRouteBuilder.class.getName());

	@Autowired
	private ConsumerTemplate consumerTemplate;

	public void configure() {

		from("jms:OSOMIN_SERVICEORDER").log(LoggingLevel.DEBUG, log, "New OSOMIN_SERVICEORDER message received")
				.end();
//		.unmarshal().json( JsonLibrary.Jackson, ServiceOrder.class, true)
//		.bean( ServiceOrderManager.class, "processOrder")
//		.to("direct:orders.newOrder");
	}

	public void processNextInvoice() {
		ServiceOrder so = consumerTemplate.receiveBody("jms:OSOMIN_SERVICEORDER", ServiceOrder.class);

		logger.info("ServiceOrder so = " + so.toString());

//	    ...
//	    producerTemplate.sendBody("netty-http:http://invoicing.com/received/" + invoice.id());
	}
}
