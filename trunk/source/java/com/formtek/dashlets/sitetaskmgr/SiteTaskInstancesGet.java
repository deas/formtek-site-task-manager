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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.util.ModelUtil;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.cmr.workflow.WorkflowTaskQuery;
import org.alfresco.service.cmr.workflow.WorkflowTaskState;
import org.alfresco.service.cmr.workflow.WorkflowTaskQuery.OrderBy;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import org.alfresco.repo.web.scripts.workflow.AbstractWorkflowWebscript;
import org.alfresco.repo.web.scripts.workflow.WorkflowModelBuilder;

import com.formtek.dashlets.sitetaskmgr.SiteTaskManagerWorkflowModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.formtek.dashlets.sitetaskmgr.SiteWFUtil;

/**
 * Webscript impelementation to return workflow task instances.
 *   Executing user must be a member of the site group or administrator
 * 
 * @author Modified by Formtek  [Originally from Alfresco file TaskInstancesGet.java]
 * @author Nick Smith
 * @author Gavin Cornwell
 * @since 4.0 Site Task Manager
 */
public class SiteTaskInstancesGet extends AbstractWorkflowWebscript
{
    private static Log logger = LogFactory.getLog(SiteTaskInstancesGet.class);
    
    public static final String PARAM_AUTHORITY = "authority";
    public static final String PARAM_STATE = "state";
    public static final String PARAM_SITE = "site";           // Make site an available filter
    public static final String PARAM_PRIORITY = "priority";
    public static final String PARAM_DUE_BEFORE = "dueBefore";
    public static final String PARAM_DUE_AFTER = "dueAfter";
    public static final String PARAM_PROPERTIES = "properties";
    public static final String PARAM_TASKTYPE = "taskType";
    public static final String PARAM_POOLED_TASKS = "pooledTasks";
    public static final String TASK_DONE_AND_ASSIGNED = "DoneAndAssigned";
    public static final String TASK_ASSIGNED_MODEL = "stmwf:assignedTask";
    public static final String TASK_DONE_MODEL = "stmwf:doneTask";
    public static final String TASK_ARCHIVED_MODEL = "stmwf:archivedTask";
    public static final String TASK_ASSIGNED = "Assigned";
    public static final String TASK_DONE = "Done";
    public static final String TASK_ARCHIVED = "Archived";
    public static final String VAR_WORKFLOW_INSTANCE_ID = "workflow_instance_id";
    
    public static final String PARAM_SORT = "sort";
    public static final String PARAM_DIRECTION = "dir";
    public static final String PARAM_STARTINDEX = "startIndex";
    public static final String PARAM_COUNT = "results";
    public static final String PARAM_MAXRESULTS = "maxItems";
    public static final String PARAM_SKIP = "skipCount";
    public static final int    DEFAULT_MAX_ITEMS = 1000;
    
    public static final String TASK_IS_SITE_EDITABLE = "isSiteEditable";

    private WorkflowTaskDueAscComparator taskDueTaskComparator = new WorkflowTaskDueAscComparator();
    private WorkflowTaskStartAscComparator taskStartTaskComparator = new WorkflowTaskStartAscComparator();
    private WorkflowTaskStateAscComparator taskStateComparator = new WorkflowTaskStateAscComparator();
    private WorkflowNameAscComparator taskWFNameComparator = new WorkflowNameAscComparator();
    private WorkflowTaskBurdenAscComparator taskWFBurdenComparator = new WorkflowTaskBurdenAscComparator();
    private WorkflowTaskPriorityAscComparator taskWFPriorityComparator = new WorkflowTaskPriorityAscComparator();
    private WorkflowTaskAssigneeAscComparator taskAssigneeComparator = new WorkflowTaskAssigneeAscComparator();
    private WorkflowTaskInitiatorAscComparator taskWFInitiatorComparator = new WorkflowTaskInitiatorAscComparator();
    private WorkflowTaskPercentAscComparator taskWFPercentCompleteComparator = new WorkflowTaskPercentAscComparator();
    private WorkflowCommentAscComparator taskCommentComparator = new WorkflowCommentAscComparator();
    private WorkflowPrevCommentAscComparator taskPrevCommentComparator = new WorkflowPrevCommentAscComparator();
    
    

