## Ready! API SwaggerHub Plugin

Welcome to the Ready! API plugin for SwaggerHub. This plugin allows you to:

*import a Swagger definition from SwaggerHub
*publish an API from Ready! API to SwaggerHub

###Importing an API from SwaggerHub

There are two ways to bring an API into Ready! API from SwaggerHub:

* Create New Project: 
  * In the Projects tab, select New.
  * choose "SwaggerHub" from the drop-down list in the dialog box.
* Add to Existing Project:
  * Select an existing project.
  * Right-click and select Import From SwaggerHub from the menu
  
After you choose SwaggerHub as the source for the API definition, a dialog box displays with the list of APIs available in the SwaggerHub Registry. You can choose one or more APIs to import. If you want to narrow down the list, enter a search string in the Query box and click Search. 

After you select the APIs to import, Ready! API reads in the Swagger definition and creates the API in your Projects tab. You can then use those APIs to generate functional, load, and security tests. You can also use them as the basis for creating virtual APIs.

###Publishing an API to SwaggerHub

When you have completed your testing and are ready to publish the API in the SwaggerHub Registry, just select the API from your project and right-click on it. Choose Publish to SwaggerHub. 

A dialog box displays asking for the required information for publishing the API to the SwaggerHub Registry:

* Owner - name of the API owner as you would like it to display in the Registry
* Name - name of the API
* Version - version number you want to associate with the API in SwaggerHub
* API Key
* Actions to take: Save API Key and Open in Browser after publishing



