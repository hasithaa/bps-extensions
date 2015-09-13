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

import javax.xml.namespace.QName;
import java.util.HashMap;

/**
 * ClassActivity holder class
 */
public class ClassActivityHolder {

    private static Log log = LogFactory.getLog(ClassActivityHolder.class);

    private static ClassActivityHolder instance;
    /**
     * We will keep Single ClassActivity object for Single Process version. This is to reduce overhead of ClassActivity
     * object creation, invoking init and destroy methods.
     * <p>
     * Following HashMap will be used for storing ClassActivities. Following mapping will be used.
     * <p>
     * 1 ProcessDefinition -> Many ClassActivities.
     * HashMap< ProcessVersion , HashMap< Class name , ClassActivity> >
     * <p>
     * TODO: Document following line.
     * It is recommended to version ClassActivity classes along with process versions.
     * Eg: HelloWorld1 -> com.example.bpel.log.v1.LogActivity
     * HelloWorld2 -> com.example.bpel.log.v2.LogActivity
     */
    private HashMap<QName, HashMap<String, ClassActivity>> classActivities;

    private ClassActivityHolder() {
        classActivities = new HashMap<QName, HashMap<String, ClassActivity>>();
        if (log.isDebugEnabled()) {
            log.debug("Activity Content Holder initialized.");
        }
    }

    /**
     * Get ClassActivityHolder instance
     *
     * @return ClassActivityHolder
     */
    protected static ClassActivityHolder getInstance() {
        if (instance == null) {
            instance = new ClassActivityHolder();
        }
        return instance;
    }

    /**
     * Get ClassActivity for given processID and ClassName
     *
     * @param processVersionedID versioned process instance ID QName.
     * @param className          class name
     * @return ClassActivity Object. returns null if Object is not an instance of ClassActivity
     * @throws FaultException
     */
    protected ClassActivity getClassActivity(QName processVersionedID, String className) throws FaultException {

        HashMap<String, ClassActivity> classActivitiesPerProcessID;

        if (classActivities.containsKey(processVersionedID)) {
            classActivitiesPerProcessID = classActivities.get(processVersionedID);

            if (classActivitiesPerProcessID != null) {
                if (classActivitiesPerProcessID.containsKey(className)) {
                    if (log.isDebugEnabled()) {
                        log.debug(processVersionedID + " : " + className + " is exists.");
                    }
                    return classActivitiesPerProcessID.get(className);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(processVersionedID + " : " + className + " is not found. Initializing object.");
                    }
                    ClassActivity classActivity = lookupClassActivity(className);
                    classActivitiesPerProcessID.put(className, classActivity);
                    return classActivity;
                }
            } else {
                // classActivitiesPerProcessID can't be null. because, outer else condition always set empty HashMap.
                log.error("classActivitiesPerProcessID can't be null, But found null.");
                return null;
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(processVersionedID + " : " + className + "is initializing." +
                        " ClassActivity will be initializing first time for " + processVersionedID);
            }
            classActivitiesPerProcessID = new HashMap<String, ClassActivity>();
            classActivities.put(processVersionedID, classActivitiesPerProcessID);
            ClassActivity classActivity = lookupClassActivity(className);
            classActivitiesPerProcessID.put(className, classActivity);
            return classActivity;
        }
    }


    /**
     * Creates ClassActivity Object.
     *
     * @param className Class Name
     * @return ClassActivity
     * @throws FaultException
     */
    private ClassActivity lookupClassActivity(String className) throws FaultException {
        try {
            Object c = Class.forName(className).newInstance();
            ClassActivity classActivity = null;
            if (c instanceof ClassActivity) {
                classActivity = (ClassActivity) c;
                classActivity.init();
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(className + " is not a ClassActivity..");
                }
                // return null;
            }
            return classActivity;
        } catch (Exception e) {
            throw new FaultException(ExtensionConstants.QNAME_FAULT_RUNTIME_ERROR,
                    e.getMessage(), e.getCause());
        }
    }

    /**
     * Destroy all ClassActivities.
     */
    protected void destroyClassActivities() {
        for (QName processID : classActivities.keySet()) {
            HashMap<String, ClassActivity> stringClassActivityHashMap = classActivities.get(processID);
            for (String className : stringClassActivityHashMap.keySet()) {
                if (log.isDebugEnabled()) {
                    log.debug("Destroying ClassActivity " + className + " in " + processID);
                }
                ClassActivity classActivity = stringClassActivityHashMap.get(className);
                if (classActivity != null) {
                    classActivity.destroy();
                    if (log.isDebugEnabled()) {
                        log.debug("Destroying ClassActivity Done : " + className + " in " + processID);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Destroying ClassActivity(null) Done :" + className + " in " + processID);
                    }
                }
            }
        }
    }
}