    @Override
    protected Map<String, Object> buildModel(WorkflowModelBuilder modelBuilder, WebScriptRequest req, Status status, Cache cache)
    {
        Map<String, String> params = req.getServiceMatch().getTemplateVars();
        Map<String, Object> filters = new HashMap<String, Object>(4);
        
        // site is in filters, but we also need to check appropriate privileges to run 
        //    members of the site are given access
        String site = getSite(req);
        logger.debug("Finding tasks for site: " + site);
                
        // authority is not included into filters list as it will be taken into account before filtering
        String authority = getAuthority(req);
        
        // state is also not included into filters list, for the same reason
        WorkflowTaskState state = getState(req);
        
        // look for a workflow instance id
        String workflowInstanceId = params.get(VAR_WORKFLOW_INSTANCE_ID);
        
        // get list of properties to include in the response
        List<String> properties = getProperties(req);
        
        // Get sort parameters, if defined
        String sortColumn = getSort(req);
        String sortDirection = getDir(req);
        int sortStart = getStartIndex(req);
        int sortCount = getResultCount(req);
        logger.debug("Sort column and direction: " + sortColumn + " : " + sortDirection);
        logger.debug("Sort start and count: " + sortStart + " : " + sortCount);
              
        //  If User is not authorized to view this workflow, exit
        if(!SiteWFUtil.canUserViewWorkflow(site, workflowService, nodeService, authenticationService, authorityService))
        {
            throw new WebScriptException(HttpServletResponse.SC_UNAUTHORIZED, "User is not authorized to view workflows for this site: " + site);
        }     

        // get filter param values
        filters.put(PARAM_PRIORITY, req.getParameter(PARAM_PRIORITY));
        filters.put(PARAM_SITE, req.getParameter(PARAM_SITE));  
        filters.put(PARAM_TASKTYPE, req.getParameter(PARAM_TASKTYPE));
        processDateFilter(req, PARAM_DUE_BEFORE, filters);
        processDateFilter(req, PARAM_DUE_AFTER, filters);
        
        String excludeParam = req.getParameter(PARAM_EXCLUDE);
        if (excludeParam != null && excludeParam.length() > 0)
        {
            filters.put(PARAM_EXCLUDE, new ExcludeFilter(excludeParam));
        }
        
        List<WorkflowTask> allTasks;

        if (workflowInstanceId != null)
        {
            // a workflow instance id was provided so query for tasks
            WorkflowTaskQuery taskQuery = new WorkflowTaskQuery();
            taskQuery.setActive(null);
            taskQuery.setProcessId(workflowInstanceId);
            taskQuery.setTaskState(state);
            taskQuery.setOrderBy(new OrderBy[]{OrderBy.TaskDue_Asc});
            
            if (authority != null)
            {
                taskQuery.setActorId(authority);
            }
            
            allTasks = workflowService.queryTasks(taskQuery);
        }
        else
        {
            // default task state to IN_PROGRESS if not supplied
            if (state == null)
            {
                state = WorkflowTaskState.IN_PROGRESS;
            }
            
            // no workflow instance id is present so get all tasks
            if (authority != null)
            {
                List<WorkflowTask> tasks = workflowService.getAssignedTasks(authority, state);

                allTasks = new ArrayList<WorkflowTask>(tasks.size());
                allTasks.addAll(tasks);
                
                // sort tasks by due date
//                Collections.sort(allTasks, taskComparator);
            }
            else
            {
                // authority was not provided -> return all active tasks in the system
                WorkflowTaskQuery taskQuery = new WorkflowTaskQuery();
                taskQuery.setTaskState(state);
                taskQuery.setActive(null);
                List<WorkflowTask>allTasks1 = workflowService.queryTasks(taskQuery);  // unmodifiable -- need better solution
                allTasks = new ArrayList<WorkflowTask>(allTasks1);
            }
            
            // Sort the tasks before filtering
            sortTasks(allTasks, sortColumn, sortDirection);
        }
        
        // filter results
        ArrayList<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        for (WorkflowTask task : allTasks)
        {
            if (matches(task, filters))
            {
                // Add flag to specify editability by user
                Map<String, Object> model = modelBuilder.buildSimple(task, properties);
                // Overwrite the editability value
                model.put(modelBuilder.TASK_IS_EDITABLE, 
                     SiteWFUtil.canUserChangeWorkflow(task.getPath(), site, workflowService, nodeService, 
                         authenticationService, authorityService));
                results.add(model);
            }
        }
        
        // create and return results, paginated if necessary
        return createResultModel1(req, "taskInstances", results);
        
        // Get a subet of the results for pagination
//        if(sortStart>= 0 && sortCount > 0)
//        {
//            if(results.size()-sortStart<sortCount) sortCount = results.size()-sortStart;
//            List<Map<String, Object>> results1 = (List<Map<String, Object>>)results.subList(sortStart, sortCount);
//            
//            return createResultModel(req, "taskInstances", results1);
//        }
//       else
//        {
//            // create and return results, paginated if necessary
//            return createResultModel(req, "taskInstances", results);
//        }

    }
    
