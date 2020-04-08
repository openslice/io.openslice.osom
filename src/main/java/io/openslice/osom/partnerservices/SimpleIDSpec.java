package io.openslice.osom.partnerservices;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;



/**
 * @author ctranoris
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class SimpleIDSpec{


	@JsonProperty("id")
	protected long id;

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}
	
	
	
}
