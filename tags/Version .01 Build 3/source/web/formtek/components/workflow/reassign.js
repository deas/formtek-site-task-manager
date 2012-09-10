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
 * TaskReassign component.
 *
 * @namespace Formtek.component
 * @class Formtek.component.TaskReassign
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
      Event = YAHOO.util.Event,
      Selector = YAHOO.util.Selector;

  /**
    * Alfresco Slingshot aliases
    */
    var $html = Alfresco.util.encodeHTML,
       $siteURL = Alfresco.util.siteURL;

   /**
    * TaskReassign constructor.
    *
    * @param {String} htmlId The HTML id of the parent element
    * @return {Formtek.component.TaskReassign} The new TaskReassign instance
    * @constructor
    */
   Formtek.component.TaskReassign = function TaskReassign_constructor(htmlId)
   {
      Formtek.component.TaskReassign.superclass.constructor.call(this, htmlId, ["button"]);

      // Re-register with our own name
      this.name = "Formtek.component.TaskReassign";
      this.id = htmlId + "-reassignPanel";
      this.dashletId = htmlId;

      // Instance variables
      this.options = YAHOO.lang.merge(this.options, Formtek.component.TaskReassign.superclass.options);
      Alfresco.util.ComponentManager.register(this);
      this.isRunning = false;
      this.taskId = null;

      return this;
   };

   YAHOO.extend(Formtek.component.TaskReassign, Alfresco.component.ShareFormManager,
   {

      /**
       * Keeps track if this component is running an action or not
       *
       * @property isRunning
       * @type Boolean
       */
      isRunning: false,

      /**
       * The task instance id
       *
       * @property taskId
       * @type String
       */
      taskId: null,
      
      /**
       * The workflow instance id
       *
       * @property wfId
       * @type String
       */
      wfId: null,

      /**
       * Fired by YUI when parent element is available for scripting.
       * Template initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function SiteTaskManager_onReady()
      {
         // Load in the People Finder component from the server
         Alfresco.util.Ajax.request(
         {
            url: Alfresco.constants.URL_SERVICECONTEXT + "components/people-finder/people-finder",
            dataObj:
            {
               htmlid: this.dashletId + "-peopleFinder"
            },
            successCallback:
            {
               fn: this.onPeopleFinderLoaded,
               scope: this
            },
            failureMessage: "Could not load People Finder component",
            execScripts: true
         });

      },      

      /**
       * Called when the people finder template has been loaded.
       * Creates a dialog and inserts the people finder for choosing assignees.
       *
       * @method onPeopleFinderLoaded
       * @param response The server response
       */
      onPeopleFinderLoaded: function SiteTaskManager_onPeopleFinderLoaded(response)
      {
         // Inject the component from the XHR request into it's placeholder DIV element
         var finderDiv = Dom.get(this.dashletId + "-peopleFinder");
         finderDiv.innerHTML = response.serverResponse.responseText;

         // Create the Assignee dialog
         this.widgets.reassignPanel = Alfresco.util.createYUIPanel(this.dashletId + "-reassignPanel");

         // Find the People Finder by container ID
         this.widgets.peopleFinder = Alfresco.util.ComponentManager.get(this.dashletId + "-peopleFinder");

         // Set the correct options for our use
         this.widgets.peopleFinder.setOptions(
         {
            singleSelectMode: true,
            addButtonLabel: this.msg("button.select")
         });

      },

      /**
       * Updates a task property
       *
       * @method: _updateTaskProperties
       * @private
       */
      _updateTaskProperties: function SiteTaskManager__updateTaskProperties(properties, action)
      {
         YAHOO.lang.later(2000, this, function()
         {
            if (this.isRunning)
            {
               if (!this.widgets.feedbackMessage)
               {
                  this.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage(
                  {
                     text: this.msg("message." + action),
                     spanClass: "wait",
                     displayTime: 0
                  });
               }
               else if (!this.widgets.feedbackMessage.cfg.getProperty("visible"))
               {
                  this.widgets.feedbackMessage.show();
               }
            }
         }, []);

         // Run rules for folder (and sub folders)
         if (!this.isRunning)
         {
            this.isRunning = true;

            // Start/stop inherit rules from parent folder
            Alfresco.util.Ajax.jsonPut(
            {
               url: Alfresco.constants.PROXY_URI_RELATIVE + "api/task-instances/" + this.taskId,
               dataObj: properties,
               successCallback:
               {
                  fn: function(response, action)
                  {
                     this.isRunning = false;
                     var data = response.json.data;
                     if (data)
                     {
                        Alfresco.util.PopupManager.displayMessage(
                        {
                           text: this.msg("message." + action + ".success")
                        });

                        YAHOO.lang.later(3000, this, function(data)
                        {
                           if (data.owner && data.owner.userName == Alfresco.constants.USERNAME)
                           {
                              // Let the user keep working on the task since he claimed it
                              document.location.reload();
                           }
                           else
                           {
                              // Take the user to the most suitable place
                              this.navigateForward(true);
                           }
                        }, data);

                     }
                  },
                  obj: action,
                  scope: this
               },
               failureCallback:
               {
                  fn: function(response)
                  {
                     this.isRunning = false;
                     Alfresco.util.PopupManager.displayPrompt(
                     {
                        title: this.msg("message.failure"),
                        text: this.msg("message." + action + ".failure")
                     });
                  },
                  scope: this
               }
            });
         }
      }


   });
})();