    private void sortTasks(List<WorkflowTask> allTasks, String sortColumn, String sortDirection)
    {
        logger.debug("Calling the sort method.");
        if(sortColumn == null || sortDirection == null)
        {
            logger.debug("Standard Sort order invoked");
            logger.debug("Number of tasks: " + allTasks.size());
            Collections.sort(allTasks, taskDueTaskComparator);
        }
        else
        {
            if(sortColumn.equals("state"))
            {
                logger.debug("Sort by state");
                if(sortDirection.equals("asc"))
                {
                    Collections.sort(allTasks, taskStateComparator);
                }
                else
                {
                    Collections.sort(allTasks, Collections.reverseOrder(taskStateComparator));
                }
            }
            else if(sortColumn.equals("priority"))
            {
                logger.debug("Sort by priority");
                if(sortDirection.equals("asc"))
                {
                    Collections.sort(allTasks, taskWFPriorityComparator);
                }
                else
                {
                    Collections.sort(allTasks, Collections.reverseOrder(taskWFPriorityComparator));
                }
            }
            else if(sortColumn.equals("burden"))
            {
                logger.debug("Sort by burden");
                if(sortDirection.equals("asc"))
                {
                    Collections.sort(allTasks, taskWFBurdenComparator);
                }
                else
                {
                    Collections.sort(allTasks, Collections.reverseOrder(taskWFBurdenComparator));
                }
            }
            else if(sortColumn.equals("name"))
            {
                logger.debug("Sort by state");
                if(sortDirection.equals("asc"))
                {
                    Collections.sort(allTasks, taskWFNameComparator);
                }
                else
                {
                    Collections.sort(allTasks, Collections.reverseOrder(taskWFNameComparator));
                }
            }
            else if(sortColumn.equals("assignee"))
            {
                logger.debug("Sort by state");
                if(sortDirection.equals("asc"))
                {
                    Collections.sort(allTasks, taskAssigneeComparator);
                }
                else
                {
                    Collections.sort(allTasks, Collections.reverseOrder(taskAssigneeComparator));
                }
            }
            else if(sortColumn.equals("initiator"))
            {
                logger.debug("Sort by state");
                if(sortDirection.equals("asc"))
                {
                    Collections.sort(allTasks, taskWFInitiatorComparator);
                }
                else
                {
                    Collections.sort(allTasks, Collections.reverseOrder(taskWFInitiatorComparator));
                }
            }
            else if(sortColumn.equals("created"))
            {
                logger.debug("Sort by creation date");
                if(sortDirection.equals("asc"))
                {
                    Collections.sort(allTasks, taskStartTaskComparator);
                }
                else
                {
                    Collections.sort(allTasks, Collections.reverseOrder(taskStartTaskComparator));
                }
            }
            else if(sortColumn.equals("due"))
            {
                logger.debug("Sort by due date");
                if(sortDirection.equals("asc"))
                {
                    Collections.sort(allTasks, taskDueTaskComparator);
                }
                else
                {
                    Collections.sort(allTasks, Collections.reverseOrder(taskDueTaskComparator));
                }
            }
            else if(sortColumn.equals("percent"))
            {
                logger.debug("Sort by percent complete");
                if(sortDirection.equals("asc"))
                {
                    Collections.sort(allTasks, taskWFPercentCompleteComparator);
                }
                else
                {
                    Collections.sort(allTasks, Collections.reverseOrder(taskWFPercentCompleteComparator));
                }
            }
            else if(sortColumn.equals("comment"))
            {
                logger.debug("Sort by comment");
                if(sortDirection.equals("asc"))
                {
                    Collections.sort(allTasks, taskCommentComparator);
                }
                else
                {
                    Collections.sort(allTasks, Collections.reverseOrder(taskCommentComparator));
                }
            }
            else if(sortColumn.equals("prevcomment"))
            {
                logger.debug("Sort by previous comment");
                if(sortDirection.equals("asc"))
                {
                    Collections.sort(allTasks, taskPrevCommentComparator);
                }
                else
                {
                    Collections.sort(allTasks, Collections.reverseOrder(taskPrevCommentComparator));
                }
            }
            else
            {
               // Default sort order
               Collections.sort(allTasks, taskDueTaskComparator);
            }
        }
    }

