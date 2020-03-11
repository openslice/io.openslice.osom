package io.openslice.osom.partnerservices;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;
import javax.validation.constraints.NotNull;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.common.model.service.ServiceSpecificationRef;
import io.openslice.tmf.pm632.model.Organization;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderCreate;
import io.openslice.tmf.so641.model.ServiceOrderItem;
import io.openslice.tmf.so641.model.ServiceOrderStateType;
import io.openslice.tmf.so641.model.ServiceRestriction;
import reactor.core.publisher.Mono;

@Service
public class PartnerOrganizationServicesManager {

	private static final transient Log logger = LogFactory.getLog(PartnerOrganizationServicesManager.class.getName());

	@Autowired
	private ProducerTemplate template;

	@Value("${CATALOG_GET_EXTERNAL_SERVICE_PARTNERS}")
	private String CATALOG_GET_EXTERNAL_SERVICE_PARTNERS = "";


	@Value("${CATALOG_UPD_EXTERNAL_SERVICESPEC}")
	private String CATALOG_UPD_EXTERNAL_SERVICESPEC = "";
	

	
	Map<String, WebClient> webclients = new HashMap<>();

	public List<Organization> retrievePartners() {
		logger.info("will retrieve Service Providers  from catalog ");
		try {
			Map<String, Object> map = new HashMap<>();
			Object response = template.requestBodyAndHeaders(CATALOG_GET_EXTERNAL_SERVICE_PARTNERS, "", map);

			if (!(response instanceof String)) {
				logger.error("List  object is wrong.");
				return null;
			}

			Class<List<Organization>> clazz = (Class) List.class;
			List<Organization> organizations = mapJsonToObjectList(new Organization(), (String) response,
					Organization.class);
			logger.info("retrieveSPs response is: " + response);
			return organizations;

		} catch (Exception e) {
			logger.error("Cannot retrieve Listof Service Providers from catalog. " + e.toString());
		}
		return null;
	}

	protected static <T> List<T> mapJsonToObjectList(T typeDef, String json, Class clazz) throws Exception {
		List<T> list;
		ObjectMapper mapper = new ObjectMapper();
		System.out.println(json);
		TypeFactory t = TypeFactory.defaultInstance();
		list = mapper.readValue(json, t.constructCollectionType(ArrayList.class, clazz));

//	      System.out.println(list);
//	      System.out.println(list.get(0).getClass());
		return list;
	}

	public List<ServiceSpecification> fetchServiceSpecs(Organization org) {
		logger.info("Will fetchServiceSpecs of organization: " + org.getName() + ", id: " + org.getId());

		WebClient webclient = this.getOrganizationWebClient(org);

		List<ServiceSpecification> specs = new ArrayList<>();
		
		if ( webclient!=null ) {
			
			specs = webclient.get()
					.uri("/tmf-api/serviceCatalogManagement/v4/serviceSpecification")
						//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
						.retrieve()
						.onStatus(HttpStatus::is4xxClientError, response -> {
							logger.error("4xx eror");
					        return Mono.error(new RuntimeException("4xx"));
					      })
					      .onStatus(HttpStatus::is5xxServerError, response -> {
					    	  logger.error("5xx eror");
					        return Mono.error(new RuntimeException("5xx"));
					      })
					  .bodyToMono( new ParameterizedTypeReference<List<ServiceSpecification>>() {})
					  .block();
			
		} else  {
			logger.error("WebClient is null. Cannot be created.");
		}

		/**
		 * will create or fetch existing web client for this organization
		 */

		return specs;
	}

	private WebClient getOrganizationWebClient(Organization org) {
		
		if (webclients.get(org.getId()) != null) {
			return webclients.get( org.getId() );
		} else {
			
			String[] scopes = {"admin" , "read"};
//			GenericClient oac = new GenericClient(
//					"authOpensliceProvider", 
//					"osapiWebClientId",
//					"secret",
//					scopes ,
//					"http://portal.openslice.io/osapi-oauth-server/oauth/token",
//					"admin", 
//					"openslice", 
//					"http://portal.openslice.io" );

			try {

				String strapiparams = org.getPartyCharacteristic().stream().findFirst().get().getValue().getValue();
			ObjectMapper mapper = new ObjectMapper();
			mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
			Map<String, Object> apiparams = mapper.readValue(strapiparams, Map.class);
			
			
			
			GenericClient oac = new GenericClient(
					(String) apiparams.get("CLIENTREGISTRATIONID"), 
					(String) apiparams.get("OAUTH2CLIENTID"), 
					(String) apiparams.get("OAUTH2CLIENTSECRET"), 
					((ArrayList<String>) apiparams.get("OAUTH2SCOPES")).toArray( new String[0]), 
					(String) apiparams.get("OAUTH2TOKENURI"), 
					(String) apiparams.get("USERNAME"), 
					(String) apiparams.get("PASSWORD"), 
					(String) apiparams.get("BASEURL") );
			
			WebClient webClient;
				webClient = oac.createWebClient();
				webclients.put( org.getId() , webClient);
				return webClient;
			} catch (SSLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		return null;
	}

	public ServiceSpecification updateSpecInLocalCatalog(String orgid, ServiceSpecification serviceSpecification) {
		logger.info("Will UpdateSpecInLocalCatalog serviceSpecification name: " + serviceSpecification.getName() + ", id: " + serviceSpecification.getId());

		try {
			Map<String, Object> map = new HashMap<>();
			map.put("servicespecid", serviceSpecification.getId() );
			map.put("orgid", orgid );
			Object response = template.requestBodyAndHeaders( CATALOG_UPD_EXTERNAL_SERVICESPEC, toJsonString( serviceSpecification ), map);

			if ( !(response instanceof String)) {
				logger.error("Service Spec object is wrong.");
			}

			ServiceSpecification serviceSpecResp = toJsonObj( (String)response, ServiceSpecification.class); 
			//logger.debug("createService response is: " + response);
			return serviceSpecResp;
			
			
		}catch (Exception e) {
			logger.error("Cannot update Service Spec : " + serviceSpecification.getId() + ": " + e.toString());
		}
		return null;

	}
	

	static <T> T toJsonObj(String content, Class<T> valueType) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.readValue(content, valueType);
	}

	static String toJsonString(Object object) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.writeValueAsString(object);
	}

