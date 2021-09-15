package io.openslice.osom.lcm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.osom.partnerservices.GenericClient;
import io.openslice.tmf.common.model.Any;
import io.openslice.tmf.common.model.EValueType;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.lcm.model.LCMRuleSpecification;
import io.openslice.tmf.prm669.model.RelatedParty;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderCreate;
import io.openslice.tmf.so641.model.ServiceOrderItemRelationship;
import io.openslice.tmf.so641.model.ServiceOrderStateType;
import reactor.core.publisher.Mono;

/**
 * @author ctranoris
 * 
 *
 */
public abstract class LcmBaseExecutor {

	private LCMRulesExecutorVariables vars;

	private LCMRuleSpecification lcmspec;

	private static final transient Log logger = LogFactory.getLog(LcmBaseExecutor.class.getName());

	public LCMRulesExecutorVariables run(LCMRulesExecutorVariables variables, LCMRuleSpecification lcmspec) {
		this.vars = variables;
		this.lcmspec = lcmspec;
		try {
			this.exec();
		} catch (Exception e) {
			vars.getCompileDiagnosticErrors().add(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return this.vars;
	}

	/**
	 * this is overriden
	 */
	public abstract void exec();

	private void testF() {

	}

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

	/**
	 * @return the lcmspec
	 */
	public LCMRuleSpecification getLcmspec() {
		return lcmspec;
	}

	/**
	 * @param lcmspec the lcmspec to set
	 */
	public void setLcmspec(LCMRuleSpecification lcmspec) {
		this.lcmspec = lcmspec;
	}

	public String getCharValFromStringType(String charName) {
		logger.debug("getCharValFromStringType " + charName);
		Optional<Characteristic> c = getCharacteristicByName(charName);

		if (c.isPresent()) {
			logger.debug("getCharValFromStringType " + c.get().getValue().getValue());
			return c.get().getValue().getValue();
		}

		logger.debug("getCharValFromStringType NULL ");
		return null;

	}

	private Optional<Characteristic> getCharacteristicByName(String charName) {

		List<Characteristic> serviceCharacteristic;
		if (lcmspec.getLcmrulephase().equals("PRE_PROVISION") || this.vars.getService() == null) {
			serviceCharacteristic = this.vars.getServiceToCreate().getServiceCharacteristic();
		} else { // use as input the running service in all other phases!
			serviceCharacteristic = new ArrayList<>(this.vars.getService().getServiceCharacteristic());
		}

		logger.debug("getCharacteristicByName " + charName);
		if (serviceCharacteristic != null) {
			for (Characteristic c : serviceCharacteristic) {
				if (c.getName().equals(charName)) {
					if (c.getValue() != null) {
						if (c.getValue().getValue() != null) {
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
		logger.debug("setCharValFromStringType " + charName + " = " + newValue);
		Optional<Characteristic> c = getCharacteristicByName(charName);
		c.ifPresent(val -> val.getValue().setValue(newValue));

		copyCharacteristicToServiceToUpdate(c.get());
		
	}

	private void copyCharacteristicToServiceToUpdate(Characteristic characteristic) {

		if ( this.vars.getServiceToUpdate() != null ) {
			this.vars.getServiceToUpdate().addServiceCharacteristicItem( characteristic );			
		}
		
	}

	public void setCharValNumber(String charName, int newValue) {

		logger.debug("setCharValNumber " + charName + " = " + newValue);
		Optional<Characteristic> c = getCharacteristicByName(charName);
		c.ifPresent(val -> val.getValue().setValue("" + newValue));

		copyCharacteristicToServiceToUpdate(c.get());
	}

	public int getCharValNumber(String charName) {
		logger.debug("getCharValNumber " + charName);
		Optional<Characteristic> c = getCharacteristicByName(charName);

		if (c.isPresent()) {
			logger.debug("getCharValNumber " + c.get().getValue().getValue());
			if (c.get().getValueType().equals(EValueType.BINARY.getValue())) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;
			} else if (c.get().getValueType().equals(EValueType.ENUM.getValue())) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;
			} else if (c.get().getValueType().equals(EValueType.INTEGER.getValue())) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;
			} else if (c.get().getValueType().equals(EValueType.lONGINT.getValue())) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;
			} else if (c.get().getValueType().equals(EValueType.SMALLINT.getValue())) {
				int i = Integer.parseInt(c.get().getValue().getValue());
				return i;
			}
		}

		logger.debug("getCharValNumber NULL ");
		return -1;
	}

	public String getCharValAsString(String charName) {
		logger.debug("getCharValAsString " + charName);
		Optional<Characteristic> c = getCharacteristicByName(charName);

		if (c.isPresent()) {
			logger.debug("getCharValAsString " + c.get().getValue().getValue());
			return c.get().getValue().getValue();
		}

		logger.debug("getCharValAsString NULL ");
		return null;

	}

	public Boolean getCharValFromBooleanType(String charName) {
		logger.debug("getCharValFromBooleanType " + charName);
		Optional<Characteristic> c = getCharacteristicByName(charName);

		if (c.isPresent() && c.get().getValue() != null) {
			logger.debug("getCharValFromBooleanType " + c.get().getValue().getValue());

			return c.get().getValue().getValue().toUpperCase().equals("TRUE");
		}

		logger.debug("getCharValFromBooleanType NULL ");
		return false;

	}

	public void setCharValFromBooleanType(String charName, boolean newValue) {

		logger.debug("setCharValFromBooleanType " + charName + " = " + newValue);
		Optional<Characteristic> c = getCharacteristicByName(charName);
		c.ifPresent(val -> val.getValue().setValue("" + newValue));

		copyCharacteristicToServiceToUpdate(c.get());
	}

	/**
	 * 
	 * example setCharValNumber("Director return channel to cameras", 1);
	 * setCharValFromSetType("cirros_2vnf_nsd::Primitive::fsetup",
	 * "[{\"value\":\"1\",\"alias\":\"member_vnf_index\"},{\"value\":\"fsetup\",\"alias\":\"primitive\"},{\"value\":\"{
	 * \\\"tvg\\\": { \\\"ip\\\": \\\"\\\", \\\"channel1\\\": { \\\"mode\\\":
	 * \\\"0\\\" } } }\",\"alias\":\"confjson\"}]");
	 * 
	 * @param charName
	 * @param newValue
	 */
	public void setCharValFromSetType(String charName, String newValue) {

		logger.debug("setCharValFromBooleanType " + charName + " = " + newValue);
		Optional<Characteristic> c = getCharacteristicByName(charName);
		c.ifPresent(val -> val.getValue().setValue("" + newValue));

		copyCharacteristicToServiceToUpdate(c.get());
	}

	public List<String> getCharValFromSetType(String charName) {
		logger.debug("getCharValFromSetType " + charName);
		Optional<Characteristic> c = getCharacteristicByName(charName);

//		if (lcmspec.getLcmrulephase().equals("PRE_PROVISION") || this.vars.getService() == null) {
//			serviceCharacteristic = this.vars.getServiceToCreate().getServiceCharacteristic();
//		} else { // use as input the running service in all other phases!
//			serviceCharacteristic = new ArrayList<>(this.vars.getService().getServiceCharacteristic());
//		}

		if (c.isPresent() && c.get().getValue() != null) {
			logger.debug("getCharValFromSetType " + c.get().getValue().getValue());

			String val = c.get().getValue().getValue();

			List<Any> as = null;
			try {
				as = toJsonObj(val, new TypeReference<List<Any>>() {
				});
				logger.debug("getCharValFromSetType " + as.toString());

				ArrayList<String> asret = new ArrayList<>();
				for (Any any : as) {
					asret.add(any.getValue());
				}
				return asret;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		logger.debug("getCharValFromSetType NULL ");
		return null;
	}

	public boolean checkIfSetContainsValue(List<String> charValFromSetType, String value) {

		if (charValFromSetType != null) {
			for (String s : charValFromSetType) {
				if (s.equals(value))
					return true;
			}
		}

		return false;
	}

	public void logtext(String txt) {
		logger.info("From LCM Rule Log: " + txt);
	}

	public WebClient getAwebClient(String baseurl, String clientRegId, String aOAUTH2CLIENTID, String aOAUTHSECRET,
			String scopes, String aTOKEURI, String aUSERNAME, String aPASSWORD) {
		logger.info(baseurl);

		String[] aOAUTHscopes = null;
		if (scopes != null) {
			aOAUTHscopes = scopes.split(";");
		}

		GenericClient oac = new GenericClient(

				clientRegId, aOAUTH2CLIENTID, aOAUTHSECRET, aOAUTHscopes, aTOKEURI, aUSERNAME, aPASSWORD, baseurl);

		try {
			WebClient webClient;
			webClient = oac.createWebClient();
			return webClient;
		} catch (SSLException e) {
			logger.error(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return null;
	}

	public String rest_block(String verb, String eurl, String headers, String apayload) {
		return rest_block(verb, eurl, headers, apayload, null, null, null, null, null, null, null);

	}

	public String rest_block(String verb, String eurl, String headers, String apayload, String baseurl,
			String aOAUTH2CLIENTID, String aOAUTHSECRET, String scopes, String aTOKEURI, String aUSERNAME,
			String aPASSWORD) {

		logger.debug(String.format(
				"verb: %s\n eurl: %s\n headers: %s\n apayload: %s\n baseurl: %s\n aOAUTH2CLIENTID: %s\n aOAUTHSECRET: %s\n scopes: %s\n aTOKEURI: %s\n aUSERNAME: %s\n ",
				verb, eurl, headers, apayload, baseurl, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI, aUSERNAME,
				aPASSWORD));

		if (baseurl != null) {
			eurl = eurl.replace(baseurl, ""); // remove the baseurl if present
		}

		Consumer<HttpHeaders> httpHeaders = (t) -> {
			if (headers != null) {
				String[] hs = headers.split(";");
				for (String headervals : hs) {
					String[] ah = headervals.split("=");
					t.add(ah[0], ah[1]);
				}
			}

		};

		if (verb.equals("GET")) {
			return rest_block_GET(eurl, httpHeaders, apayload, baseurl, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
					aUSERNAME, aPASSWORD);
		} else if (verb.equals("POST")) {
			return rest_block_POST(eurl, httpHeaders, apayload, baseurl, aOAUTH2CLIENTID, aOAUTHSECRET, scopes,
					aTOKEURI, aUSERNAME, aPASSWORD);
		} else if (verb.equals("PUT")) {
			return rest_block_PUT(eurl, httpHeaders, apayload, baseurl, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
					aUSERNAME, aPASSWORD);
		} else if (verb.equals("PATCH")) {
			return rest_block_PATCH(eurl, httpHeaders, apayload, baseurl, aOAUTH2CLIENTID, aOAUTHSECRET, scopes,
					aTOKEURI, aUSERNAME, aPASSWORD);
		} else if (verb.equals("DELETE")) {
			return rest_block_DELETE(eurl, httpHeaders, apayload, baseurl, aOAUTH2CLIENTID, aOAUTHSECRET, scopes,
					aTOKEURI, aUSERNAME, aPASSWORD);
		}

		return null;
	}

	public String rest_block_GET(String eurl, Consumer<HttpHeaders> httpHeaders, String apayload, String baseurl,
			String aOAUTH2CLIENTID, String aOAUTHSECRET, String scopes, String aTOKEURI, String aUSERNAME,
			String aPASSWORD) {

		String clientRegId = "lcmBaseExecutor_WebClient";
		WebClient webclient = getAwebClient(baseurl, clientRegId, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
				aUSERNAME, aPASSWORD);
		String aresponse = null;
		if (webclient != null) {

			try {
				aresponse = webclient.get().uri(eurl).headers(httpHeaders).retrieve()
						.onStatus(HttpStatus::is4xxClientError, response -> {
							logger.error("4xx eror");
							return Mono.error(new RuntimeException("4xx"));
						}).onStatus(HttpStatus::is5xxServerError, response -> {
							logger.error("5xx eror");
							return Mono.error(new RuntimeException("5xx"));
						}).bodyToMono(new ParameterizedTypeReference<String>() {
						}).block();

			} catch (Exception e) {
				logger.error(" error on web client request for " + eurl);
				e.printStackTrace();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

		} else {
			logger.error("WebClient is null. Cannot be created for " + eurl);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return aresponse;
	}

	public String rest_block_POST(String eurl, Consumer<HttpHeaders> httpHeaders, String apayload, String baseurl,
			String aOAUTH2CLIENTID, String aOAUTHSECRET, String scopes, String aTOKEURI, String aUSERNAME,
			String aPASSWORD) {
		String clientRegId = "lcmBaseExecutor_WebClient";
		WebClient webclient = getAwebClient(baseurl, clientRegId, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
				aUSERNAME, aPASSWORD);
		String aresponse = null;
		if (webclient != null) {

			try {
				aresponse = webclient.post().uri(eurl).headers(httpHeaders).bodyValue(apayload).retrieve()
						.onStatus(HttpStatus::is4xxClientError, response -> {
							logger.error("4xx eror");
							return Mono.error(new RuntimeException("4xx"));
						}).onStatus(HttpStatus::is5xxServerError, response -> {
							logger.error("5xx eror");
							return Mono.error(new RuntimeException("5xx"));
						}).bodyToMono(new ParameterizedTypeReference<String>() {
						}).block();

			} catch (Exception e) {
				logger.error(" error on web client request for " + eurl);
				e.printStackTrace();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

		} else {
			logger.error("WebClient is null. Cannot be created for " + eurl);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return aresponse;
	}

	public String rest_block_PUT(String eurl, Consumer<HttpHeaders> httpHeaders, String apayload, String baseurl,
			String aOAUTH2CLIENTID, String aOAUTHSECRET, String scopes, String aTOKEURI, String aUSERNAME,
			String aPASSWORD) {
		String clientRegId = "lcmBaseExecutor_WebClient";
		WebClient webclient = getAwebClient(baseurl, clientRegId, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
				aUSERNAME, aPASSWORD);
		String aresponse = null;
		if (webclient != null) {

			try {
				aresponse = webclient.put().uri(eurl).headers(httpHeaders).bodyValue(apayload).retrieve()
						.onStatus(HttpStatus::is4xxClientError, response -> {
							logger.error("4xx eror");
							return Mono.error(new RuntimeException("4xx"));
						}).onStatus(HttpStatus::is5xxServerError, response -> {
							logger.error("5xx eror");
							return Mono.error(new RuntimeException("5xx"));
						}).bodyToMono(new ParameterizedTypeReference<String>() {
						}).block();

			} catch (Exception e) {
				logger.error(" error on web client request for " + eurl);
				e.printStackTrace();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

		} else {
			logger.error("WebClient is null. Cannot be created for " + eurl);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return aresponse;
	}

	public String rest_block_PATCH(String eurl, Consumer<HttpHeaders> httpHeaders, String apayload, String baseurl,
			String aOAUTH2CLIENTID, String aOAUTHSECRET, String scopes, String aTOKEURI, String aUSERNAME,
			String aPASSWORD) {
		String clientRegId = "lcmBaseExecutor_WebClient";
		WebClient webclient = getAwebClient(baseurl, clientRegId, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
				aUSERNAME, aPASSWORD);
		String aresponse = null;
		if (webclient != null) {

			try {
				aresponse = webclient.patch().uri(eurl).headers(httpHeaders).bodyValue(apayload).retrieve()
						.onStatus(HttpStatus::is4xxClientError, response -> {
							logger.error("4xx eror");
							return Mono.error(new RuntimeException("4xx"));
						}).onStatus(HttpStatus::is5xxServerError, response -> {
							logger.error("5xx eror");
							return Mono.error(new RuntimeException("5xx"));
						}).bodyToMono(new ParameterizedTypeReference<String>() {
						}).block();

			} catch (Exception e) {
				logger.error(" error on web client request for " + eurl);
				e.printStackTrace();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

		} else {
			logger.error("WebClient is null. Cannot be created for " + eurl);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return aresponse;
	}

	public String rest_block_DELETE(String eurl, Consumer<HttpHeaders> httpHeaders, String apayload, String baseurl,
			String aOAUTH2CLIENTID, String aOAUTHSECRET, String scopes, String aTOKEURI, String aUSERNAME,
			String aPASSWORD) {
		String clientRegId = "lcmBaseExecutor_WebClient";
		WebClient webclient = getAwebClient(baseurl, clientRegId, aOAUTH2CLIENTID, aOAUTHSECRET, scopes, aTOKEURI,
				aUSERNAME, aPASSWORD);
		String aresponse = null;
		if (webclient != null) {

			try {
				aresponse = webclient.delete().uri(eurl).headers(httpHeaders).retrieve()
						.onStatus(HttpStatus::is4xxClientError, response -> {
							logger.error("4xx eror");
							return Mono.error(new RuntimeException("4xx"));
						}).onStatus(HttpStatus::is5xxServerError, response -> {
							logger.error("5xx eror");
							return Mono.error(new RuntimeException("5xx"));
						}).bodyToMono(new ParameterizedTypeReference<String>() {
						}).block();

			} catch (Exception e) {
				logger.error(" error on web client request for " + eurl);
				e.printStackTrace();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

		} else {
			logger.error("WebClient is null. Cannot be created for " + eurl);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return aresponse;
	}

	public String getCurrentServiceOrderPropValue(String propertyName, String... props) {

		logger.debug("getCurrentServiceOrderPropValue propertyName=" + propertyName);
		ServiceOrder serviceOrder = this.vars.getSorder();
		if (serviceOrder == null) {
			return null;
		}

		switch (propertyName) {
		case "state":
			return serviceOrder.getState().name();
		case "id":
			return serviceOrder.getId();
		case "externaId":
			return serviceOrder.getExternalId();
		case "serviceOrderObjectasJSON":
			try {
				return toJsonString(serviceOrder);
			} catch (IOException e) {
				logger.error(e.getLocalizedMessage());
				e.printStackTrace();
			}
		default:
			break;
		}

		return "";
	}

	public String getCurrentServicePropValue(String propertyName, String... props) {

		logger.debug("getCurrentServicePropValue propertyName=" + propertyName);
		Service service = this.vars.getService();
		if (service == null) {
			return null;
		}

		return getServicePropValue(service, propertyName, props);
	}

	public String getFromPayloadServicePropValue(String jsonpayload, String propertyName, String... props) {

		logger.debug("getFromPayloadServicePropValue propertyName=" + propertyName);

		Service service = null;
		try {
			service = toJsonObj(jsonpayload, new TypeReference<Service>() {
			});
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		if (service == null) {
			return null;
		}

		return getServicePropValue(service, propertyName, props);
	}

	private String getServicePropValue(Service service, String propertyName, String... props) {
		switch (propertyName) {
		case "state":
			return service.getState().name();
		case "name":
			return service.getName();
		case "hasStarted":
			return service.isHasStarted().toString();
		case "isServiceEnabled":
			return service.isIsServiceEnabled().toString();
		case "serviceType":
			return service.getServiceType();
		case "startMode":
			return service.getStartMode();
		case "serviceCharacteristicValue":
			if (props != null && props.length > 0) {
				Characteristic cp = service.getServiceCharacteristicByName(props[0]);
				logger.debug("getFromPayloadServicePropValue propertyName=" + propertyName + ',' + props[0]);
				if (cp != null && cp.getValue() != null) {
					return cp.getValue().getValue();
				}
			}
			break;
		case "serviceOrderID":
			if (service.getServiceOrder() != null) {
				return service.getServiceOrder().stream().findFirst().get().getId();
			}
			break;
		case "serviceSpecificationID":
			if (service.getServiceSpecificationRef() != null) {
				return service.getServiceSpecificationRef().getId();
			}
			break;
		case "serviceObjectasJSON":
			try {
				return toJsonString(service);
			} catch (IOException e) {
				logger.error(e.getLocalizedMessage());
				e.printStackTrace();
			}
		default:
			break;
		}

		return "";
	}

	public String createServiceOrder(String sorderJson) {

		logger.debug("createServiceOrder sorderJson=" + sorderJson);
		ServiceOrder contextServiceOrder = this.vars.getSorder();
		try {
			ServiceOrderCreate sonew = toJsonObj(sorderJson, new TypeReference<ServiceOrderCreate>() {
			});

			sonew.setRequestedStartDate(contextServiceOrder.getRequestedStartDate());
			sonew.setRequestedCompletionDate(contextServiceOrder.getRequestedCompletionDate());
			sonew.getOrderItem().stream().findFirst().get().setState(ServiceOrderStateType.ACKNOWLEDGED); // will be
																											// processed
																											// immediately
																											// by OSOM

			if (this.vars.getSoItem() != null) {
				ServiceOrderItemRelationship soitemrel = new ServiceOrderItemRelationship();
				;
				soitemrel.setId(this.vars.getSoItem().getId());
				soitemrel.setRelationshipType("DEPENDENCY");
				sonew.getOrderItem().stream().findFirst().get().addOrderItemRelationshipItem(soitemrel);
			}

			if (sonew.getRelatedParty() == null) {
				RelatedParty rp = new RelatedParty();
				rp.setName("OSOM LCM");
				rp.setRole("REQUESTER");
				sonew.addRelatedPartyItem(rp);
			}

			if (sonew.getNote() == null) {
				Note n = new Note();
				if (this.vars.getSorder() != null) {
					n.setText(String.format("Order created by LCM rule, %s, of orderid = %s", this.lcmspec.getName(),
							this.vars.getSorder().getId()));
				} else {
					n.setText(String.format("Order created by LCM rule, %s", this.lcmspec.getName()));
				}
				sonew.addNoteItem(n);
			}

			if (this.vars.getServiceOrderManager() != null) {
				ServiceOrder scorder = this.vars.getServiceOrderManager().createServiceOrder(sonew);
				return toJsonString(scorder);
			} else {

				logger.error("createServiceOrder serviceOrderManager is NULL!");
			}

		} catch (IOException e) {
			logger.error("createServiceOrder error=" + e.getLocalizedMessage());
			e.printStackTrace();
		}

		return null;
	}

	public void setCharacteristicOfCurrentService(String charName, String newValue) {
		logger.debug("setCharacteristicOfCurrentService " + charName + " = " + newValue);
		Optional<Characteristic> c = getCharacteristicByName(charName);
		c.ifPresent(val -> val.getValue().setValue(newValue));

		copyCharacteristicToServiceToUpdate(c.get());
	}

	static <T> T toJsonObj(String content, TypeReference<T> typeReference) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.readValue(content, typeReference);
	}

	static String toJsonString(Object object) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.writeValueAsString(object);
	}
}
