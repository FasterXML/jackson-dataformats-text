package com.fasterxml.jackson.dataformat.yaml.impl;

import com.fasterxml.jackson.dataformat.yaml.BooleanMatcher;

public class StandardBooleanMatcher implements BooleanMatcher {
    @Override
    public Boolean match(String value) {
        int len = value.length();
        switch (len) {
            case 1:
                switch (value.charAt(0)) {
                    case 'y': case 'Y': return Boolean.TRUE;
                    case 'n': case 'N': return Boolean.FALSE;
                }
                break;
            case 2:
                if ("no".equalsIgnoreCase(value)) return Boolean.FALSE;
                if ("on".equalsIgnoreCase(value)) return Boolean.TRUE;
                break;
            case 3:
                if ("yes".equalsIgnoreCase(value)) return Boolean.TRUE;
                if ("off".equalsIgnoreCase(value)) return Boolean.FALSE;
                break;
            case 4:
                if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
                break;
            case 5:
                if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
                break;
        }
        return null;
    }
}
