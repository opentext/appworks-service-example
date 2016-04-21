/**
 * Copyright © 2016 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example;

import com.appworks.service.example.eimconnector.ExampleAuthHandler;
import com.appworks.service.example.eimconnector.ExampleEIMConnector;
import com.appworks.service.example.services.MailerService;
import com.appworks.service.example.services.PushNotificationService;
import com.appworks.service.example.services.SettingsService;
import com.appworks.service.example.services.TrustedProviderService;
import com.appworks.service.example.util.ServiceLogger;
import com.opentext.otag.sdk.client.v3.AuthClient;
import com.opentext.otag.sdk.client.v3.GatewayClientRegistry;
import com.opentext.otag.sdk.client.v3.RuntimesClient;
import com.opentext.otag.sdk.client.v3.ServiceClient;
import com.opentext.otag.sdk.handlers.AWServiceContextHandler;
import com.opentext.otag.sdk.handlers.AWServiceStartupComplete;
import com.opentext.otag.sdk.types.v3.api.error.APIException;
import com.opentext.otag.sdk.types.v3.apps.Runtime;
import com.opentext.otag.sdk.types.v3.apps.Runtimes;
import com.opentext.otag.sdk.types.v3.auth.AuthHandler;
import com.opentext.otag.sdk.types.v3.auth.RegisterAuthHandlersRequest;
import com.opentext.otag.sdk.types.v3.management.DeploymentResult;
import com.opentext.otag.sdk.types.v3.sdk.EIMConnector;
import com.opentext.otag.sdk.types.v3.settings.Setting;
import com.opentext.otag.service.context.components.AWComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This is our "main" class in this AppWorks service, it contains the service startup method
 * ({@link AWServiceStartupComplete}) which at least one (and at most one) method in the service
 * must be annotated with, and hooks into the service lifecycle via its
 * {@link AWServiceContextHandler#onStart(java.lang.String)} implementation.
 * <p>
 * This class makes use of the {@link GatewayClientRegistry} to access the various AppWorks
 * API Clients, using them to construct our own components
 * ({@link com.opentext.otag.service.context.components.AWComponent}).
 * <p>
 * It is also responsible for creating the required configuration {@link Setting} for this service.
 */
