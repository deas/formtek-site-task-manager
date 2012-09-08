<#assign el=args.htmlid?html>

<div id="${el}-dialog" class="form-dialog">
   <div class="hd">
      <span id="${el}-header-span" class="form-dialog-header"></span>
   </div>
   <div class="bd">     
      <div id="${el}-properties-form">
         <p>Loading...</p>
      </div>

   </div>
</div>

<script type="text/javascript">//<![CDATA[
Alfresco.util.addMessages(${messages}, "Formtek.module.FormDialog");
//]]></script>
