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

/**
 * Formtek Site Task Manager site Dashlet.
 *
 * @namespace Formtek.dashlet
 * @class Formtek.dashlet.SiteTaskMgr
 */

(function()
{
   /**
    * Define YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
      Event = YAHOO.util.Event,
      Selector = YAHOO.util.Selector;

   /**
    * Define Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML,
       $siteURL = Alfresco.util.siteURL;
   /**
    * Preferences -- unique for each site
    */
   var PREFERENCES_TASKS_DASHLET_FILTER = "com.formtek.sitetaskmgr.preferences." + Alfresco.constants.SITE + ".filter";
   var PREFERENCES_TASKS_DASHLET_COLSTATE = "com.formtek.sitetaskmgr.preferences." + Alfresco.constants.SITE + ".colstate";

   /**
    * Dashboard Formtek Site Task Manager constructor.
    *
    * @param {String} htmlId The HTML id of the parent element
    * @return {Formtek.dashlet.SiteTaskMgr} The new component instance
    * @constructor
    */
   Formtek.dashlet.SiteTaskMgr = function SiteTaskMgr_constructor(htmlId)
   {
      Formtek.dashlet.SiteTaskMgr.superclass.constructor.call(this, 
         "Formtek.dashlet.SiteTaskMgr", htmlId, ["button", "container", "datasource", "datatable", "paginator", "history", "animation", "dragdrop", "event"]);

      // Services
      this.services.preferences = new Alfresco.service.Preferences();
      return this;
   };

   /**
    * Extend from Alfresco.component.Base
    */
   YAHOO.extend(Formtek.dashlet.SiteTaskMgr, Alfresco.component.Base);

   /**
    * Augment prototype with Common Workflow actions to reuse createFilterURLParameters
    */
   YAHOO.lang.augmentProto(Formtek.dashlet.SiteTaskMgr, Alfresco.action.WorkflowActions);

   /**
    * Augment prototype with main class implementation, ensuring overwrite is enabled
    */
   YAHOO.lang.augmentObject(Formtek.dashlet.SiteTaskMgr.prototype,
   {
      /**
       * Object container for initialization options
       *
       * @property options
       * @type object
       */
      options:
      {
         /**
          * The Activiti Workflow Id
          *
          * @property workflowid
          * @type string
          * @default 'FTKSiteTaskManager'
          */
         workflowid: 'FTKSiteTaskManager',
         
         /**
          * Maximum number of tasks per page to display in the dashlet.
          *
          * @property maxPageItems
          * @type int
          * @default 50
          */
         maxPageItems: 50,

         /**
          * Filter look-up: type to display value and query value
          *
          * @property filters
          * @type Object
          */
         filters: {},
         
         /**
          * State of the columns.  The ordering and width
          *
          * @property filters
          * @type List
          */
         colState: []
      },

      /**
       * Fired by YUI when parent element is available for scripting
       * @method onReady
       */
      onReady: function SiteTaskMgr_onReady()
      {       
         // Task Display
         //     Create filter menu
         this.widgets.filterMenuButton = Alfresco.util.createYUIButton(this, "filters", this.onFilterSelected,
         {
            type: "menu",
            menu: "filters-menu",
            lazyloadmenu: false
         });

         //     Load column state preferences
         this.services.preferences.request(PREFERENCES_TASKS_DASHLET_COLSTATE,
         {
            successCallback:
            {
               fn: function(p_response)
               {
                     // Extract the column state preferences
                     //   Alfresco preferences failed when saving an array.
                     //   Store data as a string and recover with eval
                     if(typeof p_response.json.com != "undefined")
                     {
                         try
                         {
                             var colPref = p_response.json.com.formtek.sitetaskmgr.preferences[Alfresco.constants.SITE].colstate;
                             this.options.colState = eval('(' + colPref + ')');
                         }
                         catch(ex){}
                     }
                     
                     //     Load filter preferences
                     this.services.preferences.request(PREFERENCES_TASKS_DASHLET_FILTER,
                     {
                        successCallback:
                        {
                           fn: this.onPreferencesLoaded,
                           scope: this
                        }
                     });
               },
               scope: this
            }
         });
             
         // Initialize the no items message
         this.renderNoItemsMessage();
             
         // Make sure we listen for events when the user selects a person
         YAHOO.Bubbling.on("personSelected", this.onPersonSelected, this);
      },
      
      /**
       * Process response from preference query
       *
       * @method onPreferencesLoaded
       * @param p_response {object} Response from "api/people/{userId}/preferences" query
       */
      onPreferencesLoaded: function SiteTaskMgr_onPreferencesLoaded(p_response)
      {
         // Select the preferred filter in the ui
         var filter = Alfresco.util.findValueByDotNotation(p_response.json, PREFERENCES_TASKS_DASHLET_FILTER, "allTasks");
         filter = this.options.filters.hasOwnProperty(filter) ? filter : "allTasks";
         this.widgets.filterMenuButton.set("label", this.msg("filter." + filter));
         this.widgets.filterMenuButton.value = filter;

         // Display the toolbar now that we have selected the filter
         Dom.removeClass(Selector.query(".toolbar div", this.id, true), "hidden");

         // Prepare webscript url to task instances
         var webscript = YAHOO.lang.substitute("api/formtek/dashlets/sitemgr/site-task-instances?site={site}&properties={properties}&exclude={exclude}&",
         {
            properties: ["bpm_priority", "bpm_status", "bpm_dueDate", "bpm_description", "bpm_comment",
                         "stmwf_burden", "bpm_percentComplete", "bpm_assignee", "stmwf_previousComment"].join(","),
            exclude: '',
            site: Alfresco.constants.SITE
         });
         
         // Add a listener for the newTask event
         var newTaskLink = Dom.get(this.id + "-newtask");
         if (newTaskLink)
         {
           Event.addListener(newTaskLink, "click", function (event, obj)
           {
              Formtek.module.getFormDialogInstance().show(
              {
                 itemId: 'activiti%24' + this.options.workflowid,
                 dialogTitle: this.msg("createdialog.title")
              });
              
              // Listen to know when the popup form has finished rendering
              YAHOO.Bubbling.on("formDialogFormAvailable", this.onNewTaskFormRendered, this);
      
              // Listen for when the popup form dialog closes to know when to refresh
              YAHOO.Bubbling.on("formDialogSuccessClose", this.onNewTaskFormClose, this);

           },
           {
              newTaskLink: newTaskLink
           }, this);
         }
         
         var colDefsDefault = 
         [
              { key: "state", resizeable:true, sortable: true, formatter: this.bind(this.renderCellState), className: 'col-align-center', label: this.msg("label.datatable.state")},
              { key: "priority", resizeable:true, sortable: true, formatter: this.bind(this.renderCellPriority), className: 'col-align-center', label: this.msg("label.datatable.priority") },
              { key: "burden", resizeable:true, sortable: true, formatter: this.bind(this.renderCellBurden), className: 'col-align-center', label: this.msg("label.datatable.burden") },
              { key: "name", resizeable:true, sortable: true, formatter: this.bind(this.renderCellTaskName), className: 'col-align-left', width: 200, label: this.msg("label.datatable.name") },
              { key: "comment", resizeable:true, sortable: true, formatter: this.bind(this.renderCellComment), className: 'col-align-left', width: 200, label: this.msg("label.datatable.comment") },
              { key: "prevcomment", hidden: true, resizeable:true, sortable: true, formatter: this.bind(this.renderCellPrevComment), className: 'col-align-left', width: 200, label: this.msg("label.datatable.prevcomment") },
              { key: "assignee", resizeable:true, sortable: true, formatter: this.bind(this.renderCellAssignee), className: 'col-align-left', label: this.msg("label.datatable.assignee")},
              { key: "initiator", resizeable:true, sortable: true, formatter: this.bind(this.renderCellInitiator), className: 'col-align-left', label: this.msg("label.datatable.initiator") },
              { key: "percent", resizeable:true, sortable: true, formatter: this.bind(this.renderCellPercent), className: 'col-align-right', label: this.msg("label.datatable.percent") },
              { key: "created", resizeable:true, sortable: true, formatter: this.bind(this.renderCellCreated), className: 'col-align-left', width: 110, label: this.msg("label.datatable.created") },
              { key: "due", resizeable:true, sortable: true, formatter: this.bind(this.renderCellDueDate), className: 'col-align-left', width: 110, label: this.msg("label.datatable.due") },
              { key: "actions", resizeable:true, sortable: false, formatter: this.bind(this.renderCellActions), className: 'col-align-left', width: 125, label: this.msg("label.datatable.actions") }
           ];
           
           // Reorder and resize the columns, based on the user preferences for the datatable on this site
           var colDefs = [];  colDefs.length = colDefsDefault.length;
           if(this.options.colState!=null && this.options.colState.length>0)
           {
               for(var mm=0; mm<this.options.colState.length; mm++)
               {
                   for(var nn=0; nn<this.options.colState.length; nn++)
                   {
                       if(colDefsDefault[nn].key == this.options.colState[mm].key)
                       {
                          colDefs[mm] = colDefsDefault[nn];
                          if(!isNaN(this.options.colState[mm].width)) colDefs[mm].width = this.options.colState[mm].width;
                          if(this.options.colState[mm].hidden=="true")
                          {
                              colDefs[mm].hidden = true;
                          }
                          else
                          {
                              colDefs[mm].hidden = false;
                          }
                       }
                   }
               }
           }
           else
           {
               colDefs = colDefsDefault;
           }
           
           var orig_formatters = [];  orig_formatters.length = colDefs.length;
           for(var i=0; i<colDefs.length; i++) orig_formatters[i] = colDefs[i].formatter;
         

         /**
          * Create datatable with a simple pagination that only displays number of results.
          */
         this.widgets.alfrescoDataTable = new Alfresco.util.DataTable(
         {
            dataSource:
            {
               url: Alfresco.constants.PROXY_URI + webscript,
               initialParameters: this.substituteParameters(this.options.filters[filter]) || ""
            },
            dataTable:
            {
               container: this.id + "-tasks",
               columnDefinitions: colDefs,
               config:
               {
                  MSG_EMPTY: this.msg("message.noTasks"),
                  draggableColumns: true  
               }
            },
            paginator:
            {
               history: false,
               hide: false,
               config:
               {
                  containers: [this.id + "-paginator"],
                  rowsPerPage: this.options.maxPageItems
               }               
            }
         });

         // Override DataTable function to set custom empty message
         var me = this,
            dataTable = this.widgets.alfrescoDataTable.getDataTable(),
            original_doBeforeLoadData = dataTable.doBeforeLoadData,
            columnDefs = this.widgets.alfrescoDataTable.config.dataTable.columnDefinitions,
            aDefinitions = dataTable._oColumnSet._aDefinitions,
            flat = dataTable._oColumnSet.flat;
            
            
         // Remove some restrictions on datatables imposed by Alfresco CSS
         YAHOO.util.Dom.replaceClass(this.id + '-tasks', 'alfresco-datatable', 'sitetaskmgr-datatable');   
         
         // Alfresco changes around the formatters for some reason, but this breaks reorder, hide and resize
         //  Put them back to their original functions     
         orig_formatters = this.widgets.alfrescoDataTable.formatters;
         for (var i = 0, il = columnDefs.length; i <il; i++)
         {
             columnDefs[i].formatter = orig_formatters[i];
             aDefinitions[i].formatter = orig_formatters[i];
             flat[i].formatter = orig_formatters[i];
         }
         
         dataTable.subscribe("columnReorderEvent", function(a1, a2)
         {
            // Recalcualte and then Save preferences
            this._saveDataTableState();
            this.services.preferences.set(PREFERENCES_TASKS_DASHLET_COLSTATE, YAHOO.lang.JSON.stringify(this.options.colState));
            this._refreshTaskWindow();
         }, this, true);   
         
         dataTable.subscribe("columnResizeEvent", function(a1, a2)
         {
            // Recalcualte and then Save preferences
            this._saveDataTableState();
            this.services.preferences.set(PREFERENCES_TASKS_DASHLET_COLSTATE, YAHOO.lang.JSON.stringify(this.options.colState));
            this._refreshTaskWindow();
         }, this, true); 
         
         dataTable.subscribe("columnSortEvent", function(a1, a2)
         {
         }, this, true);

         dataTable.doBeforeLoadData = function SiteTaskMgr_doBeforeLoadData(sRequest, oResponse, oPayload)
         {
            var id = this._elContainer.id.replace("-tasks","");
            var noItems = Dom.get(id + "-noitems");
            var itemList = Dom.get(id + "-taskgroup");
            
            if(oResponse.results.length === 0)
            {
               Dom.setStyle(itemList, "display", "none");
               Dom.setStyle(noItems, "display", "block");
            }
            else
            {
               Dom.setStyle(itemList, "display", "block");
               Dom.setStyle(noItems, "display", "none");
               
               // Fix bug for YUI column resize.  Resizers get set to 0 height
               var resizers = Dom.getElementsByClassName("yui-dt-resizer");
               for(var kk=0; kk<resizers.length; kk++)
               {
                   resizers[kk].style.height = "23px";
               }
            }

            return original_doBeforeLoadData.apply(this, arguments);
         };

         
         // Capture click events to be able to process actions
         dataTable.subscribe("cellClickEvent", function(e, o) {
            var target = e.target,
            record = o.getRecord(target),
            column = o.getColumn(target);  
            
            // Process only clicks in an action cell          
            if(column.field=="actions")
            {
                // Find which Workflow task action is selected
                var data = record._oData;
                var tar = e.event.target || e.event.srcElement;
                var action = tar.className;
                var wfId = data.workflowInstance.id;
                
                // Current settings for priority, percent and burden
                var priority = data.properties.bpm_priority;
                var percent = data.properties.bpm_percentComplete;
                var burden = data.properties.stmwf_burden;
                
                var curTaskState = 
                {
                    wfId: wfId,
                    bpm_priority: priority + "",
                    bpm_percentComplete: percent + "",
                    stmwf_burden: burden
                }
                if(action=="cancelbutton")
                {
                    var workflow = data.workflowInstance;                
                    this._cancelSiteWorkflowConfirm(wfId, workflow.message);
                }
                else if(action=="editbutton")
                {
                    var taskId = data.id;
                    this._showEditTaskDialog(taskId, wfId);
                }
                else if(action=="reassignbutton")
                {
                    var reassign = Alfresco.util.ComponentManager.get(this.id + "-reassignPanel");
                    var reassignPanel = reassign.widgets.reassignPanel;
                    var finderDiv = Alfresco.util.ComponentManager.get(this.id + "-peopleFinder");
                    reassign.taskId = data.id;
                    reassign.wfId = wfId;
                    
                    finderDiv.clearResults();
                    reassignPanel.show();
                    
                    var textSearch = Dom.get(finderDiv.id + "-search-text");
                    textSearch.focus();
                }
                else if(action=="todone_forwardbutton")
                {
                    var taskId = data.id;
                    curTaskState["bpm_percentComplete"] = "100";
                    this._transitionWorkflow(wfId, taskId, "Task Completed", curTaskState);
                }
                else if(action=="toarchive_forwardbutton")
                {
                    var taskId = data.id;
                    curTaskState["bpm_percentComplete"] = "100";
                    this._transitionWorkflow(wfId, taskId, "Archive", curTaskState);
                }
                else if(action=="toassigned_backbutton")
                {
                    var taskId = data.id;
                    this._transitionWorkflow(wfId, taskId, "Return to Assignee", curTaskState);
                }

                
            }
         }, dataTable, me);
      },
      
      _saveDataTableState: function SiteTaskMgr_saveDataTableState()
      {
          // Get the current state of the datatable columns
          var cols = this.widgets.alfrescoDataTable.getDataTable().getColumnSet().getDefinitions();
          var colStateTemp = [];  colStateTemp.length = cols.length;
          for(var k=0; k<cols.length; k++)
          {
              colStateTemp[k] = {};
              colStateTemp[k].key = cols[k].key;
              if(cols[k].width!=null) 
              {
                  colStateTemp[k].width = cols[k].width;
              }
              else
              {
                  colStateTemp[k].width = "null";
              }
              if(cols[k].hidden!=null && cols[k].hidden==true) 
              {
                  colStateTemp[k].hidden = "true";
              }
              else
              {
                  colStateTemp[k].hidden = "false";
              }
          }
          // Keep the local cache for the ordering
          this.options.colState = colStateTemp;
      },
      
      /**
       * Called on reassignment of task
       *
       * @method onPersonSelected
       * @param e DomEvent
       * @param args Event parameters (depends on event type)
       */
      onPersonSelected: function SiteTaskManager_onPersonSelected(e, args)
      {
         var reassign = Alfresco.util.ComponentManager.get(this.id + "-reassignPanel");
         var reassignPanel = reassign.widgets.reassignPanel;
         var finderDiv = Alfresco.util.ComponentManager.get(this.id + "-peopleFinder");      
                  
         // This is a "global" event check that event is ours
         if (Alfresco.util.hasEventInterest(finderDiv, args))
         {
            var wfId = reassign.wfId;
            reassignPanel.hide();
            
            // Set the burden in the new task
            var sFunc = function (res)
            {            
                // Refresh the dashlet area after the burden is set
                this._refreshTaskWindow();
            }
            this._setTaskPropertyWithSuccess(wfId, "cm_owner", args[1].userName, sFunc, {});
         }
      },
      
      /**
       * Closes the new task dialog popup
       *
       * @method onNewTaskFormClose
       * @param e {object} The event
       * @param args {array} Event arguments
       */      
      onNewTaskFormClose: function SiteTaskMgr_onNewTaskFormClose(e, args)
      {
          var dialogPopup = Alfresco.util.ComponentManager.get('alfresco-FormDialog-instance');
          if(!('showConfig' in dialogPopup) || dialogPopup.showConfig.itemKind!="workflow") return;
          
          YAHOO.Bubbling.unsubscribe("formDialogFormAvailable", this.onNewTaskFormRendered);
          YAHOO.Bubbling.unsubscribe("formDialogSuccessClose", this.onNewTaskFormClose);

          // On successfully initiating a new workflow, set the value for burden in the new task
          if('response' in args[1])
          {
              var response = args[1].response.json.persistedObject; // WorkflowInstance[id=activiti$7351
              // Get the new workflow instance ID
              var vals = response.match(/WorkflowInstance\[id\=([^,]*),/);    // This may be a problem in IE7??         
              var burdenSel = Dom.getElementBy ( function (el){return el.name=='prop_stmwf_burden';}, 'select' );
              var burden = burdenSel.options[ burdenSel.selectedIndex ].value;
          
              // Set the burden in the new task
              var sFunc = function (res)
              {            
                  // Refresh the dashlet area after the burden is set
                  this._refreshTaskWindow();
              }
              this._setTaskPropertyWithSuccess(vals[1], "stmwf_burden", burden, sFunc, {});
          }
          else
          {                 
              // Simple refresh of the dashlet area
              this._refreshTaskWindow();
          }

          // Close the popup window
          dialogPopup.onCancelButtonClick();
      },
      
      /**
       * Cancel the new task dialog popup
       *
       * @method onNewTaskFormCancel
       * @param e {object} The event
       * @param args {array} Event arguments
       */      
      onNewTaskFormCancel: function SiteTaskMgr_onNewTaskFormCancel(e, args)
      {  
          var dialogPopup = Alfresco.util.ComponentManager.get('alfresco-FormDialog-instance');;
          if(!('showConfig' in dialogPopup) || dialogPopup.showConfig.itemKind!="workflow") return;
          
          YAHOO.Bubbling.unsubscribe("formDialogFormAvailable", this.onNewTaskFormRendered);
          YAHOO.Bubbling.unsubscribe("formDialogSuccessClose", this.onNewTaskFormClose);

          // Close the popup window
          dialogPopup.onCancelButtonClick();
      },
      
      /**
       * Do additional processing once the Task/Workflow form is rendered to alter behavior
       *
       * @method onNewTaskFormRendered
       * @param e {object} The event
       * @param args {array} Event arguments
       */      
      onNewTaskFormRendered: function SiteTaskMgr_onNewTaskFormRendered(e, args)
      {
          var popupForm = Alfresco.util.ComponentManager.get('alfresco-FormDialog-instance');
          var elem = args[1].form;
          if(!('showConfig' in popupForm) || popupForm.showConfig.itemKind!="workflow") return;
          
          var submitButton = Dom.get( elem.id + "-submit-button" );
          
          // Change the label on the submit button
          submitButton.innerHTML = this.msg("createform.button.submit.label");        
          
          // Set the SiteName in a hidden field to be submitted
          var siteInput = Dom.getElementBy ( function (el){return el.name=='prop_stmwf_siteName';}, 'input' );
          siteInput.value = Alfresco.constants.SITE;
      },
      
      /**
       * Closes the edit task dialog popup
       *
       * @method onEditTaskFormClose
       * @param e {object} The event
       * @param args {array} Event arguments
       */      
      onEditTaskFormClose: function SiteTaskMgr_onEditTaskFormClose(e, args)
      {  
          this.onEditTaskFormCancel(e, args);
          
          // Refresh the dashlet area
          this._refreshTaskWindow();
      },
      
      /**
       * Cancel the edit task dialog popup
       *
       * @method onEditTaskFormCancel
       * @param e {object} The event
       * @param args {array} Event arguments
       */      
      onEditTaskFormCancel: function SiteTaskMgr_onEditTaskFormCancel(e, args)
      {
          var dialogPopup = Alfresco.util.ComponentManager.get('alfresco-FormDialog-instance');;
          if(!('showConfig' in dialogPopup) || dialogPopup.showConfig.itemKind!="task") return;
          
          YAHOO.Bubbling.unsubscribe("formDialogFormAvailable", this.onEditTaskFormRendered);
          YAHOO.Bubbling.unsubscribe("formDialogSuccessClose", this.onEditTaskFormClose);

          // Close the popup window
          dialogPopup.onCancelButtonClick();
      },
      
      /**
       * Do additional processing once the Task/Workflow form is rendered to alter behavior
       *
       * @method onNewTaskFormRendered
       * @param e {object} The event
       * @param args {array} Event arguments
       */      
      onEditTaskFormRendered: function SiteTaskMgr_onEditTaskFormRendered(e, args)
      {
          var popupForm = Alfresco.util.ComponentManager.get('alfresco-FormDialog-instance');
          var elem = args[1].form;
          if(!('showConfig' in popupForm) || popupForm.showConfig.itemKind!="task") return;
                    
          var submitButton = Dom.get( elem.id + "-submit-button" );
                    
          // Change the label on the submit button
          submitButton.innerHTML = this.msg("editform.button.submit.label"); 
          
      },
            
      /**
       * Event listener for configuration link click.
       *
       * @method onConfigSiteTaskMgrClick
       * @param e {object} HTML event
       */
      onConfigSiteTaskMgrClick: function WebView_onConfigWebViewClick(e)
      {
         Event.stopEvent(e);  

         //  Define the URL for the config dialog POST form to send config parameter updates
         var actionUrl = Alfresco.constants.URL_SERVICECONTEXT + "formtek/modules/dashlet/sitetaskmgr/config/" + encodeURIComponent(this.options.componentId);

         if (!this.configDialog)
         {
            //  Create the Configuration dialog if it does not already exist.  
            this.configDialog = new Alfresco.module.SimpleDialog(this.id + "-configDialog").setOptions(
            {
               width: "50em",
               templateUrl: Alfresco.constants.URL_SERVICECONTEXT + "formtek/modules/dashlet/sitetaskmgr/config?site=" + Alfresco.constants.SITE,
               onSuccess:
               {
                  fn: function SiteTaskMgr_onConfigSiteTaskMgr_callback(response)
                  {
                      this.onUpdateConfigDialogDefaults();
                      
                      // Reset the number of rows per page
                      this.widgets.alfrescoDataTable.getDataTable().configs.paginator.setRowsPerPage(this.options.maxPageItems);
                      
                      // Save off any changes to hidden columns
                      this._saveDataTableState();
                      this.services.preferences.set(PREFERENCES_TASKS_DASHLET_COLSTATE, YAHOO.lang.JSON.stringify(this.options.colState));
                                            
                      // Refresh of the dashlet area
                      this._refreshTaskWindow();
                  },
                  scope: this
               },
//               doBeforeDialogShow:
//               {
//                  fn: function (e, args)
//                  {
//                      // IE9 draggable dialog isn't working.  Stop it for now
//                      var oldConfig = this.configDialog.dialog.cfg.getConfig();
//                      oldConfig.draggable = false;
//                      this.configDialog.dialog.cfg.applyConfig( oldConfig, true );
//                  },
//                  scope: this
//               },
               doSetupFormsValidation:
               {
                  fn: function SiteTaskMgr_doSetupForm_callback(form)
                  {     
                    // Set up references for config dialog fields  
                    this.configDialog.widgets.maxPageItems = Dom.get(this.configDialog.id + "-maxPageItems");

                    // Add any validation functions
                    form.addValidation(this.configDialog.widgets.maxPageItems.id, Alfresco.forms.validation.mandatory, null, "blur");  
                    form.addValidation(this.configDialog.widgets.maxPageItems.id, Alfresco.forms.validation.number, null, "blur");
                    form.setShowSubmitStateDynamically(true, false);                 
                    
                    // Set current values for the config dialog fields
                    this.onSetConfigDialogToDefaults();
                    
                      // Set the show/hide buttons
                      var showhide = Dom.get( this.configDialog.id + "-show-hide-buttons" );
                      
                      if(showhide.innerHTML.replace(/^\s+/,"").length==0)
                      {
                          var dt = this.widgets.alfrescoDataTable.getDataTable();
                        
                          var showHideClick = function(e, oSelf) {
                              var sKey = this.get("name");
                              if(this.get("value") === "Hide") {
                                  // Hides a Column
                                  dt.hideColumn(sKey);
                              }
                              else {
                                  // Shows a Column
                                  dt.showColumn(sKey);
                              }
                          };
                        
                          // Get all the table columns
                          var allColumns = dt.getColumnSet().keys;
                        
                          var templCol = document.createElement("div");
                          YAHOO.util.Dom.addClass(templCol, "col-showhide-col");
                          var templKey = templCol.appendChild(document.createElement("span"));
                          YAHOO.util.Dom.addClass(templKey, "col-showhide-key");
                          var templBtns = templCol.appendChild(document.createElement("span"));
                          YAHOO.util.Dom.addClass(templBtns, "col-showhide-button");
                          var onclickObj = {fn:showHideClick, obj:this, scope:false };
            
                        
                          var elColumn, elKey, oButtonGrp;
                          for(var i=0,l=allColumns.length;i<l;i++) 
                          {
                              var oColumn = allColumns[i];
                            
                              // Add the template
                              elColumn = templCol.cloneNode(true);
                            
                              // The Column key
                              elKey = elColumn.firstChild;
                              elKey.innerHTML = this.msg("label.datatable." + oColumn.getKey());
                            
                              // The Button Group
                              oButtonGrp = new YAHOO.widget.ButtonGroup({ 
                                            id: "buttongrp"+i, 
                                            name: oColumn.getKey(), 
                                            container: elKey.nextSibling
                              });
                              oButtonGrp.addButtons([
                                { label: this.msg("label.show"), value: this.msg("label.show"), checked: ((!oColumn.hidden)), onclick: onclickObj},
                                { label: this.msg("label.hide"), value: this.msg("label.hide"), checked: ((oColumn.hidden)), onclick: onclickObj}
                              ]);
                                            
                              showhide.appendChild(elColumn);
                          }
                      }
                  },
                  scope: this
               }
            } );
            
            

         }

         this.configDialog.setOptions(
         {
            actionUrl: actionUrl
         }).show();
      },
      
        /**
         * Initialize the defaults of the config dialog
         */
        onSetConfigDialogToDefaults : function SiteTaskMgr_onSetConfigDialogToDefaults() 
        {  
            // Set current values for the config dialog fields
            this.configDialog.widgets.maxPageItems.value = this.options.maxPageItems;
        },
        
        /**
         * Update config dialog defaults
         *   After the config dialog successfully posts new values, update those values for the cached dialog
         */
        onUpdateConfigDialogDefaults : function SiteTaskMgr_onUpdateConfigDialogDefaults() 
        {  
            // Set current values for the config dialog fields
            this.options.maxPageItems = this.configDialog.widgets.maxPageItems.value;
        },

            

      /**
       * Reloads the list with the new filter and updates the filter menu button's label
       *
       * @param p_sType {string} The event
       * @param p_aArgs {array} Event arguments
       */
      onFilterSelected: function SiteTaskMgr_onFilterSelected(p_sType, p_aArgs)
      {
         var menuItem = p_aArgs[1];
         
         if (menuItem)
         {
            this.widgets.filterMenuButton.set("label", menuItem.cfg.getProperty("text"));
            this.widgets.filterMenuButton.value = menuItem.value;
            
            var parameters = this.substituteParameters(this.options.filters[menuItem.value], {});
            this.widgets.alfrescoDataTable.loadDataTable(parameters);

            // Save preferences
            this.services.preferences.set(PREFERENCES_TASKS_DASHLET_FILTER, menuItem.value);
         }
      },


      
/////////////////////////////////////////////////    

      renderNoItemsMessage: function SiteTaskMgr_renderNoItem()
      {
          var noItems = Dom.get(this.id + "-noitems");
          
          var desc = '<div class="noitemsimg"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/images/help-task-bw-32.png" /></div>';
          desc += '<div class="empty"><h3>' + this.msg("empty.title") + '</h3>';
          desc += '<span>' + this.msg("empty.description") + '</span></div>';
          
          noItems.innerHTML = desc;
      },
     
       /**
       * Open/Done/Archive datacell formatter
       */
      renderCellState:function SiteTaskMgr_onReady_renderCellState(elCell, oRecord, oColumn, oData)
      {
         var data = oRecord.getData(),
            desc = "";

         var statename = data.name;
         var statenameKey = "no-status";
         var today = new Date();
         var dueDateStr = data.properties["bpm_dueDate"];
         var dueDate = dueDateStr ? Alfresco.util.fromISO8601(dueDateStr) : null;
        
         if(statename=="stmwf:assignedTask")
         {
            if(data.properties.bpm_status=="In Progress") statenameKey="running";
            else if(dueDate!=null && today > dueDate) statenameKey="failed";
            else if(data.properties.bpm_status=="Not Yet Started") statenameKey="no-status";
            else if(data.properties.bpm_status=="Completed") statenameKey="completed";
            else if(data.properties.bpm_status=="On Hold") statenameKey="hold";
            else if(data.properties.bpm_status=="Cancelled") statenameKey="cancelled";
         }
         else if(statename=="stmwf:doneTask")
         {
            statenameKey = "success";
         }
         else if(statename=="stmwf:archivedTask")
         {
            statenameKey = "archive";
         }

         desc = '<img src="' + Alfresco.constants.URL_RESCONTEXT + 'formtek/components/images/job-' + statenameKey + '-16.png" title="' + 
                this.msg("label.statename") + ': ' + this.msg("statename." + statenameKey) + '"/>';

         elCell.innerHTML = desc;
      },
      
      /**
       * Priority icons custom datacell formatter
       */
      renderCellPriority: function SiteTaskMgr_onReady_renderCellPriority(elCell, oRecord, oColumn, oData)
      {
         var data = oRecord.getData(),
            desc = "";

         var priority = data.properties["bpm_priority"],
           priorityMap = { "1": "high", "2": "medium", "3": "low" },
           priorityKey = priorityMap[priority + ""];

         desc = '<img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/images/priority-' + priorityKey + '-16.png" title="' + this.msg("label.priority", this.msg("priority." + priorityKey)) + '"/>';

         elCell.innerHTML = desc;
      },
      
      /**
       * Burden icons custom datacell formatter
       */
      renderCellBurden: function SiteTaskMgr_onReady_renderCellBurden(elCell, oRecord, oColumn, oData)
      {
         var data = oRecord.getData(),
            desc = "";

         var burden = data.properties["stmwf_burden"].toLowerCase();

         desc = '<img src="' + Alfresco.constants.URL_RESCONTEXT + 'formtek/components/images/' + burden + '-burden-16.png" title="' + 
             this.msg("burden." + burden) + '"/>';

         elCell.innerHTML = desc;
      },
      
      /**
       * Task Name datacell formatter
       */
      renderCellTaskName:function SiteTaskMgr_onReady_renderCellTaskName(elCell, oRecord, oColumn, oData)
      {
         var data = oRecord.getData(),
            desc = "";

         var taskname = data.workflowInstance.message;
         var wfname = data.workflowInstance.id;
         desc += '<h3><a href="' + $siteURL('workflow-details?workflowId=' + wfname) + '" class="theme-color-1" title="' + this.msg("label.wfname") + '">' + $html(taskname) + '</a></h3>';

         elCell.innerHTML = desc;
      },

      
      /**
       * Assignee datacell formatter
       */
      renderCellAssignee:function SiteTaskMgr_onReady_renderCellAssignee(elCell, oRecord, oColumn, oData)
      {
         var data = oRecord.getData(),
            desc = "";

         var assignee = data.owner;

         if (!assignee || !assignee.userName)
         {
            desc = '<h3><span class="theme-bg-color-5 theme-color-5 unassigned-task">' + this.msg("assignee.unassignedTask") + '</span></h3>';
         }
         else
         {
            desc += '<h3><span title="' + this.msg("label.assignee") +  ': ' + $html(assignee.firstName + ' ' + assignee.lastName) + '">' +  $html(assignee.userName) + '</span></h3>';
         }

         elCell.innerHTML = desc;
      },
      
      /**
       * Initiator datacell formatter
       */
      renderCellInitiator:function SiteTaskMgr_onReady_renderCellInitiator(elCell, oRecord, oColumn, oData)
      {
         var data = oRecord.getData(),
            desc = "";

         var initiator = data.workflowInstance.initiator;

         desc += '<h3><span title="' + this.msg("label.initiator") +  ': ' + $html(initiator.firstName + ' ' + initiator.lastName) + '">' +  $html(initiator.userName) + '</span></h3>';

         elCell.innerHTML = desc;
      },
      
      /**
       * Comment datacell formatter
       */
      renderCellComment:function SiteTaskMgr_onReady_renderCellComment(elCell, oRecord, oColumn, oData)
      {
         var data = oRecord.getData(),
            desc = "";

         var comment = data.properties["bpm_comment"];

         desc += '<h3><span title="' + this.msg("label.comment") + '">' +  $html(comment) + '</span></h3>';

         elCell.innerHTML = desc;
      },
      
      /**
       * Previous Comment datacell formatter
       */
      renderCellPrevComment:function SiteTaskMgr_onReady_renderCellPrevComment(elCell, oRecord, oColumn, oData)
      {
         var data = oRecord.getData(),
            desc = "";

         var comment = data.properties["stmwf_previousComment"];

         desc += '<h3><span title="' + this.msg("label.comment") + '">' +  $html(comment) + '</span></h3>';

         elCell.innerHTML = desc;
      },
      
      /**
       * Percent Complete datacell formatter
       */
      renderCellPercent:function SiteTaskMgr_onReady_renderCellPercent(elCell, oRecord, oColumn, oData)
      {
         var data = oRecord.getData(),
            desc = "";

         var percentage = data.properties["bpm_percentComplete"];

         desc += '<h3><span title="' + this.msg("label.percentage") + '">' + percentage + '%&nbsp;</span></h3>';

         elCell.innerHTML = desc;
      },
      
      /**
       * Created datacell formatter
       */
      renderCellCreated:function SiteTaskMgr_onReady_renderCellCreated(elCell, oRecord, oColumn, oData)
      {
         var data = oRecord.getData(),
            desc = "";

         var startDateStr = data.workflowInstance.startDate,
             startDate = startDateStr ? Alfresco.util.formatDate( Alfresco.util.fromISO8601(startDateStr), "mediumDate") : "";

         desc += '<h3><span title="' + this.msg("label.created") + '">' +  startDate + '</span></h3>';

         elCell.innerHTML = desc;
      },
      
      /**
       * Due Date datacell formatter
       */
      renderCellDueDate:function SiteTaskMgr_onReady_renderCellDueDate(elCell, oRecord, oColumn, oData)
      {
         var data = oRecord.getData(),
            desc = "";

         var dueDateStr = data.properties["bpm_dueDate"],
            dueDate = dueDateStr ? Alfresco.util.formatDate( Alfresco.util.fromISO8601(dueDateStr), "mediumDate") : "";

         desc += '<h3><span title="' + this.msg("label.duedate") + '">' + dueDate + '</span></h3>';

         elCell.innerHTML = desc;
      },
      
      /**
       * Actions custom datacell formatter
       */
      renderCellActions:function SiteTaskMgr_onReady_renderCellActions(elCell, oRecord, oColumn, oData)
      {
         var data = oRecord.getData(),
            desc = "";

         // Only add for those users with sufficient privilege
         if (data.isEditable)
         {
          
           if(data.name=="stmwf:assignedTask")
           {
               desc += '<img class="todone_forwardbutton" src="' + Alfresco.constants.URL_RESCONTEXT + 'formtek/components/images/done-16.png" title="' + this.msg("label.action.forwardtodone") + '"/>&nbsp;&nbsp;';
               desc += '<img class="reassignbutton" src="' + Alfresco.constants.URL_RESCONTEXT + 'components/images/user-16.png" title="' + this.msg("label.action.reassign") + '"/>&nbsp;&nbsp;';
               desc += '<img class="editbutton" src="' + Alfresco.constants.URL_RESCONTEXT + 'components/images/edit-16.png" title="' + this.msg("label.action.edit") + '"/>&nbsp;&nbsp;';
           }
           else if(data.name=="stmwf:doneTask")
           {
               desc += '<img class="toassigned_backbutton" src="' + Alfresco.constants.URL_RESCONTEXT + 'components/images/back-arrow.png" title="' + this.msg("label.action.back") + '"/>&nbsp;&nbsp;';
               desc += '<img class="toarchive_forwardbutton" src="' + Alfresco.constants.URL_RESCONTEXT + 'formtek/components/images/archive-16.png" title="' + this.msg("label.action.forwardtoarchive") + '"/>&nbsp;&nbsp;';
               desc += '<img class="reassignbutton" src="' + Alfresco.constants.URL_RESCONTEXT + 'components/images/user-16.png" title="' + this.msg("label.action.reassign") + '"/>&nbsp;&nbsp;';
               desc += '<img class="editbutton" src="' + Alfresco.constants.URL_RESCONTEXT + 'components/images/edit-16.png" title="' + this.msg("label.action.edit") + '"/>&nbsp;&nbsp;';
           }
           
           desc += '<img class="cancelbutton" src="' + Alfresco.constants.URL_RESCONTEXT + 'components/images/delete-16.png" title="' + this.msg("label.action.cancel") + '"/>';
         }
         else
         {
           desc += '<img class="editbutton-disabled" src="' + Alfresco.constants.URL_RESCONTEXT + 'components/images/edit-disabled-16.png" title="' + this.msg("label.action.edit") + '"/>&nbsp;&nbsp;';
           desc += '<img class="cancelbutton-disabled" src="' + Alfresco.constants.URL_RESCONTEXT + 'components/images/delete-disabled-16.png" title="' + this.msg("label.action.cancel") + '"/>';
         }

         elCell.innerHTML = desc;
      },

/////////////////////////////////////////////////           
      // Draw the Edit task dialog
      _showEditTaskDialog: function SiteTaskMgr_showEditTaskDialog(taskId, wfId)
      {           
          // Build the URL of where to post to
          var webscript = YAHOO.lang.substitute("/api/formtek/{wfId}/{site}/{item_kind}/{item_id}/sitetaskformprocessor",
          {
            wfId: wfId,
            site: Alfresco.constants.SITE,
            item_kind: 'task',
            item_id: taskId
          });
          Formtek.module.getFormDialogInstance().show(
          {
             formId: 'sitetask',
             itemKind: 'task',
             itemId: taskId,
             mode: 'edit',
             submissionUrl: webscript,
             dialogTitle: this.msg("editdialog.title")
          });
          
          // Listen to know when the popup form has finished rendering
          YAHOO.Bubbling.on("formDialogFormAvailable", this.onEditTaskFormRendered, this);

          // Listen for when the popup form dialog closes on success to know when to refresh
          YAHOO.Bubbling.on("formDialogSuccessClose", this.onEditTaskFormClose, this);

      },
      
      
      // Refresh the dashlet's contents based on any changes/additions made to the tasks
      _refreshTaskWindow: function SiteTaskMgr_refreshTaskWindow()
      {
          var parameters = this.substituteParameters(this.options.filters[this.widgets.filterMenuButton.value], {});
          this.widgets.alfrescoDataTable.loadDataTable(parameters);
      },
      
      // Transition the task of a workflow with the given outcome
      _transitionWorkflow: function SiteTaskMgr_transitionWorkflow(wfId, taskId, outcome, curTaskState)
      {
          // Set the 'outcome' value on the current task so that the upcoming transition will work
          var afterOutcome = function(res, args)
          {
              var finishFunc = function(res)
              {
                  // Finally, set the new properties for the new task
                  var wfId = curTaskState["wfId"];
                  delete curTaskState["wfId"];
                  this._setTaskProperties(wfId, curTaskState, null, {}); 
                  
                  // Refresh and display the difference     
                  this._refreshTaskWindow();     
              }
              //  Second, End the task and transition to the next one
              this._endTask(taskId, "Next", finishFunc);
          }
          // First set the outcome so that the transition will succeed
          this._setTaskPropertyWithSuccess(wfId, "stmwf_doneOutcome", outcome, afterOutcome, {});
      },
       
      // Set a single property on the task of the current workflow task
      _setTaskProperty: function SiteTaskMgr_setTaskProperty(wfId, propname, propvalue)
      {
          this._setTaskPropertyWithSuccess(wfId, propname, propvalue, null, {});
      },
      _setTaskPropertyWithSuccess: function SiteTaskMgr_setTaskPropertyWithSuccess(wfId, propname, propvalue, sFunc, extraProps)
      {
          var dataObj = {};
          dataObj[propname] = propvalue;
          
          this._setTaskProperties(wfId, dataObj, sFunc, extraProps);
      },

      // Set properties on a task of the current workflow task
      _setTaskProperties: function SiteTaskMgr_setTaskProperties(wfId, dataObj, sFunc, extraProps)
      {
         var webscript = YAHOO.lang.substitute("api/formtek/dashlet/projtaskmgr/site-task-instances?workflow_instance_id={workflow_instance_id}&site={site}",
         {
            workflow_instance_id: wfId,
            site: Alfresco.constants.SITE
         });
         
         var successFn = function(res)
         {
         }
         var failureFn = function(res)
         {
         }
         
         // Use a different success function, if supplied
         if(sFunc!=null) successFn = sFunc;
                     
         Alfresco.util.Ajax.request(
         {
            url: Alfresco.constants.PROXY_URI + webscript,
            method: Alfresco.util.Ajax.PUT,
            dataObj: dataObj,
            requestContentType: Alfresco.util.Ajax.JSON,
            successCallback:
            {
               fn: successFn,
               scope: this,
               obj: extraProps
            },
            failureCallback:
            {
               fn: failureFn,
               scope: this
            }
         });
      },
      // Cancel or Delete a Workflow instance
      /**
       * Prompts the user if the workflow really should be cancelled
       *
       * @method cancelSiteWorkflow
       * @param workflowId {String} The workflow id
       * @param workflowTitle {String} The workflow title
       * @private
       */
      _cancelSiteWorkflowConfirm: function SiteTaskMgr_cancelSiteWorkflowConfirm(workflowId, workflowTitle)
      {
         var me = this,
               wid = workflowId;
         Alfresco.util.PopupManager.displayPrompt(
         {
            title: this.msg("workflow.cancel.title"),
            text: this.msg("workflow.cancel.label", workflowTitle),
            noEscape: true,
            buttons: [
               {
                  text: Alfresco.util.message("button.yes", this.name),
                  handler: function WA_cancelWorkflow_yes()
                  {
                     this.destroy();
                     me._cancelSiteWorkflow.call(me, wid);
                  }
               },
               {
                  text: Alfresco.util.message("button.no", this.name),
                  handler: function WA_cancelWorkflow_no()
                  {
                     this.destroy();
                  },
                  isDefault: true
               }]
         });
      },
      _cancelSiteWorkflow: function SiteTaskMgr_cancelWorkflow(wfId)
      {
         var me = this;
         var feedbackMessage = Alfresco.util.PopupManager.displayMessage(
         {
            text: this.msg("workflow.cancel.feedback"),
            spanClass: "wait",
            displayTime: 0
         });
         
         var webscript = YAHOO.lang.substitute("api/formtek/dashlet/projtaskmgr/workflow-instances/{workflow_instance_id}?site={site}&forced={forced}",
         {
            workflow_instance_id: wfId,
            site: Alfresco.constants.SITE,
            forced: "false"
         });
         
         var successFn = function(res, wfId)
         {
              feedbackMessage.destroy();
              if (res.json && res.json.success)
              {
                this._refreshTaskWindow();
              }
              else
              {
                 Alfresco.util.PopupManager.displayMessage(
                 {
                    text: Alfresco.util.message("workflow.cancel.failure", this.name)
                 });
              }
         }
         var failureFn = function(res, wfId)
         {
              feedbackMessage.destroy();
              Alfresco.util.PopupManager.displayMessage(
              {
                 text: Alfresco.util.message("workflow.cancel.failure", this.name)
              });
         }
                     
         Alfresco.util.Ajax.request(
         {
            url: Alfresco.constants.PROXY_URI + webscript,
            method: Alfresco.util.Ajax.DELETE,
            requestContentType: Alfresco.util.Ajax.JSON,
            successCallback:
            {
               fn: successFn,
               obj: wfId,
               scope: this
            },
            failureCallback:
            {
               fn: failureFn,
               obj: wfId,
               scope: this
            }
         });
      },
      // End Task and transition to the Next Node
      _endTask: function SiteTaskMgr_endTask(taskId, transition, sFunc)
      {
         var webscript = YAHOO.lang.substitute("api/formtek/dashlet/projtaskmgr/end-task?task_id={task_id}&site={site}&transition={transition}",
         {
            task_id: taskId,
            site: Alfresco.constants.SITE,
            transition: transition
         });
         
         var successFn = function(res)
         {
             this._refreshTaskWindow();
         }
         var failureFn = function(res)
         {
         }
         if(sFunc!=null) successFn = sFunc;
                     
         Alfresco.util.Ajax.request(
         {
            url: Alfresco.constants.PROXY_URI + webscript,
            method: Alfresco.util.Ajax.GET,
            requestContentType: Alfresco.util.Ajax.JSON,
            successCallback:
            {
               fn: successFn,
               scope: this
            },
            failureCallback:
            {
               fn: failureFn,
               scope: this
            }
         });
      }

   });
})();
