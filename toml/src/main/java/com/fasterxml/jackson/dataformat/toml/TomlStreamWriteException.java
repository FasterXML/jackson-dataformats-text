package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.io.ContentReference;

public final class TomlStreamWriteException extends StreamWriteException {
    private static final long serialVersionUID = 1L;

    TomlStreamWriteException(JsonGenerator g, String msg) {
        super(g, msg);
    }

    @Override
    public StreamWriteException withGenerator(JsonGenerator g) {
        this._processor = g;
        return this;
    }
}
