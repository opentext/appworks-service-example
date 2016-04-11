/**
 * Copyright © 2016 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example.eimconnector;

import com.opentext.otag.sdk.client.v3.AuthClient;
import com.opentext.otag.sdk.handlers.AbstractAuthRequestHandler;
import com.opentext.otag.sdk.handlers.AuthResponseDecorator;
import com.opentext.otag.sdk.types.v3.auth.AuthHandlerResult;
import com.opentext.otag.sdk.types.v3.client.ClientRepresentation;
import com.opentext.otag.sdk.util.Cookie;
import com.opentext.otag.sdk.util.ForwardHeaders;
import com.opentext.otag.service.context.components.AWComponentContext;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * An example showing how a service can decorate requests using auth provided
 * by a backing EIM service. Exposed to the Gateway via the
 * {@link com.opentext.otag.sdk.connector.EIMConnectorService}.
 * <p>
 * We extend {@link AbstractAuthRequestHandler} for its convenience methods
 * that help us register this handler with the managing Gateway. It won't
 * be used unless registered. We register the handler is the service
 * startup method usually (see {@link com.opentext.otag.sdk.handlers.AWServiceStartupComplete}).
 *
 * @see ExampleEIMConnector
 * @see com.appworks.service.example.AppWorksService#registerAuthHandler(AuthClient)
 */
@AuthResponseDecorator // decorate auth requests received at the AppWorks Gateway
public class ExampleAuthHandler extends AbstractAuthRequestHandler {

    /**
     * As we are a decorator we will be asked for input on a particular auth request,
     * we are given the chance to add cookies and additional properties.
     */
    private static final String OUR_COOKIE_NAME = "my-http-only-service-cookie";

    /**
     * Additional data will be delivered via the authentication response, so will
     * be available to clients.
     */
    private static final String AUTHED_BY_CREDS = "authedByCreds";

    /**
     * Auth request with credentials.
     *
     * @param username   user name
     * @param password   password x
     * @param headers    headers to pass on
     * @param clientData client data passed to the Gateway by the original call
     * @return auth result
     */
    @Override
    public AuthHandlerResult auth(String username, String password,
                                  ForwardHeaders headers, ClientRepresentation clientData) {
        // this is where we expect the backing EIM service to be called securely to
        // validate the supplied credentials

        // in reality we will need a login endpoint of a supporting system, this could
        // be managed by a Gateway config setting easily, we can just use our EOM connector
        // since we have one
        String connectionString = getAuthUrl();
        if (connectionString != null) {
            // make your authentication call here
        }

        AuthHandlerResult authHandlerResult = new AuthHandlerResult(true);
        // add some cookie and additional props
        authHandlerResult.addRootCookie(OUR_COOKIE_NAME, UUID.randomUUID().toString());
        authHandlerResult.addAdditionalProperty(AUTHED_BY_CREDS, true);
        return authHandlerResult;
    }

    /**
     * Auth request that uses a token. You should validate this token and
     * return a result.
     *
     * @param authToken  token to validate
     * @param headers    headers to pass on
     * @param clientData client data passed to the Gateway by the original call
     * @return auth result
     */
    @Override
    public AuthHandlerResult auth(String authToken, ForwardHeaders headers, ClientRepresentation clientData) {
        // this is where we expect the backing EIM service to be called securely to
        // validate the supplied token

        String connectionString = getAuthUrl();
        if (connectionString != null) {
            // make your authentication call here
        }

        AuthHandlerResult authHandlerResult = new AuthHandlerResult(true);
        // add some cookie and additional props
        authHandlerResult.addRootCookie(OUR_COOKIE_NAME, UUID.randomUUID().toString());
        authHandlerResult.addAdditionalProperty(AUTHED_BY_CREDS, false);
        return authHandlerResult;
    }

    @Override
    public boolean resolveUsernamesViaOtdsResource() {
        // we indicate here that we do not want the Gateway to attempt username
        // resolution via the OTDS resource
        return false;
    }

    @Override
    public String getOtdsResourceId() {
        // if this service were integrated with OTDS it should return its OTDS resource ID here
        // and the Gateway should be permitted to access it, we usually retrieve the resource ID from a
        // backing EIM service, this could even be recorded as a configuration setting to allow an admin
        // to set it dynamically
        return null;
    }

    /**
     * These cookies are used to clear the clients session on logout. Be sure to list
     * any cookies you want to be removed on Gateway logout via the "Set-Cookie" header.
     *
     * @return cookies this auth handler is concerned with
     */
    @Override
    public Set<Cookie> getKnownCookies() {
        Set<Cookie> cookies = new HashSet<>();

        // we add a HTTP only cookie
        Cookie llCookie = new Cookie(OUR_COOKIE_NAME, "");
        llCookie.setPath("/");
        llCookie.setHttpOnly(true);
        cookies.add(llCookie);

        return cookies;
    }

    /**
     * Get our fake EIM connector string to use as the auth endpoint we are going to use.
     *
     * @return URL or null if the EIM connector doesn't have a URL yet
     */
    private String getAuthUrl() {
        String connectionString = null;
        ExampleEIMConnector eimConnector = AWComponentContext.getComponent(ExampleEIMConnector.class);
        if (eimConnector != null) {
            connectionString = eimConnector.getConnectionString() + "/login";
        }
        return connectionString;
    }

}
