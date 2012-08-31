/**
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

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.repo.web.scripts.workflow.AbstractWorkflowWebscript;
import org.alfresco.repo.web.scripts.workflow.WorkflowModelBuilder;
import org.alfresco.service.cmr.workflow.WorkflowPath;

import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowDeployment;
import org.alfresco.service.cmr.workflow.WorkflowPath;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.cmr.workflow.WorkflowTransition;

import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.workflow.WorkflowService;

import org.alfresco.repo.processor.BaseProcessorExtension;
import org.mozilla.javascript.ScriptableObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.formtek.dashlets.sitetaskmgr.SiteWFUtil;

/**
 * Determine if user has change privilege to be able to assess a site WF
 * 
 * @author Formtek
 * @since 4.0 Site Task Manager
 */


public final class HasSiteWFPrivilege extends BaseProcessorExtension {
 
 private static Log logger = LogFactory.getLog(HasSiteWFPrivilege.class);
 
 private WorkflowService workflowService;
 private NodeService nodeService;
 private AuthenticationService authenticationService;
 private AuthorityService authorityService;
 
 /**
  * 
  * @method setWorkflowService
  * @param workflowService
  */
 public void setWorkflowService(WorkflowService workflowService) {
     this.workflowService = workflowService;
 }
 
 /**
  * 
  * @method setNodeService
  * @param nodeService
  */
 public void setNodeService(NodeService nodeService) {
     this.nodeService = nodeService;
 }
 
 /**
  * 
  * @method setAuthenticationService
  * @param authenticationService
  */
 public void setAuthenticationService(AuthenticationService authenticationService) {
     this.authenticationService = authenticationService;
 }

 /**
  * 
  * @method setAuthorityService
  * @param authorityService
  */
 public void setAuthorityService(AuthorityService authorityService) {
     this.authorityService = authorityService;
 }
 
 /**
  * Javascript Routine to evaluate edit rights for a task
  *    Task can be edited by the Site Manager, an Adminstrator or the initiator
  * 
  * @method canEdit
  * @param taskId
  * @param site
  * @return
  */
 public boolean canEdit(String wfId, String site) 
 {
     logger.debug("canEdit called with workflow instance ID: " + wfId + " : site name: " + site);
     return SiteWFUtil.canUserChangeWorkflow(wfId, site, workflowService, nodeService, authenticationService, authorityService); 
 }

 
}
