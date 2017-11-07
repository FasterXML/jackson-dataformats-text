package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Format-specific exception used to indicate problems regarding handling of CSV
 * content above minimal decoding, based on {@link CsvSchema}.
 *
 * @since 2.9
 */
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

    public static CsvMappingException from(CsvParser p, String msg, CsvSchema schema) {
        return new CsvMappingException(p, msg, schema);
    }

    public static CsvMappingException from(CsvGenerator gen, String msg, CsvSchema schema) {
        return new CsvMappingException(gen, msg, schema);
    }

    public CsvSchema getSchema() {
        return _schema;
    }
}
