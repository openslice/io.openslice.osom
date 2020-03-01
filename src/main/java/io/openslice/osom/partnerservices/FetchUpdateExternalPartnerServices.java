package io.openslice.osom.partnerservices;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component(value = "fetchUpdateExternalPartnerServices") // bean name
public class FetchUpdateExternalPartnerServices  implements JavaDelegate {

	
	@Override
	public void execute(DelegateExecution execution) {
		if (execution.getVariable("partnerOrganization")!=null) {
			
			//fetch via webclient
			
			//update services to catalog
			
			
		}

		
	}
}
