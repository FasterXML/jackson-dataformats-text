package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Format-specific exception used to indicate problems regarding handling of CSV
 * content above minimal decoding, based on {@link CsvSchema}.
 *
 * @since 2.9
 *
 * @deprecated Since 2.13 use sub-class {@link CsvReadException} and {@link CsvWriteException}
 *   instead
 */
@Deprecated
public class CsvMappingException extends JsonMappingException
{
    private static final long serialVersionUID = 1L;

    protected final CsvSchema _schema;

    public CsvMappingException(CsvParser p, String msg, CsvSchema schema) {
        super(p, msg);
        _schema = schema;
    }

    public CsvMappingException(CsvGenerator gen, String msg, CsvSchema schema) {
        super(gen, msg);
        _schema = schema;
    }

    @Deprecated // since 2.13: use "CsvReadException.from()" instead
    public static CsvMappingException from(CsvParser p, String msg, CsvSchema schema) {
        return new CsvMappingException(p, msg, schema);
    }

    @Deprecated // since 2.13: use "CsvWriteException.from()" instead
    public static CsvMappingException from(CsvGenerator gen, String msg, CsvSchema schema) {
        return new CsvMappingException(gen, msg, schema);
    }

    public CsvSchema getSchema() {
        return _schema;
    }
}
