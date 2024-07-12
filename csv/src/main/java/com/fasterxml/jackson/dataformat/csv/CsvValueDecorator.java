package com.fasterxml.jackson.dataformat.csv;

import java.io.IOException;

/**
 * Interface defining API for handlers that can add and remove "decorations"
 * to CSV values: for example, brackets around Array (List) values encoded
 * in a single physical column.
 *<p>
 * Decorations are handled after handling other encoding aspects such as
 * optional quoting and/or escaping.
 *<p>
 * Decorators can be handled at both {@link CsvSchema} level (as defaults
 * for specific column types or classes) and {@link CsvSchema.Column} level (specific
 * to values of given column): if both defined, more specific -- latter, per-Column
 * -- is used.
 *
 * @since 2.18
 */
public interface CsvValueDecorator
{
    /**
     * Method called during serialization when encoding a value,
     * to produce "decorated" value to include in output (possibly
     * escaped and/or quoted).
     * Note that possible escaping and/or quoting (as per configuration
     * of {@link CsvSchema} is applied on decorated value.
     *
     * @param gen Generator that will be used for actual serialization
     * @param plainValue Value to decorate
     *
     * @return Decorated value (which may be {@code plainValue} as-is)
     *
     * @throws IOException if attempt to decorate the value somehow fails
     *    (typically a {@link com.fasterxml.jackson.core.exc.StreamWriteException})
     */
    public String decorateValue(CsvGenerator gen, String plainValue)
        throws IOException;

    /**
     * Method called during deserialization, to remove possible decoration
     * applied with {@link #decorateValue}.
     * Call is made after textual value for a cell (column
     * value) has been read using {@code parser} and after removing (decoding)
     * possible quoting and/or escaping of the value. Value passed in
     * has no escaping or quoting left.
     *
     * @param parser Parser that was used to decode textual value from input
     * @param decoratedValue Value from which to remove decorations, if any
     *    (some decorators can allow optional decorations; others may fail
     *    if none found)
     *
     * @return Value after removing decorations, if any.
     *
     * @throws IOException if attempt to undecorate the value fails
     *    (typically a {@link com.fasterxml.jackson.core.exc.StreamReadException})
     */
        public String undecorateValue(CsvParser parser, String decoratedValue)
        throws IOException;
}
