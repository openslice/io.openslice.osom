package io.openslice.osom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.EnableRouteCoverage;
import org.apache.camel.test.spring.MockEndpoints;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.tmf.rcm634.model.ResourceSpecificationCreate;
import io.openslice.tmf.so641.model.ServiceOrder;

@RunWith(CamelSpringBootRunner.class)
//@Transactional
@SpringBootTest( webEnvironment = SpringBootTest.WebEnvironment.MOCK , classes = OsomSpringBoot.class)
//@AutoConfigureTestDatabase //this automatically uses h2
@AutoConfigureMockMvc 
@ActiveProfiles("testing")
//@TestPropertySource(
//		  locations = "classpath:application-testing.yml")

//@MockEndpoints("jms:queue:OSOMIN_SERVICEORDER")
@EnableRouteCoverage

public class ServiceOrderIT {

	private static final transient Log logger = LogFactory.getLog( ServiceOrderIT.class.getName());
	

    @Autowired
    private ProducerTemplate template;
    
    @Autowired
    private CamelContext context;

	@Autowired
	private ConsumerTemplate consumerTemplate;

//    @EndpointInject("mock:jms:queue:OSOMIN_SERVICEORDER")
//    MockEndpoint mock;

    @Test
    public void testReceive() throws Exception {
    	
    	File sspec = new File( "src/test/resources/TestExServiceOrder.json" );
		InputStream in = new FileInputStream( sspec );
		String sspectext = IOUtils.toString(in, "UTF-8");
		//ServiceOrder sor = toJsonObj( sspectext,  ServiceOrder.class);
		
    	
        //mock.expectedBodiesReceived( sor );
        template.sendBody( "jms:queue:OSOM.IN.SERVICEORDER.PROCESS", sspectext);

        template.sendBody("seda:OSOMIN_SERVICEORDERTEXT", "ZxZZZ");
        
        String s =(String) template.requestBody("activemq:OSOMIN_TEXT", "ZZZZ");
        logger.info("String s = " +s);

        //mock.assertIsSatisfied();
        logger.info("WAIT SHUTDOWN");
        context.stop();
        Thread.sleep(500); //graceful shutdown
        logger.info("EXIT TEST CASE");
    }
    
    static <T> T toJsonObj(String content, Class<T> valueType)  throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.readValue( content, valueType);
    }
}
