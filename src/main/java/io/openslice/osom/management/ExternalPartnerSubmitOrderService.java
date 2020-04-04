package io.openslice.osom.management;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.openslice.model.DeploymentDescriptor;
import io.openslice.osom.partnerservices.PartnerOrganizationServicesManager;
import io.openslice.tmf.common.model.Any;
import io.openslice.tmf.common.model.UserPartRoleType;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.common.model.service.ServiceSpecificationRef;
import io.openslice.tmf.common.model.service.ServiceStateType;
import io.openslice.tmf.pm632.model.Organization;
import io.openslice.tmf.prm669.model.RelatedParty;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristic;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristicValue;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceUpdate;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderCreate;
import io.openslice.tmf.so641.model.ServiceOrderItem;
import io.openslice.tmf.so641.model.ServiceOrderStateType;
import io.openslice.tmf.so641.model.ServiceRestriction;

@Component(value = "externalPartnerSubmitOrderService") //bean name
public class ExternalPartnerSubmitOrderService  implements JavaDelegate {


	private static final transient Log logger = LogFactory.getLog( ExternalPartnerSubmitOrderService.class.getName());


	@Autowired
	private ServiceOrderManager serviceOrderManager;
	
	@Autowired
	private PartnerOrganizationServicesManager partnerOrganizationServicesManager;

	@Value("${THIS_PARTNER_NAME}")
	private String THIS_PARTNER_NAME = "";
	
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
					logger.info("External partner organization:" + orgz.getName() + ". Preparing Service Order." );					
					
					ServiceOrderCreate servOrder = new ServiceOrderCreate();
					servOrder.setCategory("Automated order");
					servOrder.setDescription("Automatically created by partner " + THIS_PARTNER_NAME);
					servOrder.setRequestedStartDate( sorder.getStartDate() );
					servOrder.setRequestedCompletionDate( sorder.getExpectedCompletionDate()  );					
					
					Note noteItemOrder = new Note();
					noteItemOrder.text("Automatically created by partner " + THIS_PARTNER_NAME);
					noteItemOrder.setAuthor(THIS_PARTNER_NAME);
					servOrder.addNoteItem( noteItemOrder );

					ServiceOrderItem soi = new ServiceOrderItem();
					servOrder.getOrderItem().add(soi);
					soi.setState(ServiceOrderStateType.ACKNOWLEDGED);

					ServiceRestriction serviceRestriction = new ServiceRestriction();
					ServiceSpecificationRef aServiceSpecificationRef = new ServiceSpecificationRef();
					aServiceSpecificationRef.setId( remoteServiceSpecID );
					aServiceSpecificationRef.setName( spec.getName() );
					aServiceSpecificationRef.setVersion(spec.getVersion());

					serviceRestriction.setServiceSpecification(aServiceSpecificationRef);
					
					for (Characteristic servChar : aService.getServiceCharacteristic()) {
						servChar.setUuid(null);
						serviceRestriction.addServiceCharacteristicItem(servChar);
					}
					
					soi.setService(serviceRestriction);
					
					
					ServiceOrder externalSOrder = partnerOrganizationServicesManager.makeExternalServiceOrder( servOrder, orgz, remoteServiceSpecID );
					
					if ( externalSOrder != null ) {
						execution.setVariable("externalServiceOrderId", externalSOrder.getId());

						su.setState(ServiceStateType.FEASIBILITYCHECKED );
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
