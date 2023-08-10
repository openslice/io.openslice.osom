package io.openslice.osom.lcm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceCreate;
import io.openslice.tmf.sim638.model.ServiceUpdate;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderItem;
import lombok.Data;

/**
 * @author ctranoris this class is used to pass object to execution and also
 *         store their results while they are affected by code execution
 */
@Data
public class LCMRulesExecutorVariables {

	private ServiceCreate serviceToCreate;
	private ServiceUpdate serviceToUpdate;
	private ServiceSpecification spec;
	private ServiceOrder sorder;
	private ServiceOrderItem soItem;
	private Service service;
	private List<String> compileDiagnosticErrors;
	private ServiceOrderManager serviceOrderManager;
	private Map<String, String> outParams;

	/**
	 * @param spec
	 * @param sorder
	 * @param serviceToCreate
	 */
	public LCMRulesExecutorVariables(ServiceSpecification spec, 
			ServiceOrder sorder, 
			ServiceOrderItem asoItem, 
			ServiceCreate serviceToCreate,
			ServiceUpdate serviceToUpdate,
			Service serviceInstance,
			ServiceOrderManager aServiceOrderManager) {
		this.serviceToCreate = serviceToCreate;
		this.serviceToUpdate = serviceToUpdate;
		this.spec = spec;
		this.sorder = sorder;
		this.soItem = asoItem;
		this.service = serviceInstance;
		this.serviceOrderManager = aServiceOrderManager;
		this.compileDiagnosticErrors = new ArrayList<>();
		this.outParams = new HashMap<String,String>();
	}
}
