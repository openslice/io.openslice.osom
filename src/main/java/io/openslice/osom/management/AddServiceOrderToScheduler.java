package io.openslice.osom.management;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("addServiceOrderToScheduler")
public class AddServiceOrderToScheduler  implements JavaDelegate {

    public void execute(DelegateExecution execution) {
        System.out.println("Calling the external system for SCHEDULE PROCESS orderid "
            + execution.getVariable("orderid"));
    }

}
