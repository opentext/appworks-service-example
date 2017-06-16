/**
 * Copyright © 2017 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example.services;

import com.opentext.otag.sdk.client.v3.MailClient;
import com.opentext.otag.sdk.types.v3.MailRequest;
import com.opentext.otag.sdk.types.v3.MailResult;
import com.opentext.otag.service.context.components.AWComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple AppWorks component that uses the SDK mail client to send an email, recording
 * the outcome of the request.
 */
public class MailerService implements AWComponent {

    private static final Logger LOG = LoggerFactory.getLogger(MailerService.class);

    private final MailClient mailClient;

    public MailerService(MailClient mailClient) {
        this.mailClient = mailClient;
    }

    /**
     * Sends an email via the Gateway and awaits the request outcome.
     *
     * @param mailRequest email request
     * @return true if the email was sent successfully, false otherwise
     */
    public boolean sendEmail(MailRequest mailRequest) {
        try {
            MailResult mailResult = mailClient.sendMail(mailRequest);
            if (mailResult.isSuccess()) {
                LOG.info("Successfully send email via OTAG service endpoint");
                LOG.info("MailResult message=" + mailResult.getMessage());
            } else {
                LOG.info("Failed to send email via OTAG service endpoint ");
                LOG.info("MailResult message=" + mailResult.getMessage());
            }
            return mailResult.isSuccess();
        } catch (Exception e) {
            LOG.error("Failed to send email via OTAG " +
                    "service endpoint, exception: " + e.getMessage(), e);
        }

        return false;
    }

}