// this class is never instantiated directly as AppWorks will create an instance of this for us
@SuppressWarnings("unused")
public class AppWorksService extends GatewayClientRegistry.RegistryUser implements AWServiceContextHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AppWorksService.class);

    // mark this method as the one that completes the deployment
    @AWServiceStartupComplete
    @Override
    public void onStart(String appName) {
        boostrapService(appName);
    }

    @Override
    public void onStop(String appName) {
        ServiceLogger.info(LOG, "AppWorksService#onStop() called for \"" + appName + "\"");
    }

    private void boostrapService(String appName) {
        ServiceLogger.info(LOG, "AppWorksService#onStart() - initializing service \"" + appName + "\"");
        ServiceClient serviceClient = new ServiceClient();

        try {
            // as soon as onStart is called in any implementation of AWServiceContextHandler
            // we know it is safe to instantiate our SDK clients
            initialiseService(appName);
            // make sure we let the Gateway know we have completed our startup
            serviceClient.completeDeployment(new DeploymentResult(true));
            ServiceLogger.info(LOG, "AppWorksService#onStart() completed");
        } catch (Exception e) {
            if (e instanceof APIException) {
                LOG.error("SDK call failed - {}", ((APIException) e).getCallInfo());
                throw new RuntimeException("Failed to report deployment outcome ", e);
            }
            try {
                // explicitly tell the Gateway we have failed
                serviceClient.completeDeployment(
                        new DeploymentResult("MyService deployment failed," + e.getMessage()));
                ServiceLogger.error(LOG, String.format("%s deployment failed", appName), e);
            } catch (APIException e1) {
                // API was unreachable
                throw new RuntimeException("Failed to report deployment outcome", e1);
            }
        }
    }

    private void initialiseService(String appName) {
        // setup our Gateway clients, they will be accessible via the component context
        // once init completes
        GatewayClientRegistry.init();
        // ensure this AppWorks Services's settings are recorded at the Gateway
        initialiseServiceSettings(appName);
        // construct our own services (AppWorksComponents) making each available to the entire
        // service, we only have a few to demonstrate some of the SDK
        initialiseServiceComponents();

        listKnownRuntimes();

        // setup the more advanced handler features we provide examples of
        registerEIMConnector(gatewayClients().getServiceClient());
        registerAuthHandler(gatewayClients().getAuthClient());
    }

    private void registerEIMConnector(ServiceClient serviceClient) {
        ExampleEIMConnector connector = AWComponentContext.getComponent(ExampleEIMConnector.class);
        if (connector == null)
            throw new RuntimeException("Failed to register our EIM connector, it was not" +
                    "found in the AW component context???");

        try {
            // this constructor allows the service to provide the values we register with the Gateway
            // this type is what we present to the Gateway to represent the connector
            EIMConnector eimConnector = new EIMConnector(connector);
            serviceClient.registerConnector(eimConnector);
        } catch (APIException e) {
            throw new RuntimeException("Failed to register our connector with " +
                    "the Gateway - " + e.getCallInfo());
        }
    }

    /**
     * Register our custom auth handler with the Gateway.
     *
     * @param authClient SDK client used to make the registration call
     */
    private void registerAuthHandler(AuthClient authClient) {
        // the auth handler component should be registered for use automatically
        ExampleAuthHandler authHandler = AWComponentContext.getComponent(ExampleAuthHandler.class);
        if (authHandler == null)
            throw new RuntimeException("Failed to register our auth handler, it was not" +
                    "found in the AW component context???");

        AuthHandler handler = authHandler.buildHandler();
        RegisterAuthHandlersRequest request = new RegisterAuthHandlersRequest();
        // we use the buildHandler convenience method
        request.addHandler(authHandler.buildHandler());

        try {
            authClient.registerAuthHandlers(request);
        } catch (APIException e) {
            throw new RuntimeException("Failed to register our auth handler with " +
                    "the Gateway - " + e.getCallInfo());
        }

    }

    /**
     * Initialise our {@code SettingsService AppWorksComponent} and ask it to create our
     * {@code Settings} for the service. These are usually service configuration and
     * can be updated at the Gateways admin console.
     *
     * @param appName the app name, as told to us by the SDK
     */
    private void initialiseServiceSettings(String appName) {
        ServiceLogger.info(LOG, "Starting SettingsService");
        SettingsService settingsService = new SettingsService(gatewayClients().getSettingsClient());
        AWComponentContext.add(settingsService);

        // initialise the Setting
        settingsService.createServiceSettings(appName);
    }

    private void initialiseServiceComponents() {
        // create our services that use SDK clients, we use the convenient GatewayRegistry
        // as this class is granted access to the full suite as a
        // com.opentext.otag.sdk.client.v3.GatewayClientRegistry.RegistryUser
        ServiceLogger.info(LOG, "Starting PushNotificationService");
        PushNotificationService pushNotificationService = new PushNotificationService(
                gatewayClients().getNotificationsClient(),
                gatewayClients().getRuntimesClient());

        ServiceLogger.info(LOG, "Starting MailerService");
        MailerService mailerService = new MailerService(gatewayClients().getMailClient());

        ServiceLogger.info(LOG, "Starting TrustedProviderService");
        TrustedProviderService trustedProviderService = new TrustedProviderService(
                gatewayClients().getTrustedProviderClient());

        // throw them into the context for later use
        AWComponentContext.add(pushNotificationService, mailerService, trustedProviderService);
    }

    /**
     * List the {@link Runtime}s the AppWorks Gateway currently knows about. These
     * are typically mobile applications that use the Gateway. It can identify users
     * as being a user of a particular {@link Runtime}, which is useful for targeted push
     * notifications.
     *
     * @see PushNotificationService
     */
    private void listKnownRuntimes() {
        RuntimesClient runtimesClient = new RuntimesClient();

        try {
            Runtimes allRuntimes = runtimesClient.getAllRuntimes();
            List<Runtime> runtimes = allRuntimes.getRuntimes();
            ServiceLogger.info(LOG, "Known Runtimes:");
            ServiceLogger.info(LOG, "The Gateway knows about " + runtimes.size() + " Runtimes");
            runtimes.forEach(runtime -> ServiceLogger.info(LOG, "- " + runtime));

        } catch (APIException e) {
            String errMsg = String.format("Runtimes retrieval call failed - %s", e.getCallInfo());
            ServiceLogger.error(LOG, errMsg, e);
        }
    }

}
