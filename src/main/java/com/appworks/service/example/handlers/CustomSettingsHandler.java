/**
 * Copyright © 2017 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example.handlers;

import com.appworks.service.example.ServiceConstants;
import com.appworks.service.example.services.PushNotificationService;
import com.appworks.service.example.services.SettingsService;
import com.opentext.otag.sdk.handlers.AbstractMultiSettingChangeHandler;
import com.opentext.otag.sdk.types.v3.api.error.APIException;
import com.opentext.otag.sdk.types.v3.message.SettingsChangeMessage;
import com.opentext.otag.sdk.types.v3.settings.Setting;
import com.opentext.otag.service.context.components.AWComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * An example of a multi setting change listener. This AppWorks Service has 4 system
 * settings it is concerned with in this class. This type listens to changes to all of them,
 * allowing us to react to changes made to them in the Gateway administration console all in one place.
 * <p>
 * When we do detect a change then we send a dummy push notification and check the updated
 * setting value. We do this using two services we created that make use of the SDK API clients.
 */
// our constructor is never used directly as AppWorks will create an instance of this for us
@SuppressWarnings("unused")
public class CustomSettingsHandler extends AbstractMultiSettingChangeHandler {

    public static final Logger LOG = LoggerFactory.getLogger(CustomSettingsHandler.class);

    public CustomSettingsHandler() {
        // Add the handlers for our keys on construction, taken care of by base class
        // we pass it the our own handling method defined below
        addHandler(ServiceConstants.OUR_STRING_SETTING_KEY, this::onSettingChanged);
        addHandler(ServiceConstants.OUR_BOOL_SETTING_KEY, this::onSettingChanged);
        addHandler(ServiceConstants.OUR_NUMBER_SETTING_KEY, this::onSettingChanged);
        addHandler(ServiceConstants.OUR_JSON_SETTING_KEY, this::onSettingChanged);
    }

    /**
     * Return the keys for our Services Settings. This tells the service manager what
     * we are interested in.
     *
     * @return all the key were are interested in
     */
    @Override
    public Set<String> getSettingKeys() {
        return new HashSet<>(
                Arrays.asList(ServiceConstants.OUR_STRING_SETTING_KEY,
                        ServiceConstants.OUR_NUMBER_SETTING_KEY,
                        ServiceConstants.OUR_BOOL_SETTING_KEY,
                        ServiceConstants.OUR_JSON_SETTING_KEY));
    }

    /**
     * Compare the new value we received in the handler with the value the Setting client can
     * now retrieve.
     *
     * @param message the change message passed to us from the Gateway
     */
    private void onSettingChanged(SettingsChangeMessage message) {
        LOG.info("New " + message.getKey() + " value=" + message.getNewValue());

        try {
            // issue a test push notification to some hardcoded users
            sendNotificationRegardingChange(message);
            // check that the value we were given is still the correct value by asking the Gateway via our SettingService
            verifySettingUpdate(message);
        } catch (APIException e) {
            LOG.warn("We failed to send notification regarding message update - {}", e.getCallInfo());
        }
    }

    private void sendNotificationRegardingChange(SettingsChangeMessage message) throws APIException {
        PushNotificationService pushNotificationService = AWComponentContext.getComponent(PushNotificationService.class);
        if (pushNotificationService != null) {
            String toSend = "MyService Setting " + message.getKey() + " was updated to " + message.getNewValue();
            pushNotificationService.sendTestMessage(toSend);
        } else {
            LOG.warn("Unable to send push notification, we failed to resolve PushNotificationService");
        }

    }

    private void verifySettingUpdate(SettingsChangeMessage message) {
        SettingsService settingsService = AWComponentContext.getComponent(SettingsService.class);
        if (settingsService != null) {
            Optional<Setting> settingOptional = settingsService.getSetting(message.getKey());
            settingOptional.ifPresent(setting ->
                    LOG.info("Actual value was " + setting.getValue()));
        } else {
            LOG.warn("Unable to verify setting change, we failed to resolve the SettingService.");
        }
    }

}
