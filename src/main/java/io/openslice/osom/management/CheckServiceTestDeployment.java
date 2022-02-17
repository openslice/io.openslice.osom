package io.openslice.osom.management;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.openslice.model.DeploymentDescriptorVxFInstanceInfo;
import io.openslice.tmf.common.model.Any;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.common.model.service.ServiceRef;
import io.openslice.tmf.common.model.service.ServiceStateType;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristic;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceUpdate;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.stm653.model.ServiceTest;
import io.openslice.tmf.stm653.model.ServiceTestCreate;
import io.openslice.tmf.stm653.model.ServiceTestSpecification;
import io.openslice.tmf.stm653.model.ServiceTestSpecificationRef;
import io.openslice.tmf.stm653.model.ServiceTestUpdate;

@Component(value = "checkServiceTestDeployment") //bean name
public class CheckServiceTestDeployment  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( CheckServiceTestDeployment.class.getName());

	@Value("${spring.application.name}")
	private String compname;
	@Autowired
	private ServiceOrderManager serviceOrderManager;

	@Override
	public void execute(DelegateExecution execution) {
		logger.info( "checkServiceTestDeployment" );
		logger.debug( execution.getVariableNames().toString() );

		execution.setVariable("serviceTestDeploymentFinished",   false );
		Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("contextServiceId") );
		logger.debug("Check checkServiceTestDeployment Service name:" + aService.getName() );
		logger.debug("Request checkServiceTestDeployment for Service id: " + aService.getId() );
		
		ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aService.getServiceSpecificationRef().getId() );

		ServiceSpecCharacteristic charc = spec.findSpecCharacteristicByName( "testSpecRef" );

		ServiceUpdate supd = new ServiceUpdate();
		supd.setState( ServiceStateType.TERMINATED); //by default if something goes wrong
		
		if ( charc != null ) {
			String sTestId = charc.getDefaultValue();
			logger.debug("checkServiceTestDeployment will create Service Test Spec with id: " + sTestId );
			ServiceTestSpecification serviceTestSpec = serviceOrderManager.retrieveServiceTestSpec(sTestId);
			if ( serviceTestSpec != null ) {
				
				//1. create test instance
				ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder( execution.getVariable("orderid").toString() );
				ServiceTestCreate sTCreate = new ServiceTestCreate();
				String servicename = spec.getName();
				sTCreate.setDescription("A Service Test for " + servicename );
				
				sTCreate.setName( servicename );
				sTCreate.setState( ServiceStateType.ACTIVE.name());				
				ServiceTestSpecificationRef testRef = new ServiceTestSpecificationRef();
				testRef.setId(sTestId);
				sTCreate.setTestSpecification(testRef );
				ServiceRef serviceRef = new ServiceRef();
				serviceRef.setId( aService.getId());
				sTCreate.setRelatedService(serviceRef );		
				
				ServiceTest createdServiceTest = serviceOrderManager.createServiceTest(sTCreate , sorder, serviceTestSpec); 
				
				
				//update serviceTest with service characteristics!
				ServiceTestUpdate stupd = new ServiceTestUpdate();
				for (io.openslice.tmf.stm653.model.Characteristic c : createdServiceTest.getCharacteristic()) {						
					stupd.addCharacteristicItem( c );		
					String newvalue = aService.getServiceCharacteristicByName( c.getName() ).getValue().getValue();
					c.setValue( new Any(newvalue) ) ; 
				}							
				serviceOrderManager.updateServiceTest( createdServiceTest.getId() , stupd);
				
				
				//update parent service
				if ( createdServiceTest!=null) {
					//2. reference testintance in supd
					for (Characteristic c : aService.getServiceCharacteristic()) {						
						supd.addServiceCharacteristicItem( c );					
					}	
					Characteristic serviceCharacteristicItem = new Characteristic();
					serviceCharacteristicItem.setName( "testInstanceRef" );

					String serviceTestInstanceID= createdServiceTest.getId() ;
					serviceCharacteristicItem.setValue( new Any( serviceTestInstanceID  ));
					supd.addServiceCharacteristicItem(serviceCharacteristicItem);	
					supd.setState( ServiceStateType.ACTIVE);				
				}
	
			}
		}
			
		

		serviceOrderManager.updateService( aService.getId() , supd, false);

		execution.setVariable("serviceTestDeploymentFinished",   true );
			
	}
	
}
