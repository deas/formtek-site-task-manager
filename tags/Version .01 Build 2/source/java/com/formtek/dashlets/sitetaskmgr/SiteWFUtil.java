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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.alfresco.service.cmr.workflow.WorkflowService;

import org.alfresco.service.cmr.workflow.WorkflowPath;
import org.alfresco.service.cmr.workflow.WorkflowNode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Formtek, Inc.
 * @since 4.0 Site Task Manager
 */
public class SiteWFUtil
{
    private static Log logger = LogFactory.getLog(SiteWFUtil.class);
    public static final String PARAM_SITE = "site";
    public static final String PARAM_TRANSITION = "transition";
    public static final String PARAM_WORKFLOW_INSTANCE_ID = "workflow_instance_id";
    public static final String PARAM_TASK_ID = "task_id";
    public static final String SITE_TASK_WF_NAME = "activiti$FTKSiteTaskManager";
    public static final String PROP_SITENAME = "stmwf_siteName";
    
    public static final String PROP_TASK_ASSIGN = "stmwf_assignedTask";
    public static final String PROP_TASK_DONE = "stmwf_doneTask";
    public static final String PROP_TASK_ARCHIVE = "stmwf_archivedTask";
    
    /**
     * Retrieve the Start WorkflowTask from the current Task
     *
     * @param WorkflowTask current wfTask
     * @param workflowService workflowService
     * @return WorkflowTask the start task
     */
    public static WorkflowTask getStartWorkflowTaskFromCurrent(WorkflowTask wfTask, WorkflowService workflowService)
    {   
        logger.debug("Getting the Start WorkflowTask from the current WorkflowTask: " + wfTask.getId());
        WorkflowPath wfPath = wfTask.getPath();
        if (wfPath == null)
        {
            throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND, "Failed to find workflow path for WorkflowTask id: " + wfTask.getId());
        }
        logger.debug("Retrieved WorkflowPath with id: " + wfPath.getId());
        WorkflowInstance wfInstance = wfPath.getInstance();
        
        WorkflowTask startTask = workflowService.getStartTask(wfInstance.getId());
        
