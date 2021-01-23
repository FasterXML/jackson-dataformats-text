package com.fasterxml.jackson.dataformat.csv;

/**
 * Format-specific exception used to indicate problems regarding low-level
 * generation issues specific to CSV content;
 * usually problems with field-to-column mapping as defined by {@link CsvSchema}.
 *<p>
 * In Jackson 2.x this type extends
 * {@link com.fasterxml.jackson.databind.DatabindException}, but for Jackson 3.0
 * will become streaming-level exception
 *
 * @since 2.13
 */
@SuppressWarnings("deprecation")
public class CsvWriteException
    extends CsvMappingException
{
    private static final long serialVersionUID = 1L;

    public CsvWriteException(CsvGenerator gen, String msg, CsvSchema schema) {
        super(gen, msg, schema);
    }

    public static CsvWriteException from(CsvGenerator gen, String msg, CsvSchema schema) {
        return new CsvWriteException(gen, msg, schema);
    }
}
