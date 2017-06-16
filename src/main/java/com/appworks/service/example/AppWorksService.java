/**
 * Copyright © 2017 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example;

import com.opentext.otag.sdk.client.v3.ServiceClient;
import com.opentext.otag.sdk.handlers.AWServiceContextHandler;
import com.opentext.otag.sdk.handlers.AWServiceStartupComplete;
import com.opentext.otag.sdk.types.v3.api.error.APIException;
import com.opentext.otag.sdk.types.v3.management.DeploymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is our "main" class in this AppWorks service, it contains the service startup method
 * ({@link AWServiceStartupComplete}) which at least one (and at most one) method in the service
 * must be annotated with, and hooks into the service lifecycle via its
 * {@link AWServiceContextHandler#onStart(java.lang.String)} implementation.
 */
// this class is never instantiated directly as AppWorks will create an instance of this for us
@SuppressWarnings("unused")
public class AppWorksService implements AWServiceContextHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AppWorksService.class);

    // mark this method as the one that completes the deployment
    @AWServiceStartupComplete
    @Override
    public void onStart(String appName) {
        startServiceAndCompleteDeployment(appName);
    }

    @Override
    public void onStop(String appName) {
        LOG.info("AppWorksService#onStop() called for \"" + appName + "\"");
    }

    private void startServiceAndCompleteDeployment(String appName) {
        LOG.info("AppWorksService#onStart() - initializing service \"" + appName + "\"");
        ServiceClient serviceClient = new ServiceClient();

        try {
            // as soon as onStart is called in any implementation of AWServiceContextHandler
            // we know it is safe to instantiate our SDK clients
            new ServiceBootstrapper().bootstrapService(appName);

            // make sure we let the Gateway know we have completed our startup
            serviceClient.completeDeployment(new DeploymentResult(true));
            LOG.info("AppWorksService#onStart() completed");
        } catch (Exception e) {
            reportDeploymentFailure(appName, serviceClient, e);
        }
    }

    private void reportDeploymentFailure(String appName, ServiceClient serviceClient, Exception e) {
        if (e instanceof APIException) {
            LOG.error("SDK call failed - {}", ((APIException) e).getCallInfo());
            throw new RuntimeException("Failed to report deployment outcome ", e);
        }
        try {
            // explicitly tell the Gateway we have failed
            serviceClient.completeDeployment(
                    new DeploymentResult("MyService deployment failed," + e.getMessage()));
            LOG.error(String.format("%s deployment failed", appName), e);
        } catch (APIException e1) {
            // API was unreachable
            throw new RuntimeException("Failed to report deployment outcome", e1);
        }
    }

}
