# Formtek Site Task Manager #
The Formtek Site Task Manager dashlet was developed for the 2012 Alfresco dashlet challenge by Dick Weisinger. The dashlet manages task assignment workflows within an Alfresco Share site.

![https://formtek-site-task-manager.googlecode.com/svn/wiki/images/SiteManagerDashlet.png](https://formtek-site-task-manager.googlecode.com/svn/wiki/images/SiteManagerDashlet.png)

## Site Task Manager Installation ##
```
1.  Download the file ftk-sitetaskmgr-dashlet0_01.jar from this site

2.  Make sure your Alfresco server is shut down

3.  Copy this same file to BOTH of the following locations in your Alfresco installation:

<ALFRESCOHOME>/tomcat/webapps/alfresco/WEB-INF/lib
and
<ALFRESCOHOME>/tomcat/webapps/share/WEB-INF/lib

4.  Restart the Alfresco server
```

## Site Task Manager Features ##

  * Tasks can be created and assigned within a Share site
  * Tasks and their status can be viewed by all site members
  * Tasks can be edited by site administrators, administrators, and the initiator
  * Each task corresponds to a 3-step Activiti workflow [figure below](see.md)
  * Each task/workflow is associated with a site, so each site has its own set of tasks
  * All columns of the task list are sortable
  * The task list results are paginated by a configurable number of rows
  * Site members with edit access can cancel, transition, and edit task metadata
  * Task list items can be filtered by task state, priority, and overdue
  * Task list makes full use of YUI2 datatables -- Resize, Reorder and Hide columns

## Site Task Workflow ##

![https://formtek-site-task-manager.googlecode.com/svn/wiki/images/SiteTaskMgr.png](https://formtek-site-task-manager.googlecode.com/svn/wiki/images/SiteTaskMgr.png)

## Site Task Manager Requirements ##
  * Developed for Alfresco Share 4.0.X
  * Developed for Google Chrome with minimal testing on IE9 and FireFox 14

## Formtek Alfresco Share Extensions ##
Additional Alfresco Share extensions are available from Formtek.  More information about the extensions can be found at this [page](http://www.formtek.com/products/alfresco_extensions.shtml).  For updates of our extensions, follow [@Formtek\_Inc](https://twitter.com/Formtek_inc) and [@DickAtFormtek](https://twitter.com/DickAtFormtek) on Twitter.

### Alfresco 3.4 and 4.0 [Extensions by Formtek](http://www.formtek.com/products/alfresco_extensions.shtml) ###
  * **PDF Rendering** - Convert to PDF and download any Share document [Free](Free.md)
  * **Peer Association** - Establish references between Share documents
  * **File Linking** - Create a link to a document residing in another location
  * **Auditing** - Track and display detailed document audit information
  * **Version Browser** - Preview older versions and their metadata
  * **Security** - Enable fine-grained access controls on Share site documents