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

import org.apache.ode.bpel.common.FaultException;
import org.apache.ode.bpel.runtime.extension.AbstractSyncExtensionOperation;
import org.apache.ode.bpel.runtime.extension.ExtensionContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.util.ArrayList;

public class ClassActivitySyncExtensionOperation extends AbstractSyncExtensionOperation {


    private String name;
    private String className;
    private String[] params;

    @Override
    protected void runSync(ExtensionContext extensionContext, Element element) throws FaultException {
        parseElement(element);
        invokeClass(extensionContext);
    }

    private void invokeClass(ExtensionContext extensionContext) throws FaultException {
        ClassActivity classActivity;

        String duDir = extensionContext.getDUDir().toString();
        String duVersion = duDir.substring(duDir.lastIndexOf('-') + 1);
        if (duVersion.endsWith("/")) {
            duVersion = duVersion.substring(0, duVersion.lastIndexOf("/"));
        } else if (duVersion.endsWith("\\")) {
            duVersion = duVersion.substring(0, duVersion.lastIndexOf("\\"));
        }


        QName processId = new QName(extensionContext.getProcessModel().getQName().getNamespaceURI(),
                extensionContext.getProcessModel().getQName().getLocalPart() + "-" +
                        duVersion);


        classActivity = ClassActivityHolder.getInstance().getClassActivity(processId, className);
        if (classActivity != null) {
            try {
                classActivity.process(extensionContext, params);
            } catch (Exception e) {
                extensionContext.completeWithFault(extensionContext.getCorrelatorId(),
                        new FaultException(ExtensionConstants.QNAME_FAULT_RUNTIME_ERROR,
                                "Unexpected error while invoking ClassActivity " + className, e));
            }
        } else {
            extensionContext.completeWithFault(extensionContext.getCorrelatorId(),
                    new FaultException(ExtensionConstants.QNAME_FAULT_RUNTIME_ERROR, "ClassActivity Not initialized properly."));
        }

    }

    private void parseElement(Element element) throws FaultException {

        //Validating element
        if (!element.getLocalName().equals(ExtensionConstants.CLASS_ACTIVITY) ||
                !element.getNamespaceURI().equals(ExtensionConstants.NAMESPACE)) {
            throw new FaultException(ExtensionConstants.QNAME_FAULT_MALFORMED_ACTIVITY,
                    "No " + ExtensionConstants.CLASS_ACTIVITY + " activity found");
        }

        name = element.getAttribute(ExtensionConstants.ACTIVITY_NAME);
        className = element.getAttribute(ExtensionConstants.ACTIVITY_CLASS_NAME);
        if (className == null || className.equals("")) {
            throw new FaultException(ExtensionConstants.QNAME_FAULT_MALFORMED_ACTIVITY,
                    "Invalid class name found.");
        }

        NodeList paramNodeList = element.getChildNodes();
        Node param = null;
        ArrayList<String> paramsList = new ArrayList<String>();
        for (int i = 0; i < paramNodeList.getLength(); i++) {
            param = paramNodeList.item(i);
            // Processing only param elements
            if (ExtensionConstants.ACTIVITY_PARAM.equals(param.getLocalName()) &&
                    ExtensionConstants.NAMESPACE.equals(param.getNamespaceURI())) {
                try {
                    String textContent = param.getTextContent();
                    paramsList.add(textContent);
                } catch (Exception e) {
                    throw new FaultException(ExtensionConstants.QNAME_FAULT_MALFORMED_ACTIVITY,
                            "Found malformed param element");
                }
            }
        }
        params = new String[paramsList.size()];
        params = paramsList.toArray(params);
    }

}
