package tools.jackson.dataformat.toml;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.StreamWriteException;

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
