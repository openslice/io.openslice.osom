package io.openslice.osom.partnerservices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.tmf.pm632.model.Organization;
import io.openslice.tmf.scm633.model.ServiceSpecification;


@Service
public class PartnerOrganizationServicesManager {


	private static final transient Log logger = LogFactory.getLog( PartnerOrganizationServicesManager.class.getName());

    @Autowired
    private ProducerTemplate template;

	@Value("${CATALOG_GET_EXTERNAL_SERVICE_PARTNERS}")
	private String CATALOG_GET_EXTERNAL_SERVICE_PARTNERS = "";
	
	
	public List<Organization> retrievePartners() {
		logger.info("will retrieve Service Providers  from catalog "   );
		try {
			Map<String, Object> map = new HashMap<>();
			Object response = template.
					requestBodyAndHeaders( CATALOG_GET_EXTERNAL_SERVICE_PARTNERS, "", map );

			if ( !(response instanceof String)) {
				logger.error("List  object is wrong.");
				return null;
			}

			Class<List<Organization>> clazz = (Class) List.class;
			List<Organization> organizations = mapJsonToObjectList( new Organization(), (String)response, Organization.class  ); 
			logger.info("retrieveSPs response is: " + response);
			return organizations;
			
		}catch (Exception e) {
			logger.error("Cannot retrieve Listof Service Providers from catalog. " + e.toString());
		}
		return null;
	}


	 protected static <T> List<T> mapJsonToObjectList(T typeDef,String json,Class clazz) throws Exception
	   {
	      List<T> list;
	      ObjectMapper mapper = new ObjectMapper();
	      System.out.println(json);
	      TypeFactory t = TypeFactory.defaultInstance();
	      list = mapper.readValue(json, t.constructCollectionType(ArrayList.class,clazz));

//	      System.out.println(list);
//	      System.out.println(list.get(0).getClass());
	      return list;
	   }


	public List<ServiceSpecification> fetchServiceSpecs(Organization org) {
		logger.info("Will fetchServiceSpecs of organization: " + org.getName() );
		List<ServiceSpecification> specs = new ArrayList<>();
		return specs;
	}


	public void updateSpecInLocalCatalog(ServiceSpecification serviceSpecification) {
		// TODO Auto-generated method stub
		
	}
	 
	 
	 
}
