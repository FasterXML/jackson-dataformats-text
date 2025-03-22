package com.fasterxml.jackson.dataformat.yaml.impl;

import com.fasterxml.jackson.dataformat.yaml.BooleanMatcher;

public class RestrictedBooleanMatcher implements BooleanMatcher {
    @Override
    public Boolean match(String value) {
        if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
        return null;
    }
}
