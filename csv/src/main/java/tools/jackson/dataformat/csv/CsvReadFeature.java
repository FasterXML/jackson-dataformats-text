package tools.jackson.dataformat.csv;

import tools.jackson.core.FormatFeature;

/**
 * Enumeration that defines all togglable features for CSV parsers
 *<p>
 * NOTE: in Jackson 2.x this was named {@code CsvParser.Feature}.
 */
public enum CsvReadFeature
    implements FormatFeature
{
    /**
     * Feature determines whether spaces around separator characters
     * (commas) are to be automatically trimmed before being reported
     * or not.
     * Note that this does NOT force trimming of possible white space from
     * within double-quoted values, but only those surrounding unquoted
     * values (white space outside of double-quotes is never included regardless
     * of trimming).
     *<p>
     * Default value is false, as per <a href="http://tools.ietf.org/html/rfc4180">RFC-4180</a>.
     */
    TRIM_SPACES(false),

    /**
     * Feature determines whether spaces around separator characters
     * (commas) in header line entries (header names) are to be automatically
     * trimmed before being reported or not.
     * Note that this does NOT force trimming of possible white space from
     * within double-quoted values, but only those surrounding unquoted
     * values (white space outside of double-quotes is never included regardless
     * of trimming).
     *<p>
     * Default value is {@code true}.
     */
    TRIM_HEADER_SPACES(true),

    /**
     * Feature that determines how stream of records (usually CSV lines, but sometimes
     * multiple lines when line-feeds are included in quoted values) is exposed:
     * either as a sequence of Objects (false), or as an Array of Objects (true).
     * Using stream of Objects is convenient when using
     * <code>ObjectMapper.readValues(...)</code>
     * and array of Objects convenient when binding to <code>List</code>s or
     * arrays of values.
     *<p>
     * Default value is false, meaning that by default a CSV document is exposed as
     * a sequence of root-level Object entries.
     */
    WRAP_AS_ARRAY(false),

    /**
     * Feature that allows ignoring of unmappable "extra" columns; that is, values for
     * columns that appear after columns for which types are defined. When disabled,
     * an exception is thrown for such column values, but if enabled, they are
     * silently ignored.
     *<p>
     * Feature is disabled by default.
     */
    IGNORE_TRAILING_UNMAPPABLE(false),

    /**
     * Feature that allows skipping input lines that are completely empty or blank (composed only of whitespace),
     * instead of being decoded as lines of just a single column with an empty/blank String value (or,
     * depending on binding, `null`).
     *<p>
     * Feature is disabled by default.
     */
    SKIP_EMPTY_LINES(false),

    /**
     * Feature that allows there to be a trailing single extraneous data
     * column that is empty. When this feature is disabled, any extraneous
     * column, regardless of content will cause an exception to be thrown.
     * Disabling this feature is only useful when
     * IGNORE_TRAILING_UNMAPPABLE is also disabled.
     */
    ALLOW_TRAILING_COMMA(true),

    /**
     * Feature that allows accepting "hash comments" by default, similar to
     * {@link CsvSchema#withAllowComments(boolean)}. If enabled, such comments
     * are by default allowed on all columns of all documents.
     */
    ALLOW_COMMENTS(false),
    
    /**
     * Feature that allows failing (with a {@link CsvReadException}) in cases
     * where number of column values encountered is less than number of columns
     * declared in the active schema ("missing columns").
     *<p>
     * Note that this feature has precedence over {@link #INSERT_NULLS_FOR_MISSING_COLUMNS}
     *<p>
     * Feature is disabled by default.
     */
    FAIL_ON_MISSING_COLUMNS(false),

    /**
     * Feature that allows failing (with a {@link CsvReadException}) in cases
     * where number of header columns encountered is less than number of columns
     * declared in the active schema (if there is one).
     *<p>
     * Feature is enabled by default.
     */
    FAIL_ON_MISSING_HEADER_COLUMNS(true),

    /**
     * Feature that allows "inserting" virtual key / `null` value pairs in case
     * a row contains fewer columns than declared by configured schema.
     * This typically has the effect of forcing an explicit `null` assigment (or
     * corresponding "null value", if so configured) at databinding level.
     * If disabled, no extra work is done and values for "missing" columns are
     * not exposed as part of the token stream.
     *<p>
     * Note that this feature is only considered if
     * {@link #FAIL_ON_MISSING_COLUMNS}
     * is disabled.
     *<p>
     * Feature is disabled by default.
     */
    INSERT_NULLS_FOR_MISSING_COLUMNS(false),

    /**
     * Feature that enables coercing an empty {@link String} to `null`.
     *<p>
     * Note that if this setting is enabled, {@link #EMPTY_UNQUOTED_STRING_AS_NULL}
     * has no effect.
     *
     * Feature is disabled by default for backwards compatibility.
     */
    EMPTY_STRING_AS_NULL(false),

    /**
     * Feature that enables coercing an empty un-quoted {@link String} to `null`.
     * This feature allow differentiating between an empty quoted {@link String} and an empty un-quoted {@link String}.
     *<p>
     * Note that this feature is only considered if
     * {@link #EMPTY_STRING_AS_NULL}
     * is disabled.
     *<p>
     * Feature is disabled by default for backwards compatibility.
     */
    EMPTY_UNQUOTED_STRING_AS_NULL(false),
    ;

    private final boolean _defaultState;
    private final int _mask;
    
    /**
     * Method that calculates bit set (flags) of all features that
     * are enabled by default.
     */
    public static int collectDefaults()
    {
        int flags = 0;
        for (CsvReadFeature f : values()) {
            if (f.enabledByDefault()) {
                flags |= f.getMask();
            }
        }
        return flags;
    }
    
    private CsvReadFeature(boolean defaultState) {
        _defaultState = defaultState;
        _mask = (1 << ordinal());
    }

    @Override
    public boolean enabledByDefault() { return _defaultState; }
    @Override
    public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
    @Override
    public int getMask() { return _mask; }
}
