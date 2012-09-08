Formtek Site Task Manager Dashlet
---------------------------------

Version .01  [Build 2]
September 8, 2012

The project uses version 1.6 of the standard Alfresco add-on build script written and maintained by Will Abson.
This is an ant-based script, so the assumption is that Java and ant are installed on the build machine and available.
Building the Site Task Manager also requires a compile of some Java code which needs to link to the Alfresco API.

In the root level of the source, you will find the file build.properties.
Edit this file to change the location of the Alfresco SDK.  
You'll notice that from the default location, that the dashlet was built to target an Alfresco Share 4.0.2.X installation.

That should be all you need.
From the command line, simply type "ant".  No additional arguments are needed.

If the build is successful, you should find the newly built jar file in the build/dist directory.
By default, this file will be named: ftk-sitetaskmgr-dashlet0_01.jar

Make sure your Alfresco server isn't running and then copy this jar file to the lib area in both the exploded Alfresco and Share war areas 
[with the assumption that you're using tomcat]:

<ALFRESCOHOME>/tomcat/webapps/alfresco/WEB-INF/lib
and
<ALFRESCOHOME>/tomcat/webapps/share/WEB-INF/lib

Restart the Alfresco server and the Alfresco Share application.
Navigate to the dashboard of your favorite site and add the "Site Task Manager" in a prominent location on that site's dashboard.

Then, you can start creating site tasks.
You can read more about the project at the project's home page: http://code.google.com/p/formtek-site-task-manager/

Note that there are 12 columns used in the site task list.  That can be a lot and take a lot of real estate on the dashboard.
You can resize, reorder and hide some of the columns to show only the information that most interest you and which also shrinks down the width of the dashlet.
Resize, reorder and hide information will then be saved as your preferences.


