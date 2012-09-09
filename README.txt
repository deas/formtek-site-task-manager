Formtek Site Task Manager Dashlet
---------------------------------

Version .01  [Build 2]
September 8, 2012

Installation
------------

The Formtek Site Task Management dashlet is an add-on that was developed to target an Alfresco 4.0.2.X installation.

The project uses version 1.6 of the standard Alfresco add-on build script written and maintained by Will Abson.
This is an ant-based script, so the assumption is that Java and ant are installed on the build machine and available.
Building the Site Task Manager also requires a compile of some Java code which needs to link to the Alfresco API.

In the root level of the source, you will find the file build.properties.
Edit this file to change the location of the Alfresco SDK.  
You'll notice that from the default location, that the dashlet was built to target an Alfresco Share 4.0.2.X installation.

That should be all you need.
From the command line, simply type "ant".  No additional arguments are needed to do a simple build.
Other command-line build options are available.

If the build is successful, you should find the newly built jar file in the build/dist directory.
By default, this file will be named: ftk-sitetaskmgr-dashlet0_01.jar

Make sure your Alfresco server isn't running and then copy this jar file to the lib area in both the exploded Alfresco and Share war areas 
[with the assumption that you're using tomcat]:

<ALFRESCOHOME>/tomcat/webapps/alfresco/WEB-INF/lib
and
<ALFRESCOHOME>/tomcat/webapps/share/WEB-INF/lib

Restart the Alfresco server and the Alfresco Share application.

Using the Dashlet
-----------------

Navigate to the dashboard of your favorite site.
Click the "Customize Dashboard" button and drag the "Site Task Manager" dashlet into one of the columns on your dashboard layout.
Add the "Site Task Manager" in a prominent location on that site's dashboard.
You might best consider adding the dashlet to a column of the layout that has at least 2/3's of the dashboard width.

After doing that, you can start creating site tasks from the site dashboard.  
This can be done by clicking on the "New Task" link at the upper right of the dashlet.
After doing that, a create task dialog will pop up.  This dialog is likely familiar because it is based off of the standard Alfresco 
'create a workflow' dialog.  Fill in a name for the task and a person to assign the task to.  All other fields are optional.

The Site Task Manager associates tasks to a site.  Because of that, it is only possible to have one Site Task Manager dashlet per site.
[You could have multiple dashlets, but they'd be displaying the same information.]
Each site that the dashlet is installed into will uniquely track the tasks that were created in that site.

Site task workflows are different from standard Alfresco workflows because they are visible to all site members (or anyone who has
permissions to navigate into the site), not just to the initiator or to those people to whom tasks have been assigned.

There are 12 columns used in the site task list.  Because 12 columns can take a lot of real estate on the dashboard, the standard look
of the task list can be reconfigured.
You can resize, reorder and hide some of the columns to show only the information that most interests you, and by doing so, you can also 
shrink down the width of the task list in the dashlet.
The changes you make to the column size and placement are saved as your preferences and will be used the next time you visit the page.

There is a standard dashlet configuration dialog available from the upper right side of the dashlet.
When each site user clicks on that, they are able to configure their own preferences for how the task list should be displayed.
Available on that dialog is an option to select the number of rows per page and also to select which columns you would like to hide.
By default, the column called "Previous Comment" is hidden.  The previous comment is the comment made by the user on the last step of
the task workflow.

Note that tasks are basically in one of three states:  Assigned, Done or Archived.
Immediately after creating a task, it will be put into the Assigned state.
After the assignee completes the task, it will move into the Done state.
From the Done state, a site manager (or the task initiator) can send the task back to the assignee or move it on to the final task of Archive.
Once the task is archived, you can't do anything with it other than to view it or cancel it which will delete it from the site task list.

Note that the items displayed in the task list can be filtered.  For example, if you don't normally want to see the Archived tasks,
you can select to see only the Assigned and Done tasks, or only the Assigned tasks. The last value you select as a filter will
also be saved as your preference and remembered and reused the next time you visit this page.

For each row of the task list, actions are available and located to the right of the row.
Administrators, Site Managers and task Initiators will have the priviledge to edit the task.
The edit privilege allows these users to edit task properties, reassign the task to a different user, or to move the task to the next
step of the task workflow.  These users can also cancel a task, which from the perspective of the task list, effectively deletes it.
All site members are able to view tasks.

The Task name property on the row of each task contains an active link that will direct you to the standard Alfresco workflow details page.
Here you can see all the iterations that the task has gone through and the comments associated with each step.
You are also able to look at the workflow diagram  and see the current position of the workflow within the task flow.

The Dashlet Design
------------------

The dashlet was designed to try to reuse as much of standard Alfresco capabilities as possible rather than to try to integrate 
third party extensions.

Some of the elements of the dashlet construction includes:

- Standard Alfresco Dashlet/Surf with help and configuration dialog
- YUI2 controls and event handling, particularly as related to the YUI datatable
- Web Scripts, with both Javascript and Java-backed controllers
- Extension of the Alfresco Javascript API with a custom Javascript method
- Activiti workflow
- Alfresco forms
- Alfresco user preferences
- Standard Alfresco add-on build script

You can read more about the project at the project's home page: http://code.google.com/p/formtek-site-task-manager/


