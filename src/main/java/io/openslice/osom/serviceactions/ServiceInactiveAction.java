package io.openslice.osom.serviceactions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.osom.lcm.LCMRulesController;
import io.openslice.osom.lcm.LCMRulesExecutorVariables;
import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.lcm.model.ELCMRulePhase;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceActionQueueItem;
import io.openslice.tmf.sim638.model.ServiceUpdate;
import io.openslice.tmf.so641.model.ServiceOrder;
import io.openslice.tmf.so641.model.ServiceOrderItem;
import io.openslice.tmf.so641.model.ServiceOrderUpdate;

@Component(value = "ServiceInactiveAction") //bean name
public class ServiceInactiveAction  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( ServiceInactiveAction.class.getName() );

	@Value("${spring.application.name}")
	private String compname;

    @Autowired
    private ServiceOrderManager serviceOrderManager;

	@Autowired
	private LCMRulesController lcmRulesController;
    
	public void execute(DelegateExecution execution) {
		
		logger.info("ServiceInactiveAction:" + execution.getVariableNames().toString() );
		
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			ServiceActionQueueItem item;
			Service aService;
			item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
			aService = mapper.readValue( execution.getVariable("Service").toString(), Service.class);
			
			ServiceUpdate supd = new ServiceUpdate();
			Note n = new Note();
			n.setText("Service Action ServiceInactiveAction. Action: " + item.getAction() );
			n.setAuthor( compname );
			n.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
			supd.addNoteItem( n );
			serviceOrderManager.deleteServiceActionQueueItem( item );
			
			ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aService.getServiceSpecificationRef().getId() );//fetch the equivalent spec;			
			ServiceOrder sor = null;
			ServiceOrderItem soi = null;
			
			if ( aService.getServiceOrder().size() >0  ) {
				sor = serviceOrderManager.retrieveServiceOrder( aService.getServiceOrder().stream().findFirst().get().getId() ) ;
				if ( sor.getOrderItem().size()>0) {
					soi = sor.getOrderItem().stream().findFirst().get();
				}
			}
			
			
			if ( spec!= null ) {
				//execute any LCM rules "AFTER_DEACTIVATION" phase for the SPEC;
				LCMRulesExecutorVariables vars = new LCMRulesExecutorVariables(spec, sor, soi, null, supd, aService, serviceOrderManager);
				
				logger.debug("===============BEFORE lcmRulesController.execPhas AFTER_DEACTIVATION for spec:" + spec.getName() + " =============================");
				vars = lcmRulesController.execPhase( ELCMRulePhase.AFTER_DEACTIVATION, vars );

				//logger.debug("vars= " + vars );		
				logger.debug("===============AFTER lcmRulesController.execPhas =============================");
				
				if ( vars.getCompileDiagnosticErrors().size()>0 ) {
					Note noteItem = new Note();
					String msg = "LCM Rule execution error by ServiceInactiveAction. ";
					for (String tmsg :  vars.getCompileDiagnosticErrors()) {
						msg = msg + "\n"+ tmsg;
					}
					noteItem.setText( msg );
					noteItem.setAuthor( compname );
					supd.addNoteItem(noteItem);
				}
			}
			
			
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
