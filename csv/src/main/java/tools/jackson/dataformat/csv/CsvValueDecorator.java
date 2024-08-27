package tools.jackson.dataformat.csv;

import tools.jackson.core.JacksonException;

/**
 * Interface defining API for handlers that can add and remove "decorations"
 * to CSV values: for example, brackets around Array (List) values encoded
 * in a single physical String column.
 *<p>
 * Decorations are handled after handling other encoding aspects such as
 * optional quoting and/or escaping.
 *<p>
 * Decorators can be registered on specific columns of {@link CsvSchema}.
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
     * @return Decorated value (which may be {@code plainValue} as-is) but
     *   Must Not be {@code null}
     *
     * @throws JacksonException if attempt to decorate the value somehow fails
     */
    public String decorateValue(CsvGenerator gen, String plainValue)
        throws JacksonException;

    /**
     * Method called instead of {@link #decorateValue} in case where value being
     * written is from Java {@code null} value: this is often left as-is, without
     * decoration (and this is the default implementation), but may be
     * decorated.
     * To let default Null Value Replacement be used, should return {@code null}:
     * this is the default implementation.
     *
     * @param gen Generator that will be used for actual serialization
     *
     * @return Decorated value to use, IF NOT {@code null}: if {@code null} will use
     *   default null replacement value.
     *
     * @throws JacksonException if attempt to decorate the value somehow fails
     */
    public default String decorateNull(CsvGenerator gen)
        throws JacksonException
    {
        return null;
    }

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
     * @throws JacksonException if attempt to un-decorate the value fails
     */
    public String undecorateValue(CsvParser parser, String decoratedValue)
        throws JacksonException;
}
