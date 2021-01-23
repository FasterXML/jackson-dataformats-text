package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.exc.StreamReadException;

public class JacksonYAMLParseException extends StreamReadException
{
    private static final long serialVersionUID = 1L;

    public JacksonYAMLParseException(JsonParser p, String msg, Exception e) {
        super(p, msg, e);
    }
}
