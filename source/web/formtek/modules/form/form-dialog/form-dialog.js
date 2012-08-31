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
 * Form-Dialog component.
 *
 * Popup a general Alfresco Share form
 *
 * @namespace Formtek.module
 * @class Formtek.module.FormDialog
 */
 
(function()
{
   
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
         Event = YAHOO.util.Event, 
         KeyListener = YAHOO.util.KeyListener;
   
   /**
    * FormDialog constructor.
    *
    * FormDialog is considered a singleton so constructor should be treated as private,
    * please use Formtek.module.getFormDialogInstance() instead.
    *
    * @param {string} htmlId The HTML id of the parent element
    * @return {Formtek.module.FormDialog} The new FormDialog instance
    * @constructor
    * @private
    */
   Formtek.module.FormDialog = function(containerId)
   {
      this.name = "Formtek.module.FormDialog";
      this.id = containerId;

      var instance = Alfresco.util.ComponentManager.get(this.id);
      if (instance !== null)
      {
         throw new Error("An instance of Formtek.module.FormDialog already exists -- a Form is already open as a dialog.");
      }

      /* Register this component */
      Alfresco.util.ComponentManager.register(this);

      // Load YUI Components
      Alfresco.util.YUILoaderHelper.require(["button", "container", "datatable", "datasource", "connection", "json"], this.onComponentsLoaded, this);

      return this;
      
   };

   Formtek.module.FormDialog.prototype =
   {

      /**
       * The default config for the gui state for the historic properties dialog.
       * The user can override these properties in the show() method.
       *
       * @property defaultShowConfig
       * @type object
       */
      defaultShowConfig:
      {
         itemKind: 'workflow',
         itemId: null,               // eg., activiti%24FTKSiteTaskManager  or refNodeId
         mode: 'create',
         submitType: 'json',
         showCaption: 'true',
         formUI: 'true',
         showCancelButton: 'true',
         dialogTitle: null,
         url: ""                     // optionally, the url that returns the form markup can be passed in directly
      },

      /**
       * The merged result of the defaultShowConfig and the config passed in
       * to the show method.
       *
       * @property showConfig
       * @type object
       */
      showConfig: {},
      
      /**
       * Object container for storing YUI widget and HTMLElement instances.
       *
       * @property widgets
       * @type object
       */
      widgets: {},
      
      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       *
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function FD_onComponentsLoaded()
      {
         // Shortcut for dummy instance
         if (this.id === null)
         {
            return;
         }
      },
      

      /**
       * Show can be called multiple times and will display a form depending 
       * on the config parameter.
       *
       * @method show
       * @param config {object} describes how the dialog should be displayed
       * The config object is in the form of:
       * {
       *    itemKind: {string},  // workflow, node
       *    itemId:   {string},  // nodeRef on edit/view
       *    formId:   {string},  // formId
       *    mode:     {string}   // create, view, edit
       *
       *    url:      {string}   // optional url to call to specify form markup
       * }
       */
      show: function FD_show(config)
      {
         // Merge the supplied config with default config and check mandatory properties
         this.showConfig = YAHOO.lang.merge(this.defaultShowConfig, config);
         if (this.url === undefined)
         {
             if(this.showConfig.itemId === undefined ||
                 this.showConfig.itemKind === undefined ||
                 this.showConfig.mode === undefined)
             {
                 throw new Error("An itemId, itemKind and mode must be provided");
             }
             else
             {
                 this.showConfig.url = Alfresco.constants.URL_SERVICECONTEXT + "components/form" +
                     "?itemKind=" + this.showConfig.itemKind +
                     "&itemId=" + this.showConfig.itemId +
                     "&mode=" + this.showConfig.mode +
                     "&submitType=" + this.showConfig.submitType  +
                     "&showCaption=" + this.showConfig.submitType  +
                     "&formUI=" + this.showConfig.formUI  +
                     "&showCancelButton=" + this.showConfig.showCancelButton  +
                     "&htmlid=" + this.id;
                     
                 // Do we need to specialize this to a particular formID?
                 if(this.showConfig.formId !== undefined && this.showConfig.formId!=null)
                        this.showConfig.url += "&formId=" + this.showConfig.formId;
                        
                 // Do we need to specialize this to a particular submissionUrl?
                 if(this.showConfig.submissionUrl !== undefined && this.showConfig.submissionUrl!=null)
                        this.showConfig.url += "&submissionUrl=" + this.showConfig.submissionUrl;
             }
         }
         
         Alfresco.util.Ajax.request(
         {
            url: Alfresco.constants.URL_SERVICECONTEXT + "formtek/modules/form/form-dialog?htmlid=" + this.id,
            successCallback:
            {
               fn: this.onTemplateLoaded,
               scope: this
            },
            failureMessage: "Could not load html template for form container",
            execScripts: true
         });
         
        
         // Register the ESC key to close the dialog
         this.widgets.escapeListener = new KeyListener(document,
         {
            keys: KeyListener.KEY.ESCAPE
         },
         {
            fn: this.onCancelButtonClick,
            scope: this,
            correctScope: true
         });                    
      },

      /**
       * Called when the dialog html template has been returned from the server.
       *
       * @method onTemplateLoaded
       * @param response {object} a Alfresco.util.Ajax.request response object
       */
      onTemplateLoaded: function FD_onTemplateLoaded(response)
      {
         // Inject the template from the XHR request into a new DIV element
         var containerDiv = document.createElement("div");
         containerDiv.innerHTML = response.serverResponse.responseText;
               
         var dialogDiv = YAHOO.util.Dom.getFirstChild(containerDiv);

         // Create the panel from the HTML returned in the server reponse         
         this.widgets.panel = Alfresco.util.createYUIPanel(dialogDiv);

         // Save a reference to the HTMLElement displaying texts so we can alter the text later
         this.widgets.headerText = Dom.get(this.id + "-header-span");
         this.widgets.formContainer = Dom.get(this.id + "-properties-form");
         
         // Load Form content
         this._loadForm();
         
         // Show panel
         this._showPanel();      
         
         // Change the display location of the dialog to be near top
         dialogDiv.parentNode.style.top = "50px"; 
         
         // Remove the close button in upper right for now
         var closebutton = Dom.getElementsByClassName('container-close', 'a', Dom.get('alfresco-FormDialog-instance-dialog'));
         if(closebutton!=null && closebutton.length>0)
         {
             var cancelelem = new YAHOO.util.Element( closebutton );
             this.widgets.closebutton = cancelelem;
             var handleCancelClick = function(e) 
             {
                 this.onCancelButtonClick();         
             };
             cancelelem.on('click', handleCancelClick, null, this);
             this.widgets.cancelelemfunc = handleCancelClick;
         }

      },

         
      
      /**
       * Fired when the user clicks the cancel button.
       * Closes the panel.
       *
       * @method onCancelButtonClick
       * @param event {object} a Button "click" event
       */
      onCancelButtonClick: function FD_onCancelButtonClick()
      {
         YAHOO.Bubbling.fire("formDialogSuccessClose", {});
          
         // Hide the panel
         this.widgets.panel.hide();
         
         // Disable the Esc key listener
         this.widgets.escapeListener.disable();
         
         // Disable the close button listener
         this.widgets.closebutton.removeListener("click", this.widgets.cancelelemfunc);

      },
      
      /**
       * 
       * Fired when loadForm successfully returns
       * Loads the results of the AJAX call into the HTML element we grabbed a reference to earlier
       * 
       * @method onFormLoaded
       * 
       */
      onFormLoaded: function FD_onFormLoaded(response){
         // Clean new markup from scripts so it doesn't instantiate the new component instance yet
         var result = Alfresco.util.Ajax.sanitizeMarkup(response.serverResponse.responseText);

         // Replace the old markup with the new
         this.widgets.formContainer.innerHTML = result[0];
         
         // At the end of the script for instantiating form objects add a function to set the callback
         var script = result[1];
         script += "\n  YAHOO.Bubbling.on('afterFormRuntimeInit', Alfresco.util.ComponentManager.get('alfresco-FormDialog-instance').onSetSuccessCallback, null);";

         // Run the script contained in the response
         window.setTimeout(script, 0);
      },

      /**
       * Replace the submit handler on success for the form, otherwise the form won't close
       *
       * @method onSetSuccessCallback
       */      
      onSetSuccessCallback: function FD_setSuccessCallback(layer, args)
      {
         YAHOO.Bubbling.unsubscribe('afterFormRuntimeInit', Alfresco.util.ComponentManager.get('alfresco-FormDialog-instance').onSetSuccessCallback);
         if(args.length > 1)
         {
             var formui = args[1].component
             var popupFrame = Alfresco.util.ComponentManager.get('alfresco-FormDialog-instance');
             if(formui!=null)
             {
                 formui.formsRuntime.ajaxSubmitHandlers.successCallback.fn = function formSuccessCallback(response)
                 {
                     YAHOO.Bubbling.fire("formDialogSuccessClose", 
                       {
                         frame: popupFrame,
                         response: response
                       }
                     );
                 };
             }
          }
         
         // Tell listenees that the popup form dialog is available
         YAHOO.Bubbling.fire("formDialogFormAvailable",
         {
            formdialog: formui,
            frame: popupFrame,
            form: Dom.get(popupFrame.id + "-form")
         });
      },
      
      /**
       * Adjust the gui according to the config passed into the show method.
       *
       * @method _applyConfig
       * @private
       */
      _applyConfig: function FD_applyConfig()
      {
         // Set the panel section
         var headerString = this.showConfig.dialogTitle;
         if(headerString==null && this.showConfig.itemKind=='workflow') headerString = "Dialog";
         var header = Alfresco.util.message("form.dialogue.header", this.name,
         {
            "0": "<strong>" + headerString + "</strong>"
         });
         this.widgets.headerText["innerHTML"] = header;

      },
      
      /**
       * 
       * Trigger an AJAX request to load the form markup via the forms service
       * 
       * @method loadForm
       * @private
       * 
       */     
      _loadForm: function FD_loadForm()
      { 
         Alfresco.util.Ajax.request(
         {
            url: this.showConfig.url,
            successCallback:
            {
               fn: this.onFormLoaded,
               obj: this,
               scope: this
            },
            failureMessage: "Form could not be loaded",
            scope: this
         });
      },     

      /**
       * Prepares the gui and shows the panel.
       *
       * @method _showPanel
       * @private
       */
      _showPanel: function FD_showPanel()
      {

         // Apply the config before it is showed
         this._applyConfig();

         // Enable the Esc key listener
         this.widgets.escapeListener.enable();
         
         // Show the panel
         this.widgets.panel.show();
      }

   };
})();

//  Limit so that only a single form popup is possible at one time
Formtek.module.getFormDialogInstance = function()
{
   var instanceId = "alfresco-FormDialog-instance";
   return Alfresco.util.ComponentManager.get(instanceId) || new Formtek.module.FormDialog(instanceId);
}
 