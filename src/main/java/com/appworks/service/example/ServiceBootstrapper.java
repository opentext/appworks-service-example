package com.appworks.service.example;

import com.appworks.service.example.services.MailerService;
import com.appworks.service.example.services.PushNotificationService;
import com.appworks.service.example.services.SettingsService;
import com.appworks.service.example.services.TrustedProviderService;
import com.opentext.otag.sdk.client.v3.GatewayClientRegistry;
import com.opentext.otag.sdk.client.v3.RuntimesClient;
import com.opentext.otag.sdk.types.v3.api.error.APIException;
import com.opentext.otag.sdk.types.v3.apps.Runtime;
import com.opentext.otag.sdk.types.v3.apps.Runtimes;
import com.opentext.otag.sdk.types.v3.settings.Setting;
import com.opentext.otag.service.context.components.AWComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service bootstrap class, initializes the AppWorks related services, injecting them into the
 * {@link AWComponentContext} so they can be used elsewhere in the service.
 * <p>
 * This class makes use of the {@link GatewayClientRegistry} to access the various AppWorks
 * API Clients, using them to construct our own components
 * ({@link com.opentext.otag.service.context.components.AWComponent}).
 * <p>
 * It is also responsible for creating the required configuration {@link Setting} for this service.
 */
public class ServiceBootstrapper extends GatewayClientRegistry.RegistryUser {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceBootstrapper.class);

    public void bootstrapService(String appName) {
        // setup our Gateway clients, they will be accessible via the component context
        // once init completes
        GatewayClientRegistry.init();
        // ensure this AppWorks Services's settings are recorded at the Gateway
        initialiseServiceSettings(appName);
        // construct our own services (AppWorksComponents) making each available to the entire
        // service, we only have a few to demonstrate some of the SDK
        initialiseServiceComponents();
        listKnownRuntimes();
    }

    /**
     * Initialise our {@code SettingsService AppWorksComponent} and ask it to create our
     * {@code Settings} for the service. These are usually service configuration and
     * can be updated at the Gateways admin console.
     *
     * @param appName the app name, as told to us by the SDK
     */
    private void initialiseServiceSettings(String appName) {
        LOG.info("Starting SettingsService");
        SettingsService settingsService = new SettingsService(gatewayClients().getSettingsClient());
        AWComponentContext.add(settingsService);

        // initialise the Setting
        settingsService.createServiceSettings(appName);
    }

    /**
     * Create our services that use SDK clients, we use the convenient GatewayRegistry
     * as this class is granted access to the full suite as a
     * {@link GatewayClientRegistry.RegistryUser}.
     */
    private void initialiseServiceComponents() {
        LOG.info("Starting PushNotificationService");
        PushNotificationService pushNotificationService = new PushNotificationService(
                gatewayClients().getNotificationsClient(),
                gatewayClients().getRuntimesClient());

        LOG.info("Starting MailerService");
        MailerService mailerService = new MailerService(gatewayClients().getMailClient());

        LOG.info("Starting TrustedProviderService");
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
            LOG.info("Known Runtimes:");
            LOG.info("The Gateway knows about " + runtimes.size() + " Runtimes");
            runtimes.forEach(runtime -> LOG.info("- " + runtime));

        } catch (APIException e) {
            String errMsg = String.format("Runtimes retrieval call failed - %s", e.getCallInfo());
            LOG.error(errMsg, e);
        }
    }


}
