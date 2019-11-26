package io.openslice.osom.configuration;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.tmf.so641.model.ServiceOrder;

@Configuration
//@RefreshScope
@Component
public class OSOMRouteBuilder extends RouteBuilder {

	private static final transient Log logger = LogFactory.getLog(OSOMRouteBuilder.class.getName());

	public void configure() {

		from("jms:queue:OSOM.NEW_SERVICEORDER_PROCESS")
			.log(LoggingLevel.INFO, log, "New OSOM.IN.SERVICEORDER message received!")
			.to("log:DEBUG?showBody=true&showHeaders=true")
			.unmarshal().json( JsonLibrary.Jackson, ServiceOrder.class, true)
			.log(LoggingLevel.INFO, log, "Order id = ${body.id} ")
			.bean( ServiceOrderManager.class, "processOrder")
			;
		
		from("jms:queue:OSOM.NEW_SERVICEORDER_PROCESS.LIST_PENDING")
		.log(LoggingLevel.INFO, log, "New OSOM.NEW_SERVICEORDER_PROCESS.LIST_PENDING message received!")
		.to("log:DEBUG?showBody=true&showHeaders=true")
		.bean( ServiceOrderManager.class, "getTasks")
		;
		
		from("jms:queue:OSOM.ACK_SERVICEORDER_PROCESS")
		.log(LoggingLevel.INFO, log, "New OSOM.ACK_SERVICEORDER_PROCESS message received!")
		.to("log:DEBUG?showBody=true&showHeaders=true")
		.unmarshal().json( JsonLibrary.Jackson, ServiceOrder.class, true)
		.log(LoggingLevel.INFO, log, "Order id = ${body.id}")
		.bean( ServiceOrderManager.class, "submitReview")
		;
		
		
		from("jms:queue:OSOM.HUMAN_COMPLETE_ORDER_TASK")
		.log(LoggingLevel.INFO, log, "New OSOM.HUMAN_COMPLETE_ORDER_TASK message received!")
		.to("log:DEBUG?showBody=true&showHeaders=true")
		//.unmarshal().json( JsonLibrary.Jackson, ServiceOrder.class, true)
		//.log(LoggingLevel.INFO, log, "Order id = ${body.id}")
		.bean( ServiceOrderManager.class, "humanComplete")
		;
		
//		
//		from("activemq:OSOMIN_TEXT")
//		.log(LoggingLevel.INFO, log, "New activemq:OSOMIN_TEXT message received")
//		.setBody(constant("46"))
//		.to("stream:out")
//		.end();
//		
//		from("seda:OSOMIN_SERVICEORDERTEXT").log(LoggingLevel.INFO, log, "New seda:OSOMIN_SERVICEORDERTEXT message received")
//		.to("stream:out");;
//		
	}

}
