/**
 * Copyright © 2017 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example.api;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;

public class RestApiConfiguration extends ResourceConfig {

    public RestApiConfiguration() {
        // scan this package for our JAX-RS endpoint classes
        packages("com.appworks.service.example.api");
        // use Jackson as the JSON marshalling implementation
        register(JacksonJsonProvider.class);
    }

}
