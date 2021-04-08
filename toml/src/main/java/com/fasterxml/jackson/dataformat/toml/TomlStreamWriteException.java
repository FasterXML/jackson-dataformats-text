package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.exc.StreamWriteException;

public final class TomlStreamWriteException
    extends StreamWriteException
{
    private static final long serialVersionUID = 1L;

    TomlStreamWriteException(String msg, JsonGenerator g) {
        super(msg, g);
    }

    @Override
    public StreamWriteException withGenerator(JsonGenerator g) {
        this._processor = g;
        return this;
    }
}