        logger.debug("Found the Start WorkflowTask: " + startTask.getId());
        return startTask;
    }
    
    /**
     * Retrieve the workflow path for the specified workflow
     *
     * @param Workflow wfId
     * @param workflowService workflowService
     * @return WorkflowPath wfPath
     */
    public static WorkflowPath getWorkflowPath(String wfId, WorkflowService workflowService)
    {   
        logger.debug("Getting the workflow path from workflow instance: " + wfId);
        List<WorkflowPath> wfPaths = workflowService.getWorkflowPaths(wfId);       
        if (wfPaths == null || wfPaths.size()!=1)
        {
            throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND, "Failed to find workflow paths for workflow instance id: " + wfId);
        }
        WorkflowPath wfPath = wfPaths.get(0);
        logger.debug("Retrieved WorkflowPath with id: " + wfPath.getId());
        return wfPath;
    }
 
    /**
     * Retrieve the workflow path for the specified workflow
     * @param WorkflowPath wfPath
     * @param workflowService workflowService
     * @return WorkflowTask workflowTask
     */
    public static WorkflowTask getWorkflowTask(WorkflowPath wfPath, WorkflowService workflowService)
    {     
        logger.debug("Getting the workflow task");
        List<WorkflowTask> wfTasks = workflowService.getTasksForWorkflowPath(wfPath.getId());
        if (wfTasks == null || wfTasks.size()==0)
        {
            throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND, "Failed to find workflow tasks for workflow instance id: " + wfPath.getInstance().getId());
        }
        // Get the last or the most current task
        WorkflowTask workflowTask = wfTasks.get(wfTasks.size()-1);
        logger.debug("Retrieved WorkflowTask with id: " + workflowTask.getId());
        
        return workflowTask;
    }
    
    /**
     * Checks that the Workflow task is correctly marked as associated with the site
     * @param Workflow task
     * @param String site
     * @return
     */
    public static boolean isSiteTask(WorkflowTask task, String site, WorkflowService workflowService)
    {
        logger.debug("Checking if task is in the site: " + site);  
        
        // Check the site ownership from the Start Task
        WorkflowTask startTask = getStartWorkflowTaskFromCurrent(task, workflowService);
        if (startTask.getProperties().get(SiteTaskManagerWorkflowModel.PROP_SITENAME) == null)
        {
            logger.debug("No sitename property for workflow start task");
            return false;                  
        }
        else if(!site.equals(startTask.getProperties().get(SiteTaskManagerWorkflowModel.PROP_SITENAME).toString()))
        {
            logger.debug("Sitename in Start task for workflow does not match: " + task.getProperties().get(SiteTaskManagerWorkflowModel.PROP_SITENAME).toString());
            return false;
        }
        logger.debug("Matching start task sitename is: " + startTask.getProperties().get(SiteTaskManagerWorkflowModel.PROP_SITENAME).toString()); 
        return true;
    }    
        
    /**
     * Returns the username of the user that initiated the workflow the
     * given task is part of.
     * 
     * @param path The workflow path
     * @param nodeService nodeService
     * @return Username or null if the initiator could not be found
     */
    public static String getWorkflowInitiatorUsername(WorkflowPath path, NodeService nodeService)
    {
        logger.debug("Getting the initiator user name");
        String initiator = null;        
        NodeRef initiatorRef = path.getInstance().getInitiator();
        
        if (initiatorRef != null && nodeService.exists(initiatorRef))
        {
            initiator = (String)nodeService.getProperty(initiatorRef, ContentModel.PROP_USERNAME);
        }
        logger.debug("Initiator is: " + initiator);
        return initiator;
    }

    /**
     * Determines if the current user can view the workflow contents
     *   Starts with the workflow instance id
     * 
     * @param site The name of the site the workflow should belong to
     * @param workflowService workflowService
     * @param nodeService nodeService
     * @param authenticationService authenticationService
     * @param authorityService authorityService
     * @return true if the user can view the workflow, false otherwise
     */
    public static boolean canUserViewWorkflow(String site, 
        WorkflowService workflowService, NodeService nodeService, 
        AuthenticationService authenticationService, AuthorityService authorityService)
    {    
        logger.debug("Checking view access rights for this site's workflows: " + site); 
        
        return  canAccessWorkflow("view", null, site, workflowService, nodeService, 
                                      authenticationService, authorityService);
    }    
    
    /**
     * Determines if the current user can cancel/delete or edit the workflow
     *   Starts with the workflow instance id
     * 
     * @param wfId The id of the workflow instance to check
     * @param site The name of the site the workflow should belong to
     * @param workflowService workflowService
     * @param nodeService nodeService
     * @param authenticationService authenticationService
     * @param authorityService authorityService
     * @return true if the user can change the workflow, false otherwise
     */
    public static boolean canUserChangeWorkflow(String wfId, String site, 
        WorkflowService workflowService, NodeService nodeService, 
        AuthenticationService authenticationService, AuthorityService authorityService)
    {    
        logger.debug("Checking change access rights for this workflow instance: " + wfId);
        // get the workflow path from the incoming workflow Id
        // for site task manager workflows, there will be a single path
        WorkflowPath wfPath = getWorkflowPath(wfId, workflowService);  
        
        return  canAccessWorkflow("edit", wfPath, site, workflowService, nodeService, 
                                      authenticationService, authorityService);
    }
    
    /**
     * Determines if the current user can cancel/delete or edit the workflow
     *   Starts with the workflow path
     * 
     * @param wfId The id of the workflow instance to check
     * @param site The name of the site the workflow should belong to
     * @param workflowService workflowService
     * @param nodeService nodeService
     * @param authenticationService authenticationService
     * @param authorityService authorityService
     * @return true if the user can change the workflow, false otherwise
     */
    public static boolean canUserChangeWorkflow(WorkflowPath path, String site, 
        WorkflowService workflowService, NodeService nodeService, 
        AuthenticationService authenticationService, AuthorityService authorityService)
    {           
        logger.debug("Checking change rights for this workflow instance (starting from path): " + path.getInstance().getId());
        return  canAccessWorkflow("edit", path, site, workflowService, nodeService, 
                                      authenticationService, authorityService);
    }
    
    
    /**
     * Determines if the current user can cancel/delete or edit the workflow
     *   Starts with the Workflow path
     * 
     * @param operation "edit" or "view" workflow
     * @param path The workflow path to check
     * @param site The name of the site the workflow should belong to
     * @param workflowService workflowService
     * @param nodeService nodeService
     * @param authenticationService authenticationService
     * @param authorityService authorityService
     * @return true if the user can change the workflow, false otherwise
     */
    public static boolean canAccessWorkflow(String operation, WorkflowPath path, String site, 
         WorkflowService workflowService, NodeService nodeService, 
         AuthenticationService authenticationService, AuthorityService authorityService)
    {
        boolean canAccess = false;
        
        // Access Security check
        // Check to make sure that only members of the specified site have access
        String currentUser = authenticationService.getCurrentUserName();
        logger.debug("Current User is: " + currentUser);
        
        Set<String> userAuthorities = authorityService.getAuthoritiesForUser(currentUser);
         
        if(operation == "view")
        {
            //  If the user is not a site member or administrator, then return false 
            if(  !userAuthorities.contains("GROUP_site_" + site) && 
                 !userAuthorities.contains("GROUP_ALFRESCO_ADMINISTRATORS"))
            {
                logger.debug("Exiting because user does not have authority or the workflow is not associated with the site.");
            }
            else
            {
                canAccess = true;
                logger.debug("User successfully authenticated for view");
            } 
        }
        else
        {
            // Get the current task of the path to see if it belongs to a site
            WorkflowTask workflowTask = getWorkflowTask(path, workflowService);
            
            // Query the initiator of the workflow
            logger.debug("Querying the initiator");
            String initiator = getWorkflowInitiatorUsername(path, nodeService);
            
          // Testing for edit rights
          logger.debug("Is this user [" + currentUser + "] in the SiteManager group?: " + userAuthorities.contains("GROUP_site_" + site + "_SiteManager"));
          logger.debug("Is this user an administrator?: " + authorityService.hasAdminAuthority());
          logger.debug("Is this user an administrator (via alfresco administrators group)?: " + userAuthorities.contains("GROUP_ALFRESCO_ADMINISTRATORS") );
          logger.debug("Is this user the initiator?: " + currentUser.equals(initiator) );
          logger.debug("Is this a site task?: " + isSiteTask(workflowTask, site, workflowService));
                           
            
          //  If the user is not the site manager, an administrator or the initiator, then return false
            if( initiator == null ||
                (!userAuthorities.contains("GROUP_site_" + site + "_SiteManager") && 
                 !userAuthorities.contains("GROUP_ALFRESCO_ADMINISTRATORS") &&
                 !currentUser.equals(initiator)) ||
                 !isSiteTask(workflowTask, site, workflowService) )
            {
                logger.debug("User does not have authority or the workflow is not associated with the site.");
            }
            else
            {
                canAccess = true;
                logger.debug("User successfully authenticated for change");
            } 
        }
        
        return canAccess;
    }
    
}