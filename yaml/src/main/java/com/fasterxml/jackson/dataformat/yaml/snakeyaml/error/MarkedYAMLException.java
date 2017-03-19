package com.fasterxml.jackson.dataformat.yaml.snakeyaml.error;

import com.fasterxml.jackson.core.JsonParser;

/**
 * Replacement for formerly shaded exception type from SnakeYAML; included
 * in 2.8 solely for backwards compatibility: new code that relies on Jackson 2.8
 * and alter should NOT use this type but only base type {@link YAMLException}.
 *
 * @deprecated Since 2.8
 */
@Deprecated
public class MarkedYAMLException extends YAMLException
{
    private static final long serialVersionUID = 1L;

    protected final org.yaml.snakeyaml.error.MarkedYAMLException _source;

    protected MarkedYAMLException(JsonParser p,
            org.yaml.snakeyaml.error.MarkedYAMLException src) {
        super(p, src);
        _source = src;
    }

    public static MarkedYAMLException from(JsonParser p,
            org.yaml.snakeyaml.error.MarkedYAMLException src) {
        return new MarkedYAMLException(p, src);
    }

    public String getContext() {
        return _source.getContext();
    }

    public Mark getContextMark() {
        return Mark.from(_source.getContextMark());
    }

    public String getProblem() {
        return _source.getProblem();
    }

    public Mark getProblemMark() {
        return Mark.from(_source.getProblemMark());
    }
}
