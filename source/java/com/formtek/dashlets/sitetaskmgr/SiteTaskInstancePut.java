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
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import org.alfresco.service.cmr.workflow.WorkflowPath;
import org.alfresco.service.cmr.workflow.WorkflowNode;
import org.alfresco.repo.web.scripts.workflow.AbstractWorkflowWebscript;
import org.alfresco.repo.web.scripts.workflow.WorkflowModelBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.formtek.dashlets.sitetaskmgr.SiteWFUtil;

/**
 * @author Modifications by Formtek  [Originally from Alfresco file TaskInstancePut.java]
 * @since 4.0 Site Task Manager
 */
public class SiteTaskInstancePut extends AbstractWorkflowWebscript
{
    private static Log logger = LogFactory.getLog(SiteTaskInstancePut.class);

    @Override
    protected Map<String, Object> buildModel(WorkflowModelBuilder modelBuilder, WebScriptRequest req, Status status, Cache cache)
    {
        Map<String, String> params = req.getServiceMatch().getTemplateVars();
        
        // getting task id from request parameters
        String wfId = getWorkflowId(req);
        logger.debug("Processing workflowId: " + wfId);
        String site = getSite(req);
        logger.debug("Workflow should be associated with site: " + site);
        
        logger.debug("Workflow type: " + workflowService.getWorkflowById(wfId).getDefinition().getName());
        
        if (wfId==null || site==null)
        {
            throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND, "Can't set task properties because workflow Id or site name are not defined correctly.");
        }
      
        JSONObject json = null;
                 
        try
        {                          
            // read request json containing properties to write
            json = new JSONObject(new JSONTokener(req.getContent().getContent()));
            logger.debug(json.toString());
            
            // get the workflow path from the incoming workflow Id
            // for site task manager workflows, there will be a single path
            WorkflowPath wfPath = SiteWFUtil.getWorkflowPath(wfId, workflowService);       
            
            //  If User is not authorized to change this workflow, exit
            if(!SiteWFUtil.canUserChangeWorkflow(wfPath, site, workflowService, nodeService, authenticationService, authorityService))
            {
                throw new WebScriptException(HttpServletResponse.SC_UNAUTHORIZED, "User is not authorized to change workflows for this site: " + site);
            }
     
            // Retrieve the current task.  For the site task workfklow, there should be a single current task
            WorkflowTask workflowTask = SiteWFUtil.getWorkflowTask(wfPath, workflowService);         
            String taskId = workflowTask.getId();
            logger.debug("Processing taskId: " + taskId);

            
            // update task properties
            workflowTask = workflowService.updateTask(taskId, parseTaskProperties(json, workflowTask), null, null);
            
            // task was not found -> return 404
            if (workflowTask == null)
            {
                throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND, "Failed to find workflow task with id: " + taskId);
            }
            
            // build the model for ftl
            Map<String, Object> model = new HashMap<String, Object>();
            model.put("workflowTask", modelBuilder.buildDetailed(workflowTask));
            
            return model;
        }
        catch (IOException iox)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Could not read content from request.", iox);
        }
        catch (JSONException je)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Could not parse JSON from request.", je);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<QName, Serializable> parseTaskProperties(JSONObject json, WorkflowTask workflowTask) throws JSONException
    {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        
        // gets the array of properties names
        String[] names = JSONObject.getNames(json);
        
        if (names != null)
        {
            // array is not empty
            for (String name : names)
            {
                // build the qname of property
                QName key = QName.createQName(name.replaceFirst("_", ":"), namespaceService);
                Object jsonValue = json.get(name);
                
                Serializable value = null;
                
                // process null values 
                if (jsonValue.equals(JSONObject.NULL))
                {
                    props.put(key, null);
                }
                else
                {
                    // gets the property definition from dictionary
                    PropertyDefinition prop = dictionaryService.getProperty(key);
                    
                    if (prop != null)
                    {
                        // convert property using its data type specified in model
                        value = (Serializable) DefaultTypeConverter.INSTANCE.convert(prop.getDataType(), jsonValue);
                    }
                    else
                    {
                        // property definition was not found in dictionary
                        if (jsonValue instanceof JSONArray)
                        {
                            value = new ArrayList<String>();
                            
                            for (int i = 0; i < ((JSONArray)jsonValue).length(); i++)
                            {
                                ((List<String>)value).add(((JSONArray)jsonValue).getString(i));
                            }
                        }
                        else
                        {
                            // If the JSON returns an Object which is not a String, we use that type.
                            // Otherwise, we try to convert the string
                            if (jsonValue instanceof String)
                            {
                                logger.debug("Writing value: " + key + " : " + jsonValue);
                                // Check if the task already has the property, use that type.
                                Serializable existingValue = workflowTask.getProperties().get(key);
                                if (existingValue != null)
                                {
                                    try
                                    {
                                        value = DefaultTypeConverter.INSTANCE.convert(existingValue.getClass(), jsonValue);
                                    }
                                    catch(TypeConversionException tce)
                                    {
                                        // TODO: is this the right approach, ignoring exception?
                                        // Ignore the exception, revert to using String-value
                                    }
                                }
                                else
                                {
                                    // Revert to using string-value
                                    value = (String) jsonValue;
                                }
                            }
                            else
                            {
                                // Use the value provided by JSON
                                value = (Serializable) jsonValue;
                            }
                        }
                    }
                }
                
                props.put(key, value);
            }
        }
        return props;
    }
    /**
     * Returns the site
     * @param req
     * @return
     */
    private String getWorkflowId(WebScriptRequest req)
    {
        String wfId = req.getParameter("workflow_instance_id");
        if (wfId == null || wfId.length() == 0)
        {
            wfId = null;
        }
        return wfId;
    }
    
    /**
     * Returns the workflow ID
     * @param req
     * @return
     */
    private String getSite(WebScriptRequest req)
    {
        String site = req.getParameter("site");
        if (site == null || site.length() == 0)
        {
            site = null;
        }
        return site;
    }
    

}