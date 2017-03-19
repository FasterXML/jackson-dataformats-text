package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

/**
 * @since 2.8
 */
public class JacksonYAMLParseException extends JsonParseException
{
    private static final long serialVersionUID = 1L;

    public JacksonYAMLParseException(JsonParser p, String msg, Exception e) {
        super(p, msg, e);
    }
}
