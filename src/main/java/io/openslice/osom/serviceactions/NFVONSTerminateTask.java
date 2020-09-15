package io.openslice.osom.serviceactions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.model.DeploymentDescriptor;
import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.tmf.common.model.Any;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceActionQueueItem;
import io.openslice.tmf.sim638.model.ServiceUpdate;

@Component(value = "NFVONSTerminateTask") //bean name
public class NFVONSTerminateTask  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( NFVONSTerminateTask.class.getName() );


    @Autowired
    private ServiceOrderManager serviceOrderManager;
    
	public void execute(DelegateExecution execution) {
		
		logger.info("NFVONSTerminateTask:" + execution.getVariableNames().toString() );

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


		
		if (aService.getServiceCharacteristicByName( "DeploymentRequestID" ) != null ){
			String deploymentRequestID = aService.getServiceCharacteristicByName( "DeploymentRequestID" ).getValue().getValue();
			logger.info("Will terminate DeploymentRequestID:" + deploymentRequestID );
			DeploymentDescriptor dd =serviceOrderManager.retrieveNFVODeploymentRequestById( Long.parseLong( deploymentRequestID ) );
			dd.setEndDate( new Date() ); // it will terminate it now
			serviceOrderManager.nfvoDeploymentRequestByNSDid(dd);
		}
		
		
		try {
			ServiceActionQueueItem item;

			ObjectMapper mapper = new ObjectMapper();
			item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
			aService = mapper.readValue( execution.getVariable("Service").toString(), Service.class);
			
			ServiceUpdate supd = new ServiceUpdate();
			Note n = new Note();
			n.setText("Service Action NFVONSTerminateTask. Action: " + item.getAction() );
			n.setAuthor( "OSOM" );
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

	

}
