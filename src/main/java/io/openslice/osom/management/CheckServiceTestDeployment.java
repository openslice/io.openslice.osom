package io.openslice.osom.management;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.openslice.model.DeploymentDescriptorVxFInstanceInfo;
import io.openslice.tmf.common.model.Any;
import io.openslice.tmf.common.model.EValueType;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.common.model.service.ServiceRef;
import io.openslice.tmf.common.model.service.ServiceStateType;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristic;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristicValue;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceUpdate;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.stm653.model.CharacteristicSpecification;
import io.openslice.tmf.stm653.model.CharacteristicValueSpecification;
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
				sTCreate.characteristic( new ArrayList<>());
				

				for (Characteristic serviceChar : aService.getServiceCharacteristic() ) {
					io.openslice.tmf.stm653.model.Characteristic newChar = new io.openslice.tmf.stm653.model.Characteristic();
					newChar.setName( serviceChar.getName() );
					newChar.setValueType( serviceChar.getValueType() );
					newChar.setValue( new Any(
							serviceChar.getValue().getValue(), 
							serviceChar.getValue().getAlias()) );
					sTCreate.addCharacteristicItem( newChar );
				}
				
				copyRemainingSpecCharacteristicsToServiceCharacteristic (serviceTestSpec, sTCreate.getCharacteristic()) ;
				
				
				ServiceTest createdServiceTest = serviceOrderManager.createServiceTest(sTCreate , sorder, serviceTestSpec); 
				
				
				
				
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
	
	private void copyRemainingSpecCharacteristicsToServiceCharacteristic(ServiceTestSpecification sourceSpec, @Valid List<io.openslice.tmf.stm653.model.Characteristic> list) {
		
		
		for (CharacteristicSpecification sourceCharacteristic : sourceSpec.getSpecCharacteristic()) {
			if (  sourceCharacteristic.getValueType() != null ) {
				boolean charfound = false;
				for (io.openslice.tmf.stm653.model.Characteristic destchar : list) {
					if ( destchar.getName().equals(sourceCharacteristic.getName())) {
						charfound = true;
						break;
					}
				}
				
				if (!charfound) {
				
					io.openslice.tmf.stm653.model.Characteristic newChar = new io.openslice.tmf.stm653.model.Characteristic();
					newChar.setName( sourceCharacteristic.getName() );
					newChar.setValueType( sourceCharacteristic.getValueType() );
					
					if (  sourceCharacteristic.getValueType() != null && sourceCharacteristic.getValueType().equals( EValueType.ARRAY.getValue() ) ||
							 sourceCharacteristic.getValueType() != null && sourceCharacteristic.getValueType().equals( EValueType.SET.getValue() ) ) {
						String valString = "";
						for (CharacteristicValueSpecification specchar : sourceCharacteristic.getCharacteristicValueSpecification() ) {
							if ( ( specchar.isIsDefault()!= null) && specchar.isIsDefault() ) {
								if ( !valString.equals("")) {
									valString = valString + ",";
								}
								valString = valString + "{\"value\":\"" + specchar.getValue().getValue() + "\",\"alias\":\"" + specchar.getValue().getAlias() + "\"}";
							}
							
						}
						
						newChar.setValue( new Any( "[" + valString + "]", "") );
						
						
					} else {
						for (CharacteristicValueSpecification specchar : sourceCharacteristic.getCharacteristicValueSpecification()) {
							if ( ( specchar.isIsDefault()!= null) && specchar.isIsDefault() ) {
								newChar.setValue( new Any(
										specchar.getValue().getValue(), 
										specchar.getValue().getAlias()) );
								break;
							}else {
								if (specchar.isIsDefault()== null){

								logger.info("specchar is null value: " + sourceCharacteristic.getName() );
								}
							}

						}						
					}
					
					//sourceCharacteristic.getServiceSpecCharacteristicValue()
					
					if ( newChar.getValue() !=null) {
						list.add(newChar );
					} else {
						newChar.setValue( new Any(
								"", 
								"") );
						list.add(newChar );
					}
					
				}
				
			}
			
			
		}
		
	}
}
