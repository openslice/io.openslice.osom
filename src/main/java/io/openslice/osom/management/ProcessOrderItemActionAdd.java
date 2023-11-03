package io.openslice.osom.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.openslice.osom.lcm.LCMRulesController;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderItem;

@Component(value = "processOrderItemActionAdd") // bean name
public class ProcessOrderItemActionAdd implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( ProcessOrderItemActionAdd.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;


	@Autowired
	private LCMRulesController lcmRulesController;
	
	
	@Value("${spring.application.name}")
	private String compname;
	
	public void execute(DelegateExecution execution) {

		logger.info("ProcessOrderItemActionAdd:" + execution.getVariableNames().toString());

		logger.debug("Will process/orchestrate order with id = " + execution.getVariable("orderid"));
		ServiceOrder sor = serviceOrderManager.retrieveServiceOrder((String) execution.getVariable("orderid"));
		String orderItemIdToProcess = (String) execution.getVariable("orderItemId");
		ServiceOrderItem soi = null;
		
		for (ServiceOrderItem i : sor.getOrderItem()) {
			if (i.getUuid().equals( orderItemIdToProcess )){
				soi = i;
				break;
			}
		}
		
		if ( soi == null ) {
			return;
		}
		

		ServiceSpecification spec = serviceOrderManager
				.retrieveServiceSpec(soi.getService().getServiceSpecification().getId());
		if ( (spec.isIsBundle()!=null) && spec.isIsBundle() ) {
			execution.setVariable("isBundle", Boolean.TRUE ); 
		} else {
			execution.setVariable("isBundle", Boolean.FALSE ); 
		}

		execution.setVariable("serviceSpecID", spec.getId() ); 
		
			 
	}

}
