<#assign id = args.htmlid>
<script type="text/javascript">//<![CDATA[
(function()
{
   var siteTaskMgr = new Formtek.dashlet.SiteTaskMgr("${args.htmlid}").setOptions(
   {
      componentId      : "${instance.object.id}",
      "maxPageItems"   : "${maxPageItems!"50"}",
      "workflowid"     : "${workflowid!"FTKSiteTaskManager"}",
      filters:
      {<#list filters as filter>
         "${filter.type?js_string}": "${filter.parameters?js_string}"<#if filter_has_next>,</#if>
      </#list>}
   }).setMessages(${messages});
   new Alfresco.widget.DashletResizer("${args.htmlid}", "${instance.object.id}");
   
   //Subscribe to the Site Task Manager onConfigWebViewClick function to popup the config dialog
   var editDashletEvent = new YAHOO.util.CustomEvent("onDashletConfigure");
   editDashletEvent.subscribe(siteTaskMgr.onConfigSiteTaskMgrClick, siteTaskMgr, true);
   
   new Formtek.component.TaskReassign("${args.htmlid}").setOptions(
   { }).setMessages(${messages});
   
   new Alfresco.widget.DashletTitleBarActions("${args.htmlid}").setOptions(
   {
      actions:
      [
<#if userIsSiteManager>
         {
            cssClass: "edit",
            eventOnClick: editDashletEvent,
            tooltip: "${msg("dashlet.edit.tooltip")?js_string}"
         },
</#if>
         {
            cssClass: "help",
            bubbleOnClick:
            {
               message: "${msg("dashlet.help")?js_string}"
            },
            tooltip: "${msg("dashlet.help.tooltip")?js_string}"
         }
      ]
   });
})();
//]]></script>


<div class="dashlet sitetaskmgr-dashlet">
   <div class="title">${msg("header")}</div>
   
   
   <div class="toolbar flat-button">
      <div class="hidden">
         <span class="align-left yui-button yui-menu-button" id="${id}-filters">
            <span class="first-child">
               <button type="button" tabindex="0"></button>
            </span>
         </span>
         <select id="${id}-filters-menu">
         <#list filters as filter>
            <option value="${filter.type?html}">${msg("filter." + filter.type)}</option>
         </#list>
         </select>
         <span class="align-right yui-button-align">
            <span class="first-child">
               <a href="#" class="theme-color-1" id="${id}-newtask">
                  <img src="${url.context}/res/components/images/workflow-16.png" style="vertical-align: text-bottom" width="16" />
                  ${msg("link.newTask")}</a>
            </span>
         </span>
         <div class="clear"></div>
      </div>
   </div>

   <div class="body scrollableList" id="${id}-dtbody" <#if args.height??>style="height: ${args.height}px;"</#if>>
      <div id="${id}-taskgroup">
          <div class="yui-u no-margin">
              <div id="${id}-tasks"></div>
          </div>
          <div class="yui-u center-paginator">
             <div id="${id}-paginator" class="paginator">&nbsp;</div>
          </div>
      </div>
      <div class="yui-u">
          <div id="${id}-noitems" class="noitems"></div>
      </div>
   </div>
   
   <!-- People Finder Dialog -->
   <div style="display: none;">
      <div id="${id}-reassignPanel" class="task-reassign reassign-panel">
         <div class="hd">${msg("panel.reassign.header")}</div>
         <div class="bd thin-borders">
            <div style="margin: auto 10px;">
               <div id="${id}-peopleFinder"></div>
            </div>
         </div>
      </div>
   </div>

</div>
