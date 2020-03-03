package io.openslice.osom.partnerservices;

import java.util.List;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.openslice.tmf.pm632.model.Organization;
import io.openslice.tmf.scm633.model.ServiceSpecification;

@Component(value = "fetchUpdateExternalPartnerServices") // bean name
public class FetchUpdateExternalPartnerServices  implements JavaDelegate {


	@Autowired
	PartnerOrganizationServicesManager partnerOrganizationServicesManager;
	
	@Override
	public void execute(DelegateExecution execution) {
		if ( (execution.getVariable("partnerOrganization")!=null) && (execution.getVariable("partnerOrganization") instanceof Organization ))  {
			
			Organization org = ( Organization ) execution.getVariable( "partnerOrganization" );
			/**
			 * Fetch partner service specs. For now there is no criteria and we fetch all in one json...
			 */
			List<ServiceSpecification> specs = partnerOrganizationServicesManager.fetchServiceSpecs( org );
			
			for (ServiceSpecification serviceSpecification : specs) {
				/**
				 * add to the spec, the organization as related party
				 */
				partnerOrganizationServicesManager.updateSpecInLocalCatalog( serviceSpecification );				
			}
			
			
		}

		
	}
}
