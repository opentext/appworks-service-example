# AppWorks Service Example 16.2

The project contains example code for working with most, if not all, of the AppWorks Service platforms features.
 
The service can be built using this Maven project and will produce a zip (appworks-service-example_1.0.0.zip) that 
can be deployed to an AppWorks Gateway 16.2 instance. All you need to do is run `mvn clean package` to build the service.

Please review the pom.xml for the build process, but more importantly the service's code itself for information on 
how the AppWorks SDK works and how to build a minimal but functional AppWorks service.

## Service API

This service exposes a limited REST API using Jersey, once deployed and enabled the service should service the following URL 
with a standard initial response.

`GET http://{gatewayhost}/appworks-service-example/api/configuration` 

You should receive the following JSON in the response body.

```
[
    {
        "key": "our.setting.key",
        "value": "This is a test Setting provided by our example service on startup"
    },
    {
        "key": "our.number.setting.key",
        "value": 999
    },
    {
        "key": "our.boolean.setting.key",
        "value": true
    },
    {
        "key": "our.json.setting.key",
        "value": "{\"somefield\": \"Some value\"}"
    }
]
```

A number of unsecured endpoints are available in `com.appworks.service.example.api.ServiceSettingsResource` Jersey resource;
- GET all settings
- GET setting by key
- PUT update setting 
