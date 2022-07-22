package com.fasterxml.jackson.dataformat.yaml;

import tools.jackson.core.JsonParser;
import tools.jackson.core.exc.StreamReadException;

public class JacksonYAMLParseException extends StreamReadException
{
    private static final long serialVersionUID = 1L;

    public JacksonYAMLParseException(JsonParser p, String msg, Exception e) {
        super(p, msg, e);
    }
}
