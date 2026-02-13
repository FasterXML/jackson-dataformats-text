package tools.jackson.dataformat.yaml;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.StreamWriteException;

public class JacksonYAMLWriteException extends StreamWriteException
{
    private static final long serialVersionUID = 1L;

    public JacksonYAMLWriteException(JsonGenerator g, String msg, Exception e) {
        super(g, msg, e);
    }
}
