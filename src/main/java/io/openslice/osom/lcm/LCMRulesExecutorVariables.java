package io.openslice.osom.lcm;

import io.openslice.tmf.lcm.model.LCMRuleSpecification;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.ServiceCreate;
import io.openslice.tmf.so641.model.ServiceOrder;
import lombok.Data;
/**
 * @author ctranoris
 * this class is used to pass object to execution and also store 
 * their results while they are affected by code execution
 */
@Data
public class LCMRulesExecutorVariables {
	
	private ServiceCreate serviceToCreate;
	private ServiceSpecification spec;
	private ServiceOrder sorder;


	/**
	 * @param spec
	 * @param sorder
	 * @param serviceToCreate
	 */
	public LCMRulesExecutorVariables(ServiceSpecification spec, ServiceOrder sorder, ServiceCreate serviceToCreate) {
		this.serviceToCreate = serviceToCreate;
		this.spec = spec;
		this.sorder = sorder;
	}
}
