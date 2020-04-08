package io.openslice.osom.partnerservices;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.openslice.tmf.so641.model.ServiceOrderItem;
import io.openslice.tmf.so641.model.ServiceOrderStateType;

@JsonIgnoreProperties(ignoreUnknown=true)
public class FlowOneServiceOrder  {

	@JsonProperty("id")
	private String id = null;


	@JsonProperty("state")
	private String state;


	@JsonProperty("orderItem")
	private ServiceOrderItem orderItem ;
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}


	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}


	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}


	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}


	/**
	 * @return the orderItem
	 */
	public ServiceOrderItem getOrderItem() {
		return orderItem;
	}


	/**
	 * @param orderItem the orderItem to set
	 */
	public void setOrderItem(ServiceOrderItem orderItem) {
		this.orderItem = orderItem;
	}


	
	
}