    /**
     * Retrieves the list of property names to include in the response.
     * 
     * @param req The WebScript request
     * @return List of property names
     */
    private List<String> getProperties(WebScriptRequest req)
    {
        String propertiesStr = req.getParameter(PARAM_PROPERTIES);
        if (propertiesStr != null)
        {
            return Arrays.asList(propertiesStr.split(","));
        }
        return null;
    }

    
    /**
     * Gets the specified {@link WorkflowTaskState}, null if not requested
     * 
     * @param req
     * @return
     */
    private WorkflowTaskState getState(WebScriptRequest req)
    {
        String stateName = req.getParameter(PARAM_STATE);
        if (stateName != null)
        {
            try
            {
                return WorkflowTaskState.valueOf(stateName.toUpperCase());
            }
            catch (IllegalArgumentException e)
            {
                String msg = "Unrecognised State parameter: " + stateName;
                throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST, msg);
            }
        }
        
        return null;
    }

    /**
     * Returns the specified authority. If no authority is specified then returns the current Fully Authenticated user.
     * @param req
     * @return
     */
    private String getAuthority(WebScriptRequest req)
    {
        String authority = req.getParameter(PARAM_AUTHORITY);
        if (authority == null || authority.length() == 0)
        {
            authority = null;
        }
        return authority;
    }
    
    /**
     * Returns the site
     * @param req
     * @return
     */
    private String getSite(WebScriptRequest req)
    {
        String site = req.getParameter(PARAM_SITE);
        if (site == null || site.length() == 0)
        {
            site = null;
        }
        return site;
    }
    
    /**
     * Returns the sort column
     * @param req
     * @return
     */
    private String getSort(WebScriptRequest req)
    {
        String sort = req.getParameter(PARAM_SORT);
        if (sort == null || sort.length() == 0)
        {
            sort = null;
        }
        return sort;
    }
    
    /**
     * Returns the sort direction
     * @param req
     * @return
     */
    private String getDir(WebScriptRequest req)
    {
        String dir = req.getParameter(PARAM_DIRECTION);
        if (dir == null || dir.length() == 0)
        {
            dir = null;
        }
        return dir;
    }
    
    /**
     * Returns the start index to return results
     * @param req
     * @return
     */
    private int getStartIndex(WebScriptRequest req)
    {
        String start = req.getParameter(PARAM_STARTINDEX);
        int iStart = -1;
        if (start == null || start.length() == 0)
        {
            start = req.getParameter(PARAM_SKIP);
        }
        if (start != null && start.length() > 0)
        {
            try
            {
              iStart = Integer.parseInt(start.trim());
            }
            catch(Exception ex){}
        }
        return iStart;

    }
    
    /**
     * Returns the number of results to return
     * @param req
     * @return
     */
    private int getResultCount(WebScriptRequest req)
    {
        String count = req.getParameter(PARAM_COUNT);
        int iCount = -1;
        logger.debug("Reading result count: " + count);
        if(count==null || count.length() == 0)
        {
            count = req.getParameter(PARAM_MAXRESULTS);
        }
        if (count != null && count.length() > 0)
        {
           try
            {
              iCount = Integer.parseInt(count.trim());
            }
            catch(Exception ex){}
        }
        return iCount;
    }

    /**
     * Determine if the given task should be included in the response.
     * 
     * @param task The task to check
     * @param filters The list of filters the task must match to be included
     * @return true if the task matches and should therefore be returned
     */
    private boolean matches(WorkflowTask task, Map<String, Object> filters)
    {
        // by default we assume that workflow task should be included
        boolean result = true;

        for (String key : filters.keySet())
        {
            Object filterValue = filters.get(key);

            // skip null filters (null value means that filter was not specified)
            if (filterValue != null)
            {
                if (key.equals(PARAM_EXCLUDE))
                {
                    ExcludeFilter excludeFilter = (ExcludeFilter)filterValue;
                    String type = task.getDefinition().getMetadata().getName().toPrefixString(this.namespaceService);
                    if (excludeFilter.isMatch(type))
                    {
                        result = false;
                        break;
                    }
                }
                else if (key.equals(PARAM_DUE_BEFORE))
                {
                    Date dueDate = (Date)task.getProperties().get(WorkflowModel.PROP_DUE_DATE);

                    if (!isDateMatchForFilter(dueDate, filterValue, true))
                    {
                        result = false;
                        break;
                    }
                }
                else if (key.equals(PARAM_DUE_AFTER))
                {
                    Date dueDate = (Date)task.getProperties().get(WorkflowModel.PROP_DUE_DATE);

                    if (!isDateMatchForFilter(dueDate, filterValue, false))
                    {
                        result = false;
                        break;
                    }
                }
                else if (key.equals(PARAM_PRIORITY))
                {
                    if (!filterValue.equals(task.getProperties().get(WorkflowModel.PROP_PRIORITY).toString()))
                    {
                        result = false;
                        break;
                    }
                }
                else if (key.equals(PARAM_TASKTYPE))
                {
                    logger.debug("Filter value: " + filterValue);
                    logger.debug("Checking Task with name: " + task.getName());
                    if(filterValue.equals(TASK_DONE_AND_ASSIGNED))
                    {
                        if(task.getName().equals(TASK_ASSIGNED_MODEL) || task.getName().equals(TASK_DONE_MODEL))
                        {
                            result = true;
                        }
                    }
                    else if (filterValue.equals(TASK_DONE) && task.getName().equals(TASK_DONE_MODEL) )
                    {
                        result = true;
                    }
                    else if (filterValue.equals(TASK_ASSIGNED) && task.getName().equals(TASK_ASSIGNED_MODEL) )
                    {
                        result = true;
                    }
                    else if (filterValue.equals(TASK_ARCHIVED) && task.getName().equals(TASK_ARCHIVED_MODEL) )
                    {
                        result = true;
                    }
                    else
                    {
                        result = false;
                        break;
                    }
                }
                // Make site an available filter
                else if (key.equals(PARAM_SITE))
                {
                    logger.debug("Evaluating Filter PARAM_SITENAME: " + filterValue);  
                    
                    // The start task for the workflow marks the workflow
                    WorkflowTask startTask = SiteWFUtil.getStartWorkflowTaskFromCurrent(task, workflowService);
                    if (startTask.getProperties().get(SiteTaskManagerWorkflowModel.PROP_SITENAME) == null)
                    {
                        logger.debug("No sitename property for task");
                        result = false;
                        break;                    
                    }
                    else if(!filterValue.equals(startTask.getProperties().get(SiteTaskManagerWorkflowModel.PROP_SITENAME).toString()))
                    {
                        logger.debug("Sitename does not match: " + startTask.getProperties().get(SiteTaskManagerWorkflowModel.PROP_SITENAME).toString());
                        result = false;
                        break;
                    }
                    else
                    {
                        logger.debug("Matching sitename is: " + startTask.getProperties().get(SiteTaskManagerWorkflowModel.PROP_SITENAME).toString()); 
                    }
                }
            }
        }

        return result;
    }
    
    /**
     * Comparator to sort workflow tasks by due date in ascending order.
     */
    class WorkflowTaskDueAscComparator implements Comparator<WorkflowTask>
    {
        @Override
        public int compare(WorkflowTask o1, WorkflowTask o2)
        {
            Date date1 = (Date)o1.getProperties().get(WorkflowModel.PROP_DUE_DATE);
            Date date2 = (Date)o2.getProperties().get(WorkflowModel.PROP_DUE_DATE);
            
            long time1 = date1 == null ? Long.MAX_VALUE : date1.getTime();
            long time2 = date2 == null ? Long.MAX_VALUE : date2.getTime();
            
            long result = time1 - time2;
            
            return (result > 0) ? 1 : (result < 0 ? -1 : 0);
        }
        
    }
    /**
     * Comparator to sort workflow tasks by start date in ascending order.
     */
    class WorkflowTaskStartAscComparator implements Comparator<WorkflowTask>
    {
        @Override
        public int compare(WorkflowTask o1, WorkflowTask o2)
        {
            Date date1 = (Date)o1.getProperties().get(WorkflowModel.PROP_START_DATE);
            Date date2 = (Date)o2.getProperties().get(WorkflowModel.PROP_START_DATE);
            
            long time1 = date1 == null ? Long.MAX_VALUE : date1.getTime();
            long time2 = date2 == null ? Long.MAX_VALUE : date2.getTime();
            
            long result = time1 - time2;
            
            return (result > 0) ? 1 : (result < 0 ? -1 : 0);
        }
        
    }
    
    /**
     * Comparator to sort workflow tasks by task type/state in ascending order.
     */
   class WorkflowTaskStateAscComparator implements Comparator<WorkflowTask>
   {
        @Override
      public int compare(WorkflowTask o1, WorkflowTask o2)
      {
         
         String taskID1 = o1.getDefinition().getId();
         String taskID2 = o2.getDefinition().getId();
         logger.debug("comparing: " + o1.getId() + " to " + o2.getId());
         logger.debug("definition: " + taskID1 + " to " + taskID2);
                  
         String status1 = (String)o1.getProperties().get(WorkflowModel.PROP_STATUS);
         String status2 = (String)o2.getProperties().get(WorkflowModel.PROP_STATUS);
         
         logger.debug("status: " + status1 + " to " + status2);

         Date date1 = (Date)o1.getProperties().get(WorkflowModel.PROP_DUE_DATE);
         Date date2 = (Date)o2.getProperties().get(WorkflowModel.PROP_DUE_DATE);  
         
         logger.debug("Dates: " + date1 + " : " + date2);       
         
         int v1 = assignTaskValue(taskID1, status1, date1);
         int v2 = assignTaskValue(taskID2, status2, date2);
         logger.debug("v1: " + v1 + " : " + "v2: " + v2);
         
         int val = 0;
         if (v2 > v1) val = 1;
         if (v1 > v2) val = -1;
         logger.debug("Returning: " + val);
         return val;
      }
      
      private int assignTaskValue (String id, String status, Date d)
      {
         logger.debug("AssignTaskValue:  " + id + " : " + status + " : " +d);
         //  Valid Task id and combinations are:
         //      stmwf:assignedTask
         //      stmwf:assignedTask + Not Yet Started
         //      stmwf:assignedTask + In Progress
         //      stmwf:assignedTask + Past Due Date
         //      stmwf:assignedTask + Completed
         //      stmwf:assignedTask + On Hold
         //      stmwf:assignedTask + Cancelled
         //      stmwf:doneTask
         //      stmwf:archivedTask
         if(id.equals("stmwf:assignedTask"))
         {
             Date today = new Date();
             if(d!=null && today.after(d)) return 3;
             if(status.equals("Not Yet Started")) return 1;
             if(status.equals("In Progress")) return 2;
             if(status.equals("Completed")) return 6;
             if(status.equals("On Hold")) return 7;
             if(status.equals("Cancelled")) return 8;
             return 1;
         }
         else if(id.equals("stmwf:doneTask"))
         {
             return 4;
         }
         else if(id.equals("stmwf:archivedTask"))
         {
             return 5;
         }
         else
         {
             return 999;
         }
      }
   }
   
     /**
     * Comparator to sort workflow tasks by task percent completed in ascending order.
     */
   class WorkflowTaskPercentAscComparator implements Comparator<WorkflowTask>
   {
        @Override
      public int compare(WorkflowTask o1, WorkflowTask o2)
      {
         Integer percent1 = (Integer)o1.getProperties().get(WorkflowModel.PROP_PERCENT_COMPLETE);
         Integer percent2 = (Integer)o2.getProperties().get(WorkflowModel.PROP_PERCENT_COMPLETE);
         
         int result = percent1 - percent2;        
         return (result > 0) ? 1 : (result < 0 ? -1 : 0);
      }

   }
   
    /**
     * Comparator to sort workflow tasks by task priority in ascending order.
     */
   class WorkflowTaskPriorityAscComparator implements Comparator<WorkflowTask>
   {
        @Override
      public int compare(WorkflowTask o1, WorkflowTask o2)
      {
         Integer priority1 = (Integer)o1.getProperties().get(WorkflowModel.PROP_PRIORITY);
         Integer priority2 = (Integer)o2.getProperties().get(WorkflowModel.PROP_PRIORITY);
         
         Integer result = priority1 - priority2;        
         return (result > 0) ? 1 : (result < 0 ? -1 : 0);
      }

   }
   
   
    /**
     * Comparator to sort workflow tasks by assignee in ascending order.
     */
   class WorkflowTaskAssigneeAscComparator implements Comparator<WorkflowTask>
   {
        @Override
      public int compare(WorkflowTask o1, WorkflowTask o2)
      {
         String assignee1 = (String)o1.getProperties().get(ContentModel.PROP_OWNER);
         String assignee2 = (String)o2.getProperties().get(ContentModel.PROP_OWNER);
         
         if (assignee1 == null && assignee2 == null) return 0;
         if (assignee1 == null) return -1;
         if (assignee2 == null) return 1;
         logger.debug("comparing: " + assignee1 + " to " + assignee2);
         
         return (assignee1).compareTo(assignee2);
      }

   }
   
   
    /**
     * Comparator to sort workflow tasks by initiator in ascending order.
     */
   class WorkflowTaskInitiatorAscComparator implements Comparator<WorkflowTask>
   {
        @Override
      public int compare(WorkflowTask o1, WorkflowTask o2)
      {
         String initiator1 = SiteWFUtil.getWorkflowInitiatorUsername(o1.getPath(), nodeService);
         String initiator2 = SiteWFUtil.getWorkflowInitiatorUsername(o2.getPath(), nodeService);
         
         if (initiator1 == null && initiator2 == null) return 0;
         if (initiator1 == null) return -1;
         if (initiator2 == null) return 1;
         logger.debug("comparing: " + initiator1 + " to " + initiator2);
         
         return (initiator1).compareTo(initiator2);
      }

   }
   
    /**
     * Comparator to sort workflow tasks by task burden in ascending order.
     */
   class WorkflowTaskBurdenAscComparator implements Comparator<WorkflowTask>
   {
        @Override
      public int compare(WorkflowTask o1, WorkflowTask o2)
      {
         String burden1 = (String)o1.getProperties().get(SiteTaskManagerWorkflowModel.PROP_BURDEN);
         String burden2 = (String)o2.getProperties().get(SiteTaskManagerWorkflowModel.PROP_BURDEN);
         
         if (burden1 == null && burden2 == null) return 0;
         if (burden1 == null) return -1;
         if (burden2 == null) return 1;
         logger.debug("comparing: " + burden1 + " to " + burden2);
         
         int result = assignBurdenValue(burden1) - assignBurdenValue(burden2);     
         return (result > 0) ? 1 : (result < 0 ? -1 : 0);
      }
      
      private int assignBurdenValue (String burden)
      {
         //  Valid Burdens:
         //      Big
         //      Medium
         //      Low
         if(burden.equals("Big")) return 3;
         else if(burden.equals("Medium")) return 2;
         else if(burden.equals("Small")) return 1;
         else return 0;
      }
   }
   
    /**
     * Comparator to sort workflow tasks by task name in ascending order.
     */
   class WorkflowNameAscComparator implements Comparator<WorkflowTask>
   {
        @Override
      public int compare(WorkflowTask o1, WorkflowTask o2)
      {
         String wfName1 = (String)o1.getProperties().get(SiteTaskManagerWorkflowModel.PROP_WFNAME);
         String wfName2 = (String)o2.getProperties().get(SiteTaskManagerWorkflowModel.PROP_WFNAME);
         
         logger.debug("comparing: " + wfName1 + " to " + wfName2);
         
         if (wfName1 == null && wfName2 == null) return 0;
         if (wfName1 == null) return -1;
         if (wfName2 == null) return 1;
         return (wfName1).compareTo(wfName2);
      }
   }

     /**
     * Comparator to sort workflow tasks by comment in ascending order.
     */
   class WorkflowCommentAscComparator implements Comparator<WorkflowTask>
   {
        @Override
      public int compare(WorkflowTask o1, WorkflowTask o2)
      {
         String comment1 = (String)o1.getProperties().get(WorkflowModel.PROP_COMMENT);
         String comment2 = (String)o2.getProperties().get(WorkflowModel.PROP_COMMENT);
         
         logger.debug("comparing: " + comment1 + " to " + comment2);
         
         if (comment1 == null && comment2 == null) return 0;
         if (comment1 == null) return -1;
         if (comment2 == null) return 1;
         return (comment1).compareTo(comment2);
      }
   }  
   
     /**
     * Comparator to sort workflow tasks by the 'previous comment' in ascending order.
     */
   class WorkflowPrevCommentAscComparator implements Comparator<WorkflowTask>
   {
        @Override
      public int compare(WorkflowTask o1, WorkflowTask o2)
      {
         String comment1 = (String)o1.getProperties().get(SiteTaskManagerWorkflowModel.PROP_PREVIOUSCOMMENT);
         String comment2 = (String)o2.getProperties().get(SiteTaskManagerWorkflowModel.PROP_PREVIOUSCOMMENT);
         
         logger.debug("comparing: " + comment1 + " to " + comment2);
         
         if (comment1 == null && comment2 == null) return 0;
         if (comment1 == null) return -1;
         if (comment2 == null) return 1;
         return (comment1).compareTo(comment2);
      }
   }  
   
    protected Map<String, Object> createResultModel1(WebScriptRequest req, String dataPropertyName, 
                List<Map<String, Object>> results)
    {
        int totalItems = results.size();
        int maxItems = getResultCount(req);
        int skipCount = getStartIndex(req);
        
        Map<String, Object> model = new HashMap<String, Object>();
        model.put(dataPropertyName, applyPagination(results, maxItems, skipCount));
        
        // maxItems or skipCount parameter was provided so we need to include paging into response
        model.put("paging", ModelUtil.buildPaging(totalItems, maxItems == DEFAULT_MAX_ITEMS ? totalItems : maxItems, skipCount));
        
        return model;
    }

}
