/*-
 * ========================LICENSE_START=================================
 * io.openslice.osom
 * %%
 * Copyright (C) 2019 openslice.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.openslice.osom.configuration;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.osom.serviceactions.ServiceActionCheck;
import io.openslice.tmf.am642.model.AlarmCreate;
import io.openslice.tmf.so641.model.ServiceOrder;

@Configuration
//@RefreshScope
@Component
public class OSOMRouteBuilder extends RouteBuilder {

	private static final transient Log logger = LogFactory.getLog(OSOMRouteBuilder.class.getName());

	public void configure() {
		

//		
//		prepei edw na lavoume ena action object
//		kai na to epekseragstei to ServiceActionCheck.class Prepei na gemisoume to antistoixo object

//		from("jms:queue:OSOM.NEW_SERVICEORDER_PROCESS")
//			.log(LoggingLevel.INFO, log, "New OSOM.IN.SERVICEORDER message received!")
//			.to("log:DEBUG?showBody=true&showHeaders=true")
//			.unmarshal().json( JsonLibrary.Jackson, ServiceOrder.class, true)
//			.log(LoggingLevel.INFO, log, "Order id = ${body.id} ")
//			.bean( ServiceOrderManager.class, "processOrder")
//			;
//		
//		from("jms:queue:OSOM.NEW_SERVICEORDER_PROCESS.LIST_PENDING")
//		.log(LoggingLevel.INFO, log, "New OSOM.NEW_SERVICEORDER_PROCESS.LIST_PENDING message received!")
//		.to("log:DEBUG?showBody=true&showHeaders=true")
//		.bean( ServiceOrderManager.class, "getTasks")
//		;
//		
//
//		
//		
//		from("jms:queue:OSOM.HUMAN_COMPLETE_ORDER_TASK")
//		.log(LoggingLevel.INFO, log, "New OSOM.HUMAN_COMPLETE_ORDER_TASK message received!")
//		.to("log:DEBUG?showBody=true&showHeaders=true")
//		//.unmarshal().json( JsonLibrary.Jackson, ServiceOrder.class, true)
//		//.log(LoggingLevel.INFO, log, "Order id = ${body.id}")
//		.bean( ServiceOrderManager.class, "humanComplete");
		
		
		
		//create route here to get service spec by id from model via bus
		
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
