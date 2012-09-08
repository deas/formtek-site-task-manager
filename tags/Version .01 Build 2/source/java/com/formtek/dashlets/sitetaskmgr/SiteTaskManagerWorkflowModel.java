/*
 * Copyright (C) 2012 Formtek, Inc.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.formtek.dashlets.sitetaskmgr;

import org.alfresco.service.namespace.QName;

/**
 * Defines constants for the Site Task Manager Workflow data model.
 * 
 * @author Formtek
 */
public interface SiteTaskManagerWorkflowModel {

    static final String NAMESPACE = "http://www.formtek.com/model/dashlet/sitetaskmgr/wf/1.0";
    static final String PREFIX = "stmwf";

    static final QName ASPECT_WFPROPS = QName.createQName(NAMESPACE, "wfProps");
    static final QName PROP_BURDEN = QName.createQName(NAMESPACE, "burden");
    static final QName PROP_SITENAME = QName.createQName(NAMESPACE, "siteName");
    
    static final QName TYPE_BASESITETASKMGRWF = QName.createQName(NAMESPACE, "basesiteTaskMgrWF");
    static final QName PROP_PREVIOUSCOMMENT = QName.createQName(NAMESPACE, "previousComment");
    static final QName PROP_WFNAME = QName.createQName(NAMESPACE, "wfName");

    static final QName TYPE_ASSIGNEDTASK = QName.createQName(NAMESPACE, "assignedTask");
    static final QName PROP_TASKOUTCOME = QName.createQName(NAMESPACE, "assignedOutcome");

    static final QName TYPE_DONETASK = QName.createQName(NAMESPACE, "doneTask");
    static final QName PROP_DONEOUTCOME = QName.createQName(NAMESPACE, "doneOutcome");    
        
    static final QName TYPE_ARCHIVEDTASK = QName.createQName(NAMESPACE, "archivedTask");
    static final QName PROP_ARCHIVEOUTCOME = QName.createQName(NAMESPACE, "archiveOutcome");

}
