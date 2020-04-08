package io.openslice.osom.partnerservices;

import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.so641.model.ServiceOrderCreate;

public class FlowOneServiceOrderCreate extends ServiceOrderCreate{


	public FlowOneServiceOrderCreate(ServiceOrderCreate servOrderCreate) {
		this.setDescription( servOrderCreate.getDescription());
		this.setCategory( servOrderCreate.getCategory());
		this.setOrderItem( servOrderCreate.getOrderItem() );
		this.getOrderItem().get(0).setUuid("1");
		this.getOrderItem().get(0).setBaseType(null);
		this.setRequestedCompletionDate(  servOrderCreate.getRequestedCompletionDate());
		this.setRequestedStartDate( servOrderCreate.getRequestedStartDate());
		this.setRelatedParty(servOrderCreate.getRelatedParty());
		this.setNote( servOrderCreate.getNote());
		this.getNote().get(0).setBaseType(null);
		this.getOrderItem().get(0).state(null);
		this.getOrderItem().get(0).getService().setBaseType(null);
		this.getOrderItem().get(0).getService().getServiceSpecification().setBaseType(null);
		for (Characteristic c : this.getOrderItem().get(0).getService().getServiceCharacteristic()) {
			c.setBaseType(null);
		}
		
	}

	
}
