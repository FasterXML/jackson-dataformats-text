package com.fasterxml.jackson.dataformat.csv;

/**
 * Format-specific exception used to indicate problems regarding low-level
 * decoding/parsing issues specific to CSV content;
 * usually problems with field-to-column mapping as defined by {@link CsvSchema}.
 *<p>
 * In Jackson 2.x this type extends
 * {@link com.fasterxml.jackson.databind.DatabindException}, but for Jackson 3.0
 * will become streaming-level exception
 *
 * @since 2.13
 */
@SuppressWarnings("deprecation")
public class CsvReadException
    extends CsvMappingException
{
    private static final long serialVersionUID = 1L;

    public CsvReadException(CsvParser p, String msg, CsvSchema schema) {
        super(p, msg, schema);
    }

    public static CsvReadException from(CsvParser p, String msg, CsvSchema schema) {
        return new CsvReadException(p, msg, schema);
    }
}
