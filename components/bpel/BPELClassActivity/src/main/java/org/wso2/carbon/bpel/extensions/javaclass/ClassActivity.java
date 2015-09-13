/*
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.bpel.extensions.javaclass;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.common.FaultException;
import org.apache.ode.bpel.runtime.extension.ExtensionContext;

public abstract class ClassActivity {

    private static Log log = LogFactory.getLog(ClassActivity.class);

    /**
     * Initialize ClassActivity.
     * <p>
     * this will be invoked when ClassActivity Object is created. On any exception, ClassActivity will be threaded as
     * faulty. Example usage: Create DB connections, JMS Connections connections. etc.
     */
    public abstract void init();

    /**
     * Destroy ClassActivity Object.
     * <p>
     * This will be invoked when Server shutdowns. Example Usage: Close DB connections, JMS connections etc.
     */
    public abstract void destroy();

    /**
     * Invoke process.
     *
     * @param extensionContext BPEL extensionContext.
     * @param params           ClassActivity parameters.
     * @return true on successful, false on error. Activity will mark as completed with fault.
     */

    public abstract boolean process(ExtensionContext extensionContext, String[] params);

    /**
     * Perform an error log message to ERROR log and complete activity.
     *
     * @param extensionContext  BPEL extensionContext.
     * @param completeWithFault when true, activity completes with a fault. when false, complete normally.
     * @param errorMessage      Error message
     * @param exception
     */
    public void handleException(ExtensionContext extensionContext, boolean completeWithFault, String errorMessage, Exception exception) {
        if (log.isDebugEnabled()) {
            log.debug("Error occurred while processing ClassActivity. Activity completeWithFault :" + completeWithFault);
        }
        log.error(errorMessage, exception);
        if (completeWithFault) {
            extensionContext.completeWithFault(extensionContext.getCorrelatorId(),
                    new FaultException(ExtensionConstants.QNAME_FAULT_RUNTIME_ERROR, exception));
        } else {
            extensionContext.complete(extensionContext.getCorrelatorId());
        }
    }

    /**
     * Complete Activity
     *
     * @param extensionContext BPEL extensionContext.
     */
    public void completeActivity(ExtensionContext extensionContext) {
        if (log.isDebugEnabled()) {
            log.debug("Completing activity " + extensionContext.getCorrelatorId());
        }
        extensionContext.complete(extensionContext.getCorrelatorId());
    }

    /**
     * Get Logger for Given extensionContext.
     *
     * @param extensionContext BPEL extensionContext.
     * @return org.apache.commons.logging.Log
     */
    public Log getLogger(ExtensionContext extensionContext) {
        return LogFactory.getLog(extensionContext.getActivityName());
    }
}
