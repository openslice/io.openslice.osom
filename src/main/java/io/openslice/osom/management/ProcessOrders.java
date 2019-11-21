package io.openslice.osom.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component(value = "processOrders") //bean name
public class ProcessOrders  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(ProcessOrders.class.getName());
    public void execute(DelegateExecution execution) {
        logger.info("processOrders by Orchetrator");
    }
}
