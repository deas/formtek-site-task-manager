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
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.alfresco.repo.web.scripts.workflow.AbstractWorkflowWebscript;
import org.alfresco.repo.web.scripts.workflow.WorkflowModelBuilder;
import org.alfresco.service.cmr.workflow.WorkflowPath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.formtek.dashlets.sitetaskmgr.SiteWFUtil;

/**
 * Web Script implementation of delete or cancel workflow instance for a site workflow
 * 
 * @author Modifications by Formtek  [Originally from Alfresco file WorkflowInstanceDelete.java]
 * @author Gavin Cornwell
 * @since 4.0 Site Task Manager
 */
public class SiteWorkflowInstanceDelete extends AbstractWorkflowWebscript
{
    private static Log logger = LogFactory.getLog(SiteWorkflowInstanceDelete.class);
    
    public static final String PARAM_FORCED = "forced";
    
    @Override
    protected Map<String, Object> buildModel(WorkflowModelBuilder modelBuilder, WebScriptRequest req, Status status, Cache cache)
    {
        Map<String, String> params = req.getServiceMatch().getTemplateVars();

        // getting workflow instance id from request parameters
        String workflowInstanceId = params.get("workflow_instance_id");
        logger.debug("Workflow should be associated with workflow instance: " + workflowInstanceId);
        
        // determine if instance should be cancelled or deleted
        boolean forced = getForced(req);
        logger.debug("Forced flag is set to: " + forced);
        
        // get the name of the site the workflow should be associated with
        String site = getSite(req);
        logger.debug("Workflow should be associated with site: " + site);
        
        // get the workflow path from the incoming workflow Id
        // for site task manager workflows, there will be a single path
        WorkflowPath wfPath = SiteWFUtil.getWorkflowPath(workflowInstanceId, workflowService);       

        if (SiteWFUtil.canUserChangeWorkflow(wfPath, site, workflowService, nodeService, authenticationService, authorityService))
        {
            if (forced)
            {
                workflowService.deleteWorkflow(workflowInstanceId);
            }
            else
            {
                workflowService.cancelWorkflow(workflowInstanceId);
            }
            
            return null;
        }
        else
        {
            throw new WebScriptException(HttpServletResponse.SC_FORBIDDEN, "Failed to " + 
                        (forced ? "delete" : "cancel") + " workflow instance with id: " + workflowInstanceId);
        }
    }
    
    private boolean getForced(WebScriptRequest req)
    {
        String forced = req.getParameter(PARAM_FORCED);
        if (forced != null)
        {
            try
            {
                return Boolean.valueOf(forced);
            }
            catch (Exception e)
            {
                // do nothing, false will be returned
            }
        }

        // Defaults to false.
        return false;
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
}
