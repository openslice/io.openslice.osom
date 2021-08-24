package io.openslice.osom.lcm;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.openslice.tmf.common.model.EValueType;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristic;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.so641.model.ServiceOrder;

/**
 * @author ctranoris
 * 
 *
 */
public abstract class LcmBaseExecutor {

	private LCMRulesExecutorVariables vars;

	private static final transient Log logger = LogFactory.getLog(LcmBaseExecutor.class.getName());
	
	

	public LCMRulesExecutorVariables run(LCMRulesExecutorVariables variables) {
		this.vars= variables;
		try {
			this.exec();			
		}catch (Exception e) {
			vars.getCompileDiagnosticErrors().add( e.getLocalizedMessage());
			e.printStackTrace();
		}
		return this.vars;
	}

	
	/**
	 * this is overriden
	 */
	public abstract void exec() ;

	
	
	/**
	 * @return the vars
	 */
	public LCMRulesExecutorVariables getVars() {
		return vars;
	}


	/**
	 * @param vars the vars to set
	 */
	public void setVars(LCMRulesExecutorVariables vars) {
		this.vars = vars;
	}

	


	public String getCharValFromStringType(String charName) {
		logger.debug("getCharValFromStringType " + charName);
		Optional<Characteristic> c = getCharacteristicByName(charName, this.vars.getServiceToCreate().getServiceCharacteristic() );		
		
		if ( c.isPresent()) {
			logger.debug("getCharValFromStringType " + c.get().getValue().getValue());
			return c.get().getValue().getValue();
		}

		logger.debug("getCharValFromStringType NULL ");
		return null;	
		
	}

	private Optional<Characteristic>  getCharacteristicByName(String charName, @Valid List<Characteristic> serviceCharacteristic) {
		logger.debug("getCharacteristicByName " + charName);
		if (serviceCharacteristic!=null) {
			for (Characteristic c : serviceCharacteristic ) {
				if ( c.getName().equals(charName) ) {
					if ( c.getValue() !=null ) {
						if ( c.getValue().getValue() !=null ) {
							return Optional.of(c);
						}
					}
				}
			}			
		}
		Characteristic z = null;
		return Optional.ofNullable(z);
	}


	public void setCharValFromStringType(String charName, String newValue) {
		logger.debug("setCharValFromStringType " + charName +" = " + newValue);
		Optional<Characteristic> c = getCharacteristicByName(charName, this.vars.getServiceToCreate().getServiceCharacteristic() );
		c.ifPresent( val -> val.getValue().setValue( newValue ) );
		
	}


	public void setCharValNumber(String charName, int newValue) {

		logger.debug("setCharValNumber " + charName +" = " + newValue);
		Optional<Characteristic> c = getCharacteristicByName(charName, this.vars.getServiceToCreate().getServiceCharacteristic() );
		c.ifPresent( val -> val.getValue().setValue( "" + newValue) );
		
	}

	public int getCharValNumber(String charName) {
		logger.debug("getCharValNumber " + charName);
		Optional<Characteristic> c = getCharacteristicByName(charName, this.vars.getServiceToCreate().getServiceCharacteristic() );		
		
		if ( c.isPresent()) {
			logger.debug("getCharValNumber " + c.get().getValue().getValue());
			if ( c.get().getValueType().equals( EValueType.BINARY .getValue() ) ) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;				
			} else if ( c.get().getValueType().equals( EValueType.ENUM .getValue() ) ) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;				
			}  else if ( c.get().getValueType().equals( EValueType.INTEGER .getValue() ) ) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;				
			}   else if ( c.get().getValueType().equals( EValueType.lONGINT .getValue() ) ) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;				
			}    else if ( c.get().getValueType().equals( EValueType.SMALLINT .getValue() ) ) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;				
			} 
		}

		logger.debug("getCharValNumber NULL ");
		return -1;	
	}
	
	
	public String getCharValAsString(String charName) {
		logger.debug("getCharValAsString " + charName);
		Optional<Characteristic> c = getCharacteristicByName(charName, this.vars.getServiceToCreate().getServiceCharacteristic() );		
		
		if ( c.isPresent()) {
			logger.debug("getCharValAsString " + c.get().getValue().getValue());
			return c.get().getValue().getValue();
		}

		logger.debug("getCharValAsString NULL ");
		return null;	
		
	}
}
