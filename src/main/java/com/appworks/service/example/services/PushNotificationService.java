/**
 * Copyright © 2016 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example.services;

import com.opentext.otag.sdk.client.v3.NotificationsClient;
import com.opentext.otag.sdk.client.v3.RuntimesClient;
import com.opentext.otag.sdk.types.v3.api.SDKResponse;
import com.opentext.otag.sdk.types.v3.api.error.APIException;
import com.opentext.otag.sdk.types.v3.apps.Runtimes;
import com.opentext.otag.sdk.types.v3.notification.ClientPushNotificationRequest;
import com.opentext.otag.sdk.types.v3.notification.GeneralPayload;
import com.opentext.otag.sdk.types.v3.apps.Runtime;
import com.opentext.otag.service.context.components.AWComponent;
import com.appworks.service.example.util.ServiceLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to demonstrate the use of the Gateways push notification API. A custom
 * component that can be added to the shared registry.
 */
public class PushNotificationService implements AWComponent {

    private static final Logger LOG = LoggerFactory.getLogger(PushNotificationService.class);

    private NotificationsClient notificationsClient;
    private RuntimesClient runtimesClient;

    public PushNotificationService(NotificationsClient notificationsClient,
                                   RuntimesClient runtimesClient) {
        this.notificationsClient = notificationsClient;
        this.runtimesClient = runtimesClient;
    }

    /**
     * Send a test push notification via the Gateway.
     *
     * @param message to send
     */
    public void sendTestMessage(String message) {
        try {
            Set<String> runtimes = getRuntimes();
            // log the Runtimes the Gateway knew about
            runtimes.stream()
                    .forEach(s -> ServiceLogger.info(LOG, "Retrieved runtime " + s));

            // this is the default data payload, it gets passed to the client and potential
            // AppWorks apps if we supplied a target rather than just a summary
            GeneralPayload payload = new GeneralPayload(message);

            // this request will be accepted by the Gateway but will not succeed, the request
            // recipients in particular wont be eligible to receive a notification
            ClientPushNotificationRequest testRequest = new ClientPushNotificationRequest.Builder()
                    .title("Push notification from MyService")
                    .summary(message)
                    .addClient("dummyClientId")
                    .addUser("rhyse")
                    .addGroup("otagadmins").addGroup("otadmins")
                    .runtimes(runtimes)
                    .data(payload.asMap())
                    .build();

            ServiceLogger.info(LOG, String.format("Sending test push notification - %s", testRequest));

            SDKResponse sdkResponse = notificationsClient.sendPushNotification(testRequest);
            LOG.info("Push notification sent successfully = {}", sdkResponse.isSuccess());
        } catch (APIException e) {
            LOG.error("Failed to send test message, SDK call failed - {}", e.getCallInfo());
            throw new RuntimeException(e);
        }
    }

    private Set<String> getRuntimes() throws APIException {
        Runtimes allRuntimes = runtimesClient.getAllRuntimes();
        List<String> runtimeNames = allRuntimes.getRuntimes().stream()
                .map(Runtime::getName)
                .collect(Collectors.toList());
        runtimeNames.forEach(name ->
                LOG.info("Adding Runtime {} to the request", name));
        return new HashSet<>(runtimeNames);
    }

}
