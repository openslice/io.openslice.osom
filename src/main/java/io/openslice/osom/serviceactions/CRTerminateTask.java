package io.openslice.osom.serviceactions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.openslice.model.DeploymentDescriptor;
import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.common.model.service.ResourceRef;
import io.openslice.tmf.rcm634.model.ResourceSpecificationRef;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceActionQueueItem;
import io.openslice.tmf.sim638.model.ServiceUpdate;

@Component(value = "CRTerminateTask") //bean name
public class CRTerminateTask  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( CRTerminateTask.class.getName() );

	@Value("${spring.application.name}")
	private String compname;

    @Autowired
    private ServiceOrderManager serviceOrderManager;
    
	public void execute(DelegateExecution execution) {
		
		logger.info("CRTerminateTask:" + execution.getVariableNames().toString() );

		Service aService = null;
		if (execution.getVariable("Service")!=null) {
			ObjectMapper mapper = new ObjectMapper();
			
			try {
				aService = mapper.readValue( execution.getVariable("Service").toString(), Service.class);
			} catch (JsonMappingException e) {
				e.printStackTrace();
				return;
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return;
			}
		}

		logger.info("Will terminate Service with id:" + aService.getId() );



        try {
          if (aService.getServiceCharacteristicByName( "_CR_SPEC" ) != null ){
            String crspec = aService.getServiceCharacteristicByName( "_CR_SPEC" ).getValue().getValue();

            if (crspec != null) {
              logger.info("Will terminate CR related to service. We need to fetchthe underlying resource"  );

              //we need to get the equivalent resource spec. since ServiceSpec is an RFS
              ResourceRef resRef = aService.getSupportingResource().stream().findFirst().get();
              

              Map<String, Object> map = new HashMap<>();
              map.put("org.etsi.osl.serviceId", aService.getId() );
              map.put("org.etsi.osl.resourceId", resRef.getId() );
              map.put("org.etsi.osl.statusCheckFieldName",  getServiceCharacteristic(aService, "_CR_CHECK_FIELD")    );
              map.put("org.etsi.osl.statusCheckValueStandby", getServiceCharacteristic(aService, "_CR_CHECKVAL_STANDBY")  );
              map.put("org.etsi.osl.statusCheckValueAlarm", getServiceCharacteristic(aService, "_CR_CHECKVAL_ALARM")  );
              map.put("org.etsi.osl.statusCheckValueAvailable", getServiceCharacteristic(aService, "_CR_CHECKVAL_AVAILABLE")  );
              map.put("org.etsi.osl.statusCheckValueReserved", getServiceCharacteristic(aService, "_CR_CHECKVAL_RESERVED")  );
              map.put("org.etsi.osl.statusCheckValueUnknown", getServiceCharacteristic(aService, "_CR_CHECKVAL_UNKNOWN")  );
              map.put("org.etsi.osl.statusCheckValueSuspended", getServiceCharacteristic(aService, "_CR_CHECKVAL_SUSPENDED")  );
              
              serviceOrderManager.cridgeDeletionRequest( map, crspec);
              
              
            }
            
            
          }
        } catch (Exception e) {
          e.printStackTrace();

        }

		
		
		try {
			ServiceActionQueueItem item;

			ObjectMapper mapper = new ObjectMapper();
			item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
			aService = mapper.readValue( execution.getVariable("Service").toString(), Service.class);
			
			ServiceUpdate supd = new ServiceUpdate();
			Note n = new Note();
			n.setText("Service Action CRTerminateTask. Action: " + item.getAction() );
			n.setAuthor( compname );
			n.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
			supd.addNoteItem( n );
			serviceOrderManager.deleteServiceActionQueueItem( item );			
			serviceOrderManager.updateService( aService.getId() , supd, false);
			
			
		} catch (JsonMappingException e) {
			e.printStackTrace();
			return;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return;
		}
	}


	  private Object getServiceCharacteristic(Service aService, String val) {
	    if (aService.getServiceCharacteristicByName( val ) !=null ) {
	      return aService.getServiceCharacteristicByName( val ).getValue().getValue();
	    }
	    return "";
	  }

}
