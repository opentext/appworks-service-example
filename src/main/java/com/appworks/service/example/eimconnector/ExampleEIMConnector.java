/**
 * Copyright © 2016 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example.eimconnector;

import com.opentext.otag.sdk.connector.EIMConnectorService;
import com.opentext.otag.sdk.handlers.AbstractSettingChangeHandler;
import com.opentext.otag.sdk.handlers.AuthRequestHandler;
import com.opentext.otag.sdk.types.v3.TrustedProvider;
import com.opentext.otag.sdk.types.v3.message.SettingsChangeMessage;
import com.opentext.otag.sdk.types.v3.sdk.EIMConnector;
import com.opentext.otag.service.context.components.AWComponentContext;
import com.appworks.service.example.services.TrustedProviderService;
import com.appworks.service.example.util.ServiceLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * EIMConnectors give us an easy way to achieve a few things.
 * <ul>
 * <li>
 * Shared URL endpoint so multiple AppWorks Services can collaborate with a single EIM
 * service endpoint via a centrally registered AppWorks
 * {@link com.opentext.otag.sdk.types.v3.settings.Setting}.
 * </li>
 * <li>
 * Configure {@link TrustedProvider} access to the AppWorks Gateway a backing EIM service.
 * </li>
 * </ul>
 * <p>
 * Via an embeddable {@link AuthRequestHandler} it can also:
 * <ul>
 * <li>
 * Provide auth response decoration, adding response body properties or cookie.
 * </li>
 * <li>
 * Handle OTDS Resource registration, if permitted the Gateway can peek into the EIM services
 * user partition to identify users that are coming in via the Gateway.
 * </li>
 * </ul>
 *
 * This particular service creates it shared connection URL via the app.properties file
 * that is included. We have to make sure our {@link #getConnectionStringSettingKey()} matches!
 */
public class ExampleEIMConnector extends AbstractSettingChangeHandler implements EIMConnectorService {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleEIMConnector.class);

    /**
     * Name must be unique across the Gateway, else it could be rejected on creation.
     */
    public static final String CONNECTOR_NAME = "OurFakeEIMConnector";

    /**
     * Current version number. We are still restricted to only a single connector instance
     * per given based on the name.
     */
    public static final String CONNECTOR_VERSION = "1.0.0";

    /**
     * The actual key for the {@link com.opentext.otag.sdk.types.v3.settings.Setting}
     * that will be created.
     *
     * THIS MUST MATCH THE NAME OF THE SETTING WE CREATE IN app.proprties
     */
    public static final String CONNECTION_STRING_SETTING_KEY = "myService.eimconnectorurl";

    /**
     * Our connection string, it will be visible in the Gateway UI.
     */
    private String eimConnectionUrl;

    // we implement the settings change management to listen for updates made to our connection
    // URL at the Gateway

    @Override
    public String getSettingKey() {
        return getConnectionStringSettingKey();
    }

    @Override
    public void onSettingChanged(SettingsChangeMessage message) {
        String newValue = message.getNewValue();
        ServiceLogger.info(LOG, "EIM Connection URL was updated to " + newValue);
        eimConnectionUrl = newValue;
    }

    @Override
    public String getConnectorName() {
        return CONNECTOR_NAME;
    }

    @Override
    public String getConnectorVersion() {
        return CONNECTOR_VERSION;
    }

    @Override
    public String getConnectionString() {
        // store the value in field
        return eimConnectionUrl;
    }

    @Override
    public String getConnectionStringSettingKey() {
        return CONNECTION_STRING_SETTING_KEY;
    }

    /**
     * We use the id of the {@link com.opentext.otag.sdk.types.v3.TrustedProvider}
     * we are already using with our example trusted provider service.
     *
     * @return trusted provider name
     * @see TrustedProviderService
     */
    @Override
    public String getTrustedProviderName() {
        return TrustedProviderService.TRUSTED_PROVIDER_NAME;
    }

    /**
     * Use the {@link TrustedProviderService} to get the key it has for our
     * provider.
     *
     * @return the provider key or null if we cannot access this data
     */
    @Override
    public String getTrustedProviderKey() {
        TrustedProviderService providerService = getTrustedProviderService();

        if (providerService != null) {
            Optional<TrustedProvider> providerOpt = providerService.getMyServiceTrustedProvider();
            if (providerOpt.isPresent())
                return providerOpt.get().getKey();
        }
        return null;
    }

    @Override
    public boolean registerTrustedProviderKey(String serverName, String key) {
        // This is where you should implement the call to your service to
        // pass the trusted provider key value, this service can then
        // use the key to use some of the Gateways APIs.
        return false;
    }

    @Override
    public AuthRequestHandler getAuthHandler() {
        // grab our AuthHandler implementation from the context, we have implemented it in a
        // different class, ExampleAuthHandler
        return AWComponentContext.getComponent(ExampleAuthHandler.class);
    }

    /**
     * Called when an administrator changes the connector.
     *
     * @param updated connector
     */
    @Override
    public void onUpdateConnector(EIMConnector updated) {
        // As the trusted server key for instance can be refreshed in the Gateway admin
        // dashboard we need to take care of that . In this example we could react to the
        // those changes and let our collaborating EIM service know.
        String trustedProviderKey = getTrustedProviderKey();
        registerTrustedProviderKey(TrustedProviderService.TRUSTED_PROVIDER_NAME, trustedProviderKey);
    }

    private TrustedProviderService getTrustedProviderService() {
        return AWComponentContext.getComponent(TrustedProviderService.class);
    }

}
