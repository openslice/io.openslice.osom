package io.openslice.osom;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import io.openslice.tmf.so641.model.ServiceOrder;

@RunWith(CamelSpringBootRunner.class)
@Transactional
@SpringBootTest( webEnvironment = SpringBootTest.WebEnvironment.MOCK , classes = OsomSpringBoot.class)
//@AutoConfigureTestDatabase //this automatically uses h2
@AutoConfigureMockMvc 
@ActiveProfiles("testing")
//@TestPropertySource(
//		  locations = "classpath:application-testing.yml")

//@MockEndpoints("jms:queue:OSOMIN_SERVICEORDER")
public class ServiceOrderIT {

	private static final transient Log logger = LogFactory.getLog( ServiceOrderIT.class.getName());
	

    @Autowired
    private ProducerTemplate template;

//    @EndpointInject("mock:jms:queue:OSOMIN_SERVICEORDER")
//    MockEndpoint mock;

    @Test
    public void testReceive() throws Exception {
    	
    	ServiceOrder sor = new ServiceOrder();
    	sor.setUuid("UUID");
    	
        //mock.expectedBodiesReceived( sor );
        template.sendBody("jms:OSOMIN_SERVICEORDER", sor);
        //mock.assertIsSatisfied();
    }
}
