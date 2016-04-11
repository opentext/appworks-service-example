/**
 * Copyright © 2016 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example.util;

import org.slf4j.Logger;

/**
 * Crude log line decorator that wraps a SLF4J logger bound to log4j. Makes it easy for us to spot
 * entries from this service in the log output.
 */
public class ServiceLogger {

    public static final String LOG_MARKER = "***** MY-EXAMPLE-SERVICE ****** - {}";

    public static void info(Logger logger, String message) {
        logger.info(LOG_MARKER, message);
    }

    public static void debug(Logger logger, String message) {
        logger.debug(LOG_MARKER, message);
    }

    public static void error(Logger logger, String message, Throwable t) {
        logger.error(LOG_MARKER, message, t);
    }

    public static void error(Logger logger, String message) {
        logger.error(LOG_MARKER, message);
    }

}
