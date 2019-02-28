package com.fasterxml.jackson.dataformat.yaml.snakeyaml.error;

import java.util.Optional;

/**
 * Placeholder for shaded <code>org.yaml.snakeyaml.error.Mark</code>
 *
 * @since 2.8 (as non-shaded); earlier shaded in
 *
 * @deprecated Should use basic {@link com.fasterxml.jackson.core.JsonLocation} instead
 */
@Deprecated // since 2.8
public class Mark
{
    protected final org.snakeyaml.engine.v1.exceptions.Mark _source;

    protected Mark(org.snakeyaml.engine.v1.exceptions.Mark src) {
        _source = src;
    }
    
    public static Mark from(Optional<org.snakeyaml.engine.v1.exceptions.Mark> src) {
        return (!src.isPresent())  ? null : new Mark(src.get());
    }

    public String getName() {
        return _source.getName();
    }
    
    public int getColumn() {
        return _source.getColumn();
    }

    public int getLine() {
        return _source.getLine();
    }

    public int getIndex() {
        return _source.getIndex();
    }
}
