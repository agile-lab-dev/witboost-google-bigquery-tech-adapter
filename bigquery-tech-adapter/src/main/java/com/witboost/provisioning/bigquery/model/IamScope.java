package com.witboost.provisioning.bigquery.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IamScope {
    OWNER("owner"),
    PROJECT("project");

    private final String value;

    IamScope(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static IamScope fromValue(String value) {
        for (var scope : values()) {
            if (scope.value.equalsIgnoreCase(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unsupported IAM scope: " + value);
    }
}
