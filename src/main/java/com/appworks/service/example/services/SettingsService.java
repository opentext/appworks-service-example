/**
 * Copyright © 2017 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example.services;

import com.appworks.service.example.ServiceConstants;
import com.opentext.otag.sdk.client.v3.SettingsClient;
import com.opentext.otag.sdk.types.v3.api.error.APIException;
import com.opentext.otag.sdk.types.v3.settings.Setting;
import com.opentext.otag.sdk.types.v3.settings.SettingType;
import com.opentext.otag.service.context.components.AWComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * AppWorks component that adds this services {@link Setting}s to the Gateway
 * if they don't already exist. It can retrieve {@link Setting}s via their key too.
 */
public class SettingsService implements AWComponent {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsService.class);

    private final SettingsClient settingsClient;

    public SettingsService(SettingsClient settingsClient) {
        this.settingsClient = settingsClient;
    }

    /**
     * Grab a Setting via its key.
     *
     * @param key Setting key
     * @return a Setting or null
     */
    public Optional<Setting> getSetting(String key) {
        Setting setting;
        try {
            setting = settingsClient.getSetting(key);
        } catch (APIException e) {
            if (e.getStatus() == 404) {
                LOG.debug("Setting not found for key {}", key);
            } else {
                LOG.error("We failed to find setting for key {} - {}", e.getCallInfo());
            }
            return Optional.empty();
        }

        return setting == null ? Optional.empty() : Optional.of(setting);
    }

    /**
     * Create our services required settings.
     *
     * @param appName name of this service
     */
    public void createServiceSettings(String appName) {
        createConfigSetting(appName, ServiceConstants.OUR_STRING_SETTING_KEY,
                ServiceConstants.SETTING_TEXT, "A String Config", SettingType.string, "DEFAULT VALUE!!!");
        createConfigSetting(appName, ServiceConstants.OUR_NUMBER_SETTING_KEY,
                "999", "A Numeric Config", SettingType.integer, "0");
        createConfigSetting(appName, ServiceConstants.OUR_BOOL_SETTING_KEY,
                "true", "A Boolean Config", SettingType.bool, "false");
        createConfigSetting(appName, ServiceConstants.OUR_JSON_SETTING_KEY,
                ServiceConstants.SOME_JSON_CONTENT, "A JSON Config", SettingType.json, ServiceConstants.SOME_JSON_CONTENT);
    }

    /**
     * Create a configuration setting via the SDK.
     *
     * @param appName      the name of this service, ensures we associate the setting with the correct service
     * @param key          setting key (id)
     * @param value        the setting value
     * @param label        the setting label
     * @param type         setting type
     * @param defaultValue default value
     */
    private void createConfigSetting(String appName, String key, String value, String label, SettingType type, String defaultValue) {
        Optional<Setting> retrieved = getSetting(key);
        if (!retrieved.isPresent()) {
            // construct a new setting
            Setting ourSetting = new Setting(key, appName, type, label,
                    value, defaultValue, label, false /* readOnly  */, null /* seqNo */);

            LOG.info(String.format("Creating new Setting - %s", ourSetting));
            try {
                settingsClient.createSetting(ourSetting);
            } catch (APIException e) {
                LOG.error("We failed to create setting for key {} - {}", key, e.getCallInfo());
            }
        } else {
            LOG.info(String.format("Setting already existed we wont add it again - %s", retrieved.get()));
        }
    }

}