/**
 * Copyright © 2017 Open Text.  All Rights Reserved.
 */
package com.appworks.service.example.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Some dummy type used in our local REST API.
 */
public class MyImmutableDataObject implements Serializable {

    private final String key;
    private final Object value;

    @JsonCreator
    public MyImmutableDataObject(@JsonProperty("key") String key,
                                 @JsonProperty("value") Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

}
