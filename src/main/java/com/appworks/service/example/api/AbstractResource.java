/**
 * Copyright © 2017 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example.api;

import com.opentext.otag.sdk.client.v3.AbstractOtagServiceClient;
import com.opentext.otag.sdk.client.v3.AuthClient;
import com.opentext.otag.sdk.client.v3.GatewayClientRegistry;
import com.opentext.otag.sdk.client.v3.SettingsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.function.Supplier;

/**
 * Common behaviour for our JAX-RS resources.
 */
public abstract class AbstractResource extends GatewayClientRegistry.RegistryUser {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceSettingsResource.class);

    /**
     * Message in case a client hits the service before AppWorks has initialised the service.
     */
    public static final String SERVICE_IS_YET_TO_INITIALISE_ERR =
            "The service is yet to initialise, please try again soon";

    // Guard methods to ensure we can get the services we need

    protected SettingsClient getSettingsClient() {
        return getComponent(() -> gatewayClients().getSettingsClient());

    }

    protected AuthClient getAuthClient() {
        return getComponent(() -> gatewayClients().getAuthClient());
    }

    /**
     * Wrap calls to retrieve an SDK ({@link AbstractOtagServiceClient}) client.
     *
     * @return the SDK client
     * @throws WebApplicationException if we can't get the service we need
     */
    private <T extends AbstractOtagServiceClient> T getComponent(Supplier<T> getter) {
        try {
            return getter.get();
        } catch (Exception e) {
            LOG.error("Cannot get the required SDK client yet?", e);
            throw new WebApplicationException(SERVICE_IS_YET_TO_INITIALISE_ERR,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}
