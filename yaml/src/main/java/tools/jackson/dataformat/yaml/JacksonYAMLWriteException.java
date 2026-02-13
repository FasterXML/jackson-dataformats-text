package tools.jackson.dataformat.yaml;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.StreamWriteException;

/**
 * Exception type thrown by YAML generator when SnakeYAML's emitter throws
 * a {@link org.snakeyaml.engine.v2.exceptions.YamlEngineException}.
 * This wrapping prevents SnakeYAML implementation details from leaking to callers,
 * which is important for OSGi runtime compatibility and proper abstraction.
 *
 * @since 3.1
 */
public class JacksonYAMLWriteException extends StreamWriteException
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for creating a wrapped exception.
     *
     * @param g Generator context where the error occurred
     * @param msg Error message (typically from the underlying SnakeYAML exception)
     * @param e Root cause exception (typically a YamlEngineException)
     */
    public JacksonYAMLWriteException(JsonGenerator g, String msg, Exception e) {
        super(g, msg, e);
    }
}
