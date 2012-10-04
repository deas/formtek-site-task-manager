/**
 * Site Task Manager configuration component POST method
 *   Values received from client via POST are saved in Alfresco sitedata
 */

function main()
{
   var c = sitedata.getComponent(url.templateArgs.componentId);

   var saveValue = function(name, value) {
       c.properties[name] = value;
       model[name] = value; 
   }

   var saveValueIgnoreEmpty = function(name, value) {
       if (value) {
           saveValue(name, value);
       }
   }
   
   saveValue("maxPageItems", String(json.get("maxPageItems")));
   
   c.save();
}

main();
