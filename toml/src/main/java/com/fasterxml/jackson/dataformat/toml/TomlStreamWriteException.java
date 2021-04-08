package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

public final class TomlStreamWriteException extends JsonGenerationException {
    private static final long serialVersionUID = 1L;

    TomlStreamWriteException(String msg, JsonGenerator g) {
        super(msg, g);
    }

    @Override
    public JsonGenerationException withGenerator(JsonGenerator g) {
        this._processor = g;
        return this;
    }
}
