/**
 * Copyright © 2017 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example.api;

import com.opentext.otag.sdk.client.v3.AuthClient;
import com.opentext.otag.sdk.client.v3.SettingsClient;
import com.opentext.otag.sdk.types.v3.api.error.APIException;
import com.opentext.otag.sdk.types.v3.settings.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.appworks.service.example.ServiceConstants.*;

/**
 * Simple example of a Jersey JAX-RS resource class. It asks the Gateway for its
 * Settings values (in a similar way to the admin UI) and relays them via a GET.
 * These are just some arbitrary example configuration settings.
 *
 * @see com.appworks.service.example.services.SettingsService
 * @see com.appworks.service.example.handlers.CustomSettingsHandler
 */
@Path("configuration")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceSettingsResource extends AbstractResource {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceSettingsResource.class);

    /**
     * Get the services configuration settings.
     *
     * @return response
     */
    // GET {base_url}/api/configuration
    @GET
    public Response getServiceConfig() {
        return getConfig();
    }

    /**
     * Get the services configuration settings validating the clients session with
     * the AppWorks Gateway via a header.
     *
     * @param req       request
     * @param otagToken the AppWorks Gateway session token
     * @return response
     */
    // GET {base_url}/api/configuration/secure/{key}
    @GET
    @Path("secure")
    public Response getServiceConfigSecurely(@Context HttpServletRequest req,
                                             @HeaderParam("otagtoken") String otagToken) {

        try {
            // use the Gateways authentication service to ensure the client has a valid session
            AuthClient authClient = getAuthClient();
            authClient.getUserForToken(otagToken);
        } catch (Exception e) {
            LOG.error("Rebuffed unauthorised access from I.P. " + req.getRemoteAddr());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return getConfig();
    }

    /**
     * Get a specific configuration setting by key.
     *
     * @param key setting key
     * @return response
     */
    // GET {base_url}/api/configuration/{key}
    @GET
    @Path("{key}")
    public Response getConfigByKey(@PathParam("key") String key) {
        Setting setting;
        try {
            setting = getSettingsClient().getSetting(key);

            if (setting == null)
                return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOG.error("Failed to retrieve configuration setting for key " + key, e);
            return Response.serverError().build();
        }

        return Response.ok(new MyImmutableDataObject(setting.getKey(), setting.getValue())).build();
    }

    /**
     * Update a specific configuration setting. We expect some JSON of the form:
     * <p>
     * {
     * "key": "{your_key}",
     * "value": "some value"
     * }
     *
     * @param key          setting key
     * @param updatedValue new value
     * @return response
     */
    // PUT {base_url}/api/configuration/{key}
    @PUT
    @Path("{key}")
    public Response updateConfigValue(@PathParam("key") String key,
                                      MyImmutableDataObject updatedValue) {
        // 400 BAD REQUEST for invalid input
        if (key == null || updatedValue == null || updatedValue.getKey() == null ||
                !key.equals(updatedValue.getKey()) || updatedValue.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Setting setting;
        try {
            SettingsClient settingsClient = getSettingsClient();
            setting = settingsClient.getSetting(key);

            if (setting == null) {
                LOG.error("Failed to find config setting for " + key);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            setting.setValue((String) updatedValue.getValue());
            // save the setting back to the store
            settingsClient.updateSetting(setting);
        } catch (Exception e) {
            String errMsg = "Failed to update configuration setting for key " + key +
                    " with new value " + updatedValue;
            // log the API call error if the SDK client failed
            if (e instanceof APIException)
                errMsg += " - SDK error - " + ((APIException) e).getCallInfo();

            LOG.error(errMsg, e);
            return Response.serverError().build();
        }

        // return the value with the settings new value
        return Response.ok(new MyImmutableDataObject(setting.getKey(), setting.getValue())).build();
    }

    private Response getConfig() {
        try {
            List<MyImmutableDataObject> returnList = getMyConfig();

            return Response.ok(returnList).build();
        } catch (Exception e) {
            LOG.error("Failed to retrieve the configuration for this service - " + e.getMessage(), e);
            return Response.serverError().build();
        }
    }

    private List<MyImmutableDataObject> getMyConfig() {
        SettingsClient settingsClient = getSettingsClient();
        List<MyImmutableDataObject> returnList = new ArrayList<>(3);

        addConfig(settingsClient, OUR_STRING_SETTING_KEY, returnList, Setting::getValue);

        addConfig(settingsClient, OUR_NUMBER_SETTING_KEY, returnList,
                setting -> Integer.valueOf(setting.getValue()));

        addConfig(settingsClient, OUR_BOOL_SETTING_KEY, returnList,
                setting -> Boolean.valueOf(setting.getValue()));

        addConfig(settingsClient, OUR_JSON_SETTING_KEY, returnList, Setting::getValue);

        return returnList;
    }

    private void addConfig(SettingsClient settingsClient,
                           String settingKey,
                           List<MyImmutableDataObject> returnList,
                           Function<Setting, Object> supplier) {
        Setting setting = null;
        try {
            setting = settingsClient.getSetting(settingKey);
        } catch (APIException e) {
            if (e.getStatus() == 404) {
                LOG.debug("Setting was not found for key {}", settingKey);
                return;
            }

            // print the SDK clients debugging response info
            LOG.warn("Error retrieving setting - {}", e.getCallInfo(), e);
        }

        // add the setting if we managed to retrieve one
        if (setting != null)
            returnList.add(new MyImmutableDataObject(settingKey, supplier.apply(setting)));
    }

}
