## Ready! API/SoapUI SwaggerHub Plugin

Welcome to the Ready! API/SoapUI plugin for SwaggerHub. This plugin allows you to:

*import a Swagger definition from SwaggerHub
*publish an API from Ready! API to SwaggerHub

Note: the instructions below are for Ready! API but the plugin is also available in SoapUI and works in a similar fashion.

###Importing an API from SwaggerHub

There are two ways to bring an API into Ready! API from SwaggerHub:

* Create New Project: 
  * From Ready! API's main menu, select *File > New Project*.
  * In the subsequent dialog, select *Create new project from* and then choose *SwaggerHub* from the drop-down.
* Add to Existing Project:
  * In the Navigator view, right-click the desired project.
  * Select *Import from SwaggerHub* from the context menu.
 
In SoapUI, you import APIs from SwaggerHub to existing projects. To do this, right-click your project in the Navigator and select *Import from SwaggerHub* from the context menu.
  
After you choose SwaggerHub as the source for the API definition, a dialog box displays with the list of APIs available 
in the SwaggerHub Registry. If you want to narrow down the list, enter a search string in the Query box and click Search.
When you select an API its available versions are shown in the list below - select which version you want to import and 
press the OK button at the bottom of the dialog, the corresponding Swagger definition will be read from SwaggerHub and 
imported as an API into your project. You can then use those APIs to generate functional, load, and security tests. 
You can also use them as the basis for creating virtual APIs.

###Publishing an API to SwaggerHub

When you have completed your testing and are ready to publish the API in the SwaggerHub Registry, select your API in the Navigator, right-click it and choose *Publish to SwaggerHub* from the context menu.  You will then need to specify the API parameters for publishing::

* Owner - name of the API owner as you would like it to display in the Registry
* Name - name of the API; the API Name must be at least 3 aphanumeric characters (periods, hyphens, and underscores are also allowed)
* Version - version number you want to associate with the API in SwaggerHub
* API Key - you can generate this key in your User Settings in SwaggerHub
* Actions to take: Save API Key and Open in Browser after publishing

After you specified the parameter values, click *OK*. Ready! API (or SoapUI) will export your API definition to SwaggerHub.



