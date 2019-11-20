package io.openslice.osom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class systemTaskMocked  implements JavaDelegate {


	private static final transient Log logger = LogFactory.getLog( systemTaskMocked.class.getName());
	
    public void execute(DelegateExecution execution) {
        logger.info("MOCKED, from systemTaskMocked ");
    }
}
