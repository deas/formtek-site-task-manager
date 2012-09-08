<import resource="classpath:alfresco/site-webscripts/org/alfresco/components/workflow/workflow.lib.js">

function main()
{
    var conf = new XML(config.script);
    var filters = [];

    var copyConfigAndArgsToModel = function(name) {
        var value = args[name]
        if (!args[name]) {
            if (conf && conf[name][0]) {
                value = conf[name][0].toString();
            }
        }
        model[name] = value;
    }
    
   copyConfigAndArgsToModel("maxPageItems");
    
   var height = args.height;
   if (!height)
   {
      height = "";
   }

    
   for each(var xmlFilter in conf..filter)
   {
      filters.push(
      {
          type: xmlFilter.@type.toString(),
          parameters: xmlFilter.@parameters.toString()
      });
   }
   model.filters = filters;

   var userIsSiteManager = true;
   if (page.url.templateArgs.site)
   {
      // We are in the context of a site, so call the repository to see if the user is site manager or not
      userIsSiteManager = false;
      var json = remote.call("/api/sites/" + page.url.templateArgs.site + "/memberships/" + encodeURIComponent(user.name));

      if (json.status == 200)
      {
         var obj = eval('(' + json + ')');
         if (obj)
         {
            userIsSiteManager = (obj.role == "SiteManager");
         }
      }
   }
   model.userIsSiteManager = userIsSiteManager;
}

main();