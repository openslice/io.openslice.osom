package io.openslice.osom.partnerservices;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.openslice.tmf.scm633.model.ServiceSpecification;



/**
 * @author ctranoris
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class SimpleIDSpec extends ServiceSpecification{


	@JsonProperty("id")
	protected long id;

	/**
	 * @return the id
	 */
//	@Override
//	public long getId() {
//		return id;
//	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	@JsonIgnore
	public String getIntAsString() {
		return id + "";
	}
	
	
	
}
