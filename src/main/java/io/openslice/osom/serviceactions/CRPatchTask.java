package io.openslice.osom.serviceactions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.openslice.model.ScaleDescriptor;
import io.openslice.osom.management.AlarmsService;
import io.openslice.osom.management.ServiceOrderManager;
import io.openslice.tmf.common.model.Any;
import io.openslice.tmf.common.model.EValueType;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.common.model.service.Note;
import io.openslice.tmf.sim638.model.Service;
import io.openslice.tmf.sim638.model.ServiceActionQueueItem;
import io.openslice.tmf.sim638.model.ServiceUpdate;

@Component(value = "CRPatchTask") // bean name
public class CRPatchTask implements JavaDelegate {

  private static final transient Log logger = LogFactory.getLog(CRPatchTask.class.getName());

  @Value("${spring.application.name}")
  private String compname;

  @Autowired
  private ServiceOrderManager serviceOrderManager;

  @Autowired
  AlarmsService alarmsService;

  public void execute(DelegateExecution execution) {

    logger.debug("CRPatchTask:" + execution.getVariableNames().toString());

    ObjectMapper mapper = new ObjectMapper();
    ServiceActionQueueItem item;
    Service aService;
    Service originalService;
    try {
      item = mapper.readValue(execution.getVariable("serviceActionItem").toString(),
          ServiceActionQueueItem.class);
      aService = mapper.readValue(execution.getVariable("Service").toString(), Service.class);
      // extract the original service from the Item

      originalService = mapper.readValue(item.getOriginalServiceInJSON(), Service.class);;
    } catch (JsonProcessingException e1) {
      e1.printStackTrace();
      return;
    }



    List<Characteristic> changeCharacteristics = new ArrayList<>();
    // send to mano client here: only the modified action!
    // identify here the characteristics that changed
    if (aService.getServiceCharacteristic() != null) {
      for (Characteristic srcChar : aService.getServiceCharacteristic()) {

        if (originalService.getServiceCharacteristicByName(srcChar.getName()) != null) {

          Characteristic origChar =
              originalService.getServiceCharacteristicByName(srcChar.getName());
          if ((origChar != null) && (origChar.getValue() != null)
              && (origChar.getValue().getValue() != null)) {
            if (!origChar.getValue().getValue().equals(srcChar.getValue().getValue())) {
              changeCharacteristics.add(srcChar);
            }
          }
        }
      }
    }



    Note n = new Note();
    n.setText("Service Action CRPatchTask does nothing for now. Action: " + item.getAction() + ". ");
    n.setAuthor(compname);
    n.setDate(OffsetDateTime.now(ZoneOffset.UTC).toString());



    ServiceUpdate supd = new ServiceUpdate();


    supd.addNoteItem(n);
    serviceOrderManager.deleteServiceActionQueueItem(item);
    serviceOrderManager.updateService(aService.getId(), supd, false);

    logger.debug("CRPatchTask:" + n.getText());



  }

}
