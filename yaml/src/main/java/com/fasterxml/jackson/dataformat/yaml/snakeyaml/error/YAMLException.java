package com.fasterxml.jackson.dataformat.yaml.snakeyaml.error;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.JacksonYAMLParseException;

/**
 * Replacement for formerly shaded exception type from SnakeYAML; included
 * in 2.8 solely for backwards compatibility: new code that relies on Jackson 2.8
 * and alter should NOT use this type but only base type {@link YAMLException}.
 *
 * @deprecated Since 2.8
 */
@Deprecated
public class YAMLException extends JacksonYAMLParseException
{
    private static final long serialVersionUID = 1L;

    public YAMLException(JsonParser p,
            org.yaml.snakeyaml.error.YAMLException src) {
        super(p, src.getMessage(), src);
    }

    public static YAMLException from(JsonParser p,
            org.yaml.snakeyaml.error.YAMLException src) {
        return new YAMLException(p, src);
    }
}
