package tools.jackson.dataformat.csv;

import tools.jackson.core.exc.StreamReadException;

/**
 * Format-specific exception used to indicate problems regarding low-level
 * decoding/parsing issues specific to CSV content;
 * usually problems with field-to-column mapping as defined by {@link CsvSchema}.
 *<p>
 * In Jackson 2.x this type extends
 * {@link tools.jackson.databind.DatabindException}, but for Jackson 3.0
 * will become streaming-level exception
 */
public class CsvReadException
    extends StreamReadException
{
    private static final long serialVersionUID = 3L;

    protected final CsvSchema _schema;

    public CsvReadException(CsvParser p, String msg, CsvSchema schema) {
        super(p, msg);
        _schema = schema;
    }

    public static CsvReadException from(CsvParser p, String msg, CsvSchema schema) {
        return new CsvReadException(p, msg, schema);
    }

    public CsvSchema getSchema() { return _schema; }
}
