/**
 * Copyright © 2016 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example.handlers;

import com.opentext.otag.sdk.handlers.AbstractLifecycleChangeHandler;
import com.opentext.otag.sdk.types.v3.MailRequest;
import com.opentext.otag.sdk.types.v3.message.LifecycleChangeMessage;
import com.opentext.otag.service.context.components.AWComponentContext;
import com.appworks.service.example.services.MailerService;
import com.appworks.service.example.util.ServiceLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This type of AppWorks service component handles messages sent from the Gateway at
 * key points in a managed deployments lifecycle. We demonstrate sending a test email
 * via our AppWorks component {@link MailerService}. Note the Gateways SMTP settings
 * should be configured to send a real email.
 */
// our constructor is never used directly as AppWorks will create an instance of this for us
@SuppressWarnings("unused")
public class AppLifecycleManager extends AbstractLifecycleChangeHandler {

    public static final Logger LOG = LoggerFactory.getLogger(AppLifecycleManager.class);

    @Override
    public void onInstall(LifecycleChangeMessage otagMessage) {
        ServiceLogger.info(LOG, "Called myService onInstall " + otagMessage.getEvent());
    }

    @Override
    public void onChangeVersion(LifecycleChangeMessage otagMessage) {
        ServiceLogger.info(LOG, "Called myService onUpgrade " + otagMessage.getEvent());
        // do something
        sendUpgradeNoticeEmail();
    }

    @Override
    public void onUninstall(LifecycleChangeMessage otagMessage) {
        ServiceLogger.info(LOG, "Called myService onUninstall " + otagMessage.getEvent());
    }

    /**
     * Use our {@link MailerService} to send an email regarding the upgrade notice.
     */
    private void sendUpgradeNoticeEmail() {
        MailRequest mailRequest = new MailRequest("admin@myservice.com", getToList(25),
                "MyService Upgrade Alert", "MyService has been upgraded by the otag admin");

        MailerService mailerService = AWComponentContext.getComponent(MailerService.class);
        if (mailerService == null) throw new RuntimeException("Could not get MailerService");
        mailerService.sendEmail(mailRequest);
    }

    private List<String> getToList(int addresses) {
        List<String> toAddresses = new ArrayList<>(addresses);
        for (int i = 0; i < addresses; i++) {
            toAddresses.add("testRecipient" + i + "@yourcompany.com");
        }
        return toAddresses;
    }

}