	public ServiceOrder makeExternalServiceOrder(ServiceOrderCreate servOrder, Organization org, String remoteServiceSpecID) {
		logger.info("Will makeExternalServiceOrder to organization: " + org.getName() + ", id: " + org.getId());

		/**
		 * will create or fetch existing web client for this organization
		 */
		WebClient webclient = this.getOrganizationWebClient(org);


		
		
		ServiceOrder sorder = new ServiceOrder();
		
		if ( webclient!=null ) {
			
			
			sorder = webclient.post()
					.uri("/tmf-api/serviceOrdering/v4/serviceOrder")
				      //.header("Authorization", "Basic " + encodedClientData)
				      .bodyValue( servOrder ) 
						//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
						.retrieve()
						.onStatus(HttpStatus::is4xxClientError, response -> {
							logger.error("4xx eror");
					        return Mono.error(new RuntimeException("4xx"));
					      })
					      .onStatus(HttpStatus::is5xxServerError, response -> {
					    	  logger.error("5xx eror");
					        return Mono.error(new RuntimeException("5xx"));
					      })
					  .bodyToMono( new ParameterizedTypeReference<ServiceOrder>() {})
					  .block();
		

			 
			 
		} else  {
			logger.error("WebClient is null. Cannot be created.");
		}

		

		return sorder;
	}

	public ServiceOrder retrieveServiceOrder(Organization org, String externalServiceOrderId) {

		logger.info("Will retrieveServiceOrder from organization: " + org.getName() + ", id: " + org.getId());

		/**
		 * will create or fetch existing web client for this organization
		 */
		WebClient webclient = this.getOrganizationWebClient(org);

		
		

		ServiceOrder sorder = new ServiceOrder();
		if ( webclient!=null ) {
			
			
			sorder = webclient.get()
					.uri("/tmf-api/serviceOrdering/v4/serviceOrder/{id}", externalServiceOrderId)
				      //.header("Authorization", "Basic " + encodedClientData)
						//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
						.retrieve()
						.onStatus(HttpStatus::is4xxClientError, response -> {
							logger.error("4xx eror");
					        return Mono.error(new RuntimeException("4xx"));
					      })
					      .onStatus(HttpStatus::is5xxServerError, response -> {
					    	  logger.error("5xx eror");
					        return Mono.error(new RuntimeException("5xx"));
					      })
					  .bodyToMono( new ParameterizedTypeReference<ServiceOrder>() {})
					  .block();
		

			 
			 
			
		} else  {
			logger.error("WebClient is null. Cannot be created.");
		}

		

		return sorder;
	}

	public io.openslice.tmf.sim638.model.Service retrieveServiceFromInventory(@NotNull Organization org, @NotNull String externalServiceId) {
	
		logger.info("Will retrieveServiceFromInventory from organization: " + org.getName() + ", id: " + org.getId());

		/**
		 * will create or fetch existing web client for this organization
		 */
		WebClient webclient = this.getOrganizationWebClient(org);
		

		io.openslice.tmf.sim638.model.Service srvc = new io.openslice.tmf.sim638.model.Service();
		if ( webclient!=null ) {
			
			
			srvc = webclient.get()
					.uri("/tmf-api/serviceInventory/v4/service/{id}", externalServiceId)
				      //.header("Authorization", "Basic " + encodedClientData)
						//.attributes( ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("authOpensliceProvider"))
						.retrieve()
						.onStatus(HttpStatus::is4xxClientError, response -> {
							logger.error("4xx eror");
					        return Mono.error(new RuntimeException("4xx"));
					      })
					      .onStatus(HttpStatus::is5xxServerError, response -> {
					    	  logger.error("5xx eror");
					        return Mono.error(new RuntimeException("5xx"));
					      })
					  .bodyToMono( new ParameterizedTypeReference<io.openslice.tmf.sim638.model.Service>() {})
					  .block();
		

			 
			 
			
		} else  {
			logger.error("WebClient is null. Cannot be created.");
		}
		

		return srvc;
	}

}
