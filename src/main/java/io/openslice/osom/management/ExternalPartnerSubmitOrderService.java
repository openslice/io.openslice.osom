package io.openslice.osom.management;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.openslice.model.DeploymentDescriptor;
import io.openslice.osom.partnerservices.PartnerOrganizationServicesManager;
import io.openslice.tmf.common.model.Any;
import io.openslice.tmf.common.model.UserPartRoleType;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.common.model.service.ServiceStateType;
import io.openslice.tmf.pm632.model.Organization;
import io.openslice.tmf.prm669.model.RelatedParty;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristic;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristicValue;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceUpdate;
import io.openslice.tmf.so641.model.ServiceOrder;

@Component(value = "externalPartnerSubmitOrderService") //bean name
public class ExternalPartnerSubmitOrderService  implements JavaDelegate {


	private static final transient Log logger = LogFactory.getLog( ExternalPartnerSubmitOrderService.class.getName());


	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	@Autowired
	private PartnerOrganizationServicesManager partnerOrganizationServicesManager;
	
	@Override
	public void execute(DelegateExecution execution) {

		logger.info( "ExternalPartnerSubmitOrderService" );
		logger.info( "VariableNames:" + execution.getVariableNames().toString() );
		logger.info("orderid:" + execution.getVariable("orderid").toString() );
		logger.info("serviceId:" + execution.getVariable("serviceId").toString() );
				

		ServiceUpdate su = new ServiceUpdate();//the object to update the service
		if (execution.getVariable("serviceId") instanceof String) {

			ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder( execution.getVariable("orderid").toString() );
			Service aService = serviceOrderManager.retrieveService( (String) execution.getVariable("serviceId") );
			logger.info("Service name:" + aService.getName() );
			logger.info("Service state:" + aService.getState()  );			
			logger.info("Request to External Service Partner for Service: " + aService.getId() );

			ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aService.getServiceSpecificationRef().getId() );
			
			if ( spec!=null ) {
				logger.info("Service spec:" + spec.getName()  );						
				RelatedParty rpOrg = null;
				if ( spec.getRelatedParty() != null ) {
					for (RelatedParty rp : spec.getRelatedParty()) {
						if ( rp.getRole().equals( UserPartRoleType.ORGANIZATION.getValue() )) {
							rpOrg =rp;
							break;
						}				
					}			
				}
				
				String remoteServiceSpecID = rpOrg.getExtendedInfo();				
				Organization orgz = serviceOrderManager.getExternalPartnerOrganization( rpOrg.getId() );
				
				if ( orgz!=null ) {
					logger.info("External partner organization:" + orgz.getName()  );					
					
					ServiceOrder externalSOrder = partnerOrganizationServicesManager.makeExternalServiceOrder( orgz, remoteServiceSpecID );
					
					if ( externalSOrder != null ) {
						execution.setVariable("externalServiceOrderId", externalSOrder.getId());

						su.setState(ServiceStateType.RESERVED );
						Note noteItem = new Note();
						noteItem.setText( "Request to partner " + orgz.getName() + " for spec:" + spec.getName()  + " done!  ServiceOrder id: " + externalSOrder.getId());
						noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
						noteItem.setAuthor("OSOM");
						su.addNoteItem( noteItem );
						Characteristic serviceCharacteristicItem = new Characteristic();
						serviceCharacteristicItem.setName( "externalServiceOrderId" );
						serviceCharacteristicItem.setValue( new Any( externalSOrder.getId() + "" ));
						su.addServiceCharacteristicItem(serviceCharacteristicItem);
						
						Service supd = serviceOrderManager.updateService(  execution.getVariable("serviceId").toString(), su);
						logger.info("Request to partner " + orgz.getName() + " for spec:" + spec.getName()  + " done! Service: " + supd.getId() );						
						return;						
					}
				}
				
				
			} else {
				logger.error( "Cannot retrieve ServiceSpecification for service :" + (String) execution.getVariable("serviceId") );
			}
		} else {
			logger.error( "Cannot retrieve variable serviceId"  );
		}

		//if we get here somethign is wrong so we need to terminate the service.
		Note noteItem = new Note();
		noteItem.setText("Order Request Service to External Partner FAILED");
		noteItem.setAuthor("OSOM");
		noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
		su.addNoteItem( noteItem );
		su.setState(ServiceStateType.TERMINATED   );
		serviceOrderManager.updateService(  execution.getVariable("serviceId").toString(), su);
		
	}

	


	
}
