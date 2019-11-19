package io.openslice.osom.management;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class RejectServiceOrder  implements JavaDelegate {

    public void execute(DelegateExecution execution) {
        System.out.println("Calling the external system for REJECT orderid "
            + execution.getVariable("orderid"));
    }
}
