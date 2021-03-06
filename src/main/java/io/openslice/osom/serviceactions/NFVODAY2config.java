package io.openslice.osom.serviceactions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.tmf.common.model.Any;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceActionQueueItem;
import io.openslice.tmf.sim638.model.ServiceUpdate;

@Component(value = "NFVODAY2config") //bean name
public class NFVODAY2config implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( NFVODAY2config.class.getName() );


    @Autowired
    private ServiceOrderManager serviceOrderManager;
    
	public void execute(DelegateExecution execution) {
		
		logger.debug("NFVODAY2config:" + execution.getVariableNames().toString() );
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			ServiceActionQueueItem item;
			Service aService;
			item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
			aService = mapper.readValue( execution.getVariable("Service").toString(), Service.class);
			
			
			//extract the original service from the Item

			Service originalService =  mapper.readValue( item.getOriginalServiceInJSON() , Service.class);;
			
			List<Characteristic> changeCharacteristics = new ArrayList<>();
			//send to mano client here: only the modified action!
			//identify here the characteristics that changed
			if ( aService.getServiceCharacteristic()!=null ) {
				for (Characteristic srcChar : aService.getServiceCharacteristic()) {
					
						if ( originalService.getServiceCharacteristicByName( srcChar.getName() )!= null ) {
							
							Characteristic origChar = originalService.getServiceCharacteristicByName( srcChar.getName() );
							if ( ( origChar !=null ) && ( origChar.getValue() !=null ) && ( origChar.getValue().getValue() !=null )) {
								if ( !origChar.getValue().getValue().equals(srcChar.getValue().getValue()) ) {
									changeCharacteristics.add( srcChar );									
								}
							}							
						}
				}						
			}
			
			
			
			
			Note n = new Note();
			n.setText("Service Action NFVODAY2config. Action: " + item.getAction() +". " );
			n.setAuthor( "OSOM" );
			n.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
			
			
			String nsInstanceId = "";
			if ( aService.getServiceCharacteristicByName( "InstanceId" )!=null) {
				nsInstanceId= aService.getServiceCharacteristicByName( "InstanceId" ).getValue().getValue() ;
			} else {
				logger.error("NFVODAY2config: InstanceId is NULL."  );
				
			}
			
			
			String ncTxt = "";
			for (Characteristic characteristic : changeCharacteristics) {
				ncTxt += characteristic.getName() + ", ";
				
				if ( ncTxt.toUpperCase().contains(  "PRIMITIVE::" ) ) {
					if ( characteristic.getValueType().equals("ARRAY") ) {

						NSActionRequestPayload nsp = new NSActionRequestPayload();
						nsp.setNsInstanceId(nsInstanceId);
						
						
						String characteristicValue = characteristic.getValue().getValue();
						List<Any> vals = mapper.readValue( characteristicValue, new TypeReference<List<Any>>() {});

						logger.debug("NFVODAY2config: characteristicValue = " +characteristicValue );
						for ( Any actionValue : vals) {
							if ( actionValue.getAlias().equals("primitive") ) {
								nsp.setPrimitive( actionValue.getValue() );
							} else if ( actionValue.getAlias().equals("member_vnf_index") ) {
								nsp.setVnf_member_index( actionValue.getValue() );
							} else if ( actionValue.getAlias().equals("vdu_id") ) {
								nsp.setVdu_id( actionValue.getValue() );
							} else if ( actionValue.getAlias().equals("vdu_count_index") ) {
								nsp.setVdu_count_index( actionValue.getValue() );
							} else {

								nsp.getPrimitive_params().put( actionValue.getAlias() , actionValue.getValue());
							}
						}
						
						if ( nsp.getPrimitive() != null ) {
							/**
							 * {
								"nsInstanceId": "8a3db62a-eb0e-48d9-be9b-548f7f034512",
								"member_vnf_index": "?",
								"primitive": "?",
								"primitive_params": {
									"?": "?"
								}
							}
							 */
							
							String payload = mapper.writeValueAsString(nsp) ;	
							logger.debug("NFVODAY2config NSActionRequestPayload= " + payload );						
							String actionresult = serviceOrderManager.nfvoDay2Action(nsp);
							if ( actionresult.contains("ACCEPTED") ) {
								n.setText( n.getText() + "ACCEPTED" );
								
							} else {
								n.setText( n.getText() + " " + actionresult );								
							}
						}
						
						
						
					}
				}
				
				
			}
			
			
			ServiceUpdate supd = new ServiceUpdate();
			
			supd.addNoteItem( n );
			serviceOrderManager.deleteServiceActionQueueItem( item );			
			serviceOrderManager.updateService( aService.getId() , supd, false);

			logger.debug("NFVODAY2config:" + n.getText() );
			
		} catch (JsonMappingException e) {
			e.printStackTrace();
			return;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return;
		}
		
	}

}
