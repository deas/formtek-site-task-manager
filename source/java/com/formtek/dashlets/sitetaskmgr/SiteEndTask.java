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
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.alfresco.repo.web.scripts.workflow.AbstractWorkflowWebscript;
import org.alfresco.repo.web.scripts.workflow.WorkflowModelBuilder;
import org.alfresco.service.cmr.workflow.WorkflowPath;
import org.alfresco.service.cmr.workflow.WorkflowTask;


import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowDeployment;
import org.alfresco.service.cmr.workflow.WorkflowPath;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.cmr.workflow.WorkflowTransition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.formtek.dashlets.sitetaskmgr.SiteWFUtil;

/**
 * End the task and transition to next node
 * 
 * @author Formtek
 * @since 4.0 Site End Task
 */
public class SiteEndTask extends AbstractWorkflowWebscript
{
    private static Log logger = LogFactory.getLog(SiteEndTask.class);

    @Override
    protected Map<String, Object> buildModel(WorkflowModelBuilder modelBuilder, WebScriptRequest req, Status status, Cache cache)
    {
        Map<String, String> params = req.getServiceMatch().getTemplateVars();

        // getting workflow instance id from request parameters
        String taskId = getTaskId(req);
        logger.debug("Task Instance: " + taskId);
        
        // get the name of the site the workflow should be associated with
        String site = getSite(req);
        logger.debug("Workflow should be associated with site: " + site);
        
        // get the requested transition id for the workflow
        String transition = getTransition(req);
        logger.debug("Workflow requested to transition to state: " + transition);

        // get the WorkflowPath from the Workflow Instance ID
        WorkflowTask wfTask = workflowService.getTaskById(taskId);        
        if (wfTask == null)
        {
            throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND, "Failed to find workflow task id: " + taskId);
        }
        logger.debug("Retrieved the workflow task");
        
        // get the WorkflowPath from the Workflow Instance ID
        WorkflowPath wfPath = wfTask.getPath();
        if (wfPath == null)
        {
            throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND, "Failed to find workflow path for task id: " + taskId);
        }
        
        if (SiteWFUtil.canUserChangeWorkflow(wfPath, site, workflowService, nodeService, authenticationService, authorityService))
        {
            logger.debug("User authenticated and workflow to be transitioned.");
            
            // check the requested transition
            WorkflowTransition[] wfTransitions = wfPath.getNode().getTransitions();
            
            logger.debug("Identified " + wfTransitions.length + " transitions.");
             
            // Identify the transition name as valid, by checking list of available transitions
            int i=0;
            WorkflowTransition wfTransition = null;
            for (i = 0; i<wfTransitions.length; i++) 
            {
                 logger.debug("Checking Transition: " + wfTransitions[i].getTitle() + " and id: " + wfTransitions[i].getId());
                 if (wfTransitions[i].getId().equals(transition)) 
                 {
                     logger.debug("Found the transition: " + wfTransitions[i].getTitle());
                     wfTransition = wfTransitions[i];
                 }
            }
            if (wfTransition==null) 
            {
                logger.debug("No matching transition found");
                throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND, "Unable to find a transition that matches the name: " + transition);
            }
            
            // Do the transition
            logger.debug("Performing the transition to: " + transition);
            workflowService.endTask(taskId, wfTransition.getId());
            
            // return an empty model
            return new HashMap<String, Object>();
        }
        else
        {
            throw new WebScriptException(HttpServletResponse.SC_FORBIDDEN, "Failed to transition for task id: " + taskId);
        }       
     }
     
     /**
      * Returns the task id
      * @param req
      * @return
      */
     private String getTaskId(WebScriptRequest req)
     {
        String taskId = req.getParameter(SiteWFUtil.PARAM_TASK_ID);
        if (taskId == null || taskId.length() == 0)
        {
            taskId = null;
        }
        return taskId;
     }
     
     /**
      * Returns the site
      * @param req
      * @return
      */
     private String getSite(WebScriptRequest req)
     {
        String site = req.getParameter(SiteWFUtil.PARAM_SITE);
        if (site == null || site.length() == 0)
        {
            site = null;
        }
        return site;
     }
     
     /**
      * Returns the transition name
      * @param req
      * @return
      */
     private String getTransition(WebScriptRequest req)
     {
        String transition = req.getParameter(SiteWFUtil.PARAM_TRANSITION);
        if (transition == null || transition.length() == 0)
        {
            transition = null;
        }
        return transition;
     }
 }
