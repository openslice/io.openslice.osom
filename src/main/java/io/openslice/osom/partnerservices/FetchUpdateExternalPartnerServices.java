package io.openslice.osom.partnerservices;

import java.util.List;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.tmf.common.model.UserPartRoleType;
import io.openslice.tmf.pm632.model.Organization;
import io.openslice.tmf.prm669.model.RelatedParty;
import io.openslice.tmf.scm633.model.ServiceSpecification;

@Component(value = "fetchUpdateExternalPartnerServices") // bean name
public class FetchUpdateExternalPartnerServices  implements JavaDelegate {


	@Autowired
	PartnerOrganizationServicesManager partnerOrganizationServicesManager;
	
	@Override
	public void execute(DelegateExecution execution) {
		if ( (execution.getVariable("partnerOrganization")!=null) && (execution.getVariable("partnerOrganization") instanceof String ))  {
			
			ObjectMapper mapper = new ObjectMapper();
			
			Organization org;
			try {
				org = mapper.readValue( execution.getVariable("partnerOrganization").toString(), Organization.class);
			} catch (JsonMappingException e) {
				e.printStackTrace();
				return;
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return;
			}
			
			/**
			 * Fetch partner service specs. For now there is no criteria and we fetch all in one json...
			 */
			List<ServiceSpecification> specs = partnerOrganizationServicesManager.fetchServiceSpecs( org );
			
			for (ServiceSpecification serviceSpecification : specs) {
				/**
				 * add to the spec, the organization as related party
				 */

				serviceSpecification.getRelatedParty().clear();//clear all related parties if any
				serviceSpecification.getAttachment().clear();
				

				
				partnerOrganizationServicesManager.updateSpecInLocalCatalog(org.getId(),  serviceSpecification );				
			}
			
			
		}

		
	}
}
