<#assign el=args.htmlid?html>

<div id="${el}-configDialog" class="config-dashlet sitetaskmgr">
   <div class="hd">${msg("config.title")}</div>
   <div class="bd">
      <form id="${el}-form" action="" method="POST">
         
         <div class="yui-gd">
            <div class="yui-u first"><label for="${el}-maxPageItems">${msg("config.label.maxPageItems")}:</label></div>
            <div class="yui-u"><input id="${el}-maxPageItems" type="text" name="maxPageItems" value=""/><span class="sup_mandatory">&nbsp;*</span></div>
         </div>

         <div class="bdft">
            <input type="submit" id="${el}-ok" value="${msg("button.ok")}" />
            <input type="button" id="${el}-cancel" value="${msg("button.cancel")}" />
         </div>
      </form>
   </div>
</div>
   