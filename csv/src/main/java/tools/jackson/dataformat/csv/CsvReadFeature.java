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
     * Feature that enables coercing an empty {@link String} (quoted or unquoted)
     * to {@code null}.
     *<p>
     * Note that if this setting is enabled, {@link #EMPTY_UNQUOTED_STRING_AS_NULL}
     * has no effect.
     *
     * Feature is disabled by default for backwards compatibility.
     */
    EMPTY_STRING_AS_NULL(false),

    /**
     * Feature that enables coercing an empty un-quoted {@link String} to {@code null}.
     * This feature allow differentiating between an empty quoted {@link String} and an empty un-quoted {@link String}.
     *<p>
     * Note that this feature is only considered if
     * {@link #EMPTY_STRING_AS_NULL}
     * is disabled.
     *<p>
     * Feature is disabled by default for backwards compatibility.
     */
    EMPTY_UNQUOTED_STRING_AS_NULL(false),

    /**
     * Feature that enables treating empty unquoted cell values as "missing",
     * effectively suppressing the token pair (property name + value) for such cells.
     * This means that if the target POJO field has a default value, it will be
     * preserved instead of being overwritten with an empty String.
     *<p>
     * This is different from {@link #EMPTY_STRING_AS_NULL} which coerces the value
     * to {@code null}: this feature causes the value to not be included in the token
     * stream at all, similar to how truly missing columns (row shorter than schema)
     * are handled.
     *<p>
     * Only applies to unquoted empty values; a quoted empty string ({@code ""}) is
     * still reported normally.
     *<p>
     * Feature is disabled by default for backwards compatibility.
     *
     * @since 3.2
     */
    EMPTY_UNQUOTED_STRING_AS_MISSING(false),

    /**
     * Feature that enables treating only un-quoted values matching the configured
     * "null value" String (see {@link CsvSchema#getNullValueString()}) as {@code null},
     * but not quoted values:
     * differentiating between a quoted null value String (like {@code "null"})
     * which remains as a String, and an unquoted null value (like {@code null})
     * which becomes {@code null}.
     *<p>
     * This is similar to {@link #EMPTY_UNQUOTED_STRING_AS_NULL} but applies to the
     * explicitly configured null value rather than empty strings.
     *<p>
     * Note: This feature only has an effect if a null value is configured via
     * {@link CsvSchema.Builder#setNullValue(String)}.
     *<p>
     * Feature is disabled by default for backwards compatibility.
     *
     * @since 3.1
     */
    ONLY_UNQUOTED_NULL_VALUES_AS_NULL(false),

    /**
     * Feature that allows skipping input rows that consist solely of column separator
     * characters (for example, a line containing only {@code ,,} with the default
     * comma separator).
     * This is different from {@link #SKIP_EMPTY_LINES} which only skips lines that are
     * completely empty or blank (whitespace only): this feature skips lines that
     * contain only consecutive separator characters followed by a linefeed.
     *<p>
     * Feature is disabled by default.
     *
     * @since 3.2
     */
    SKIP_EMPTY_ROWS(false),

    /**
     * Feature that enables failing (with a {@link CsvReadException}) when
     * duplicate column names are encountered in the header line.
     *<p>
     * When enabled, parsing will fail if the header line contains two or more
     * columns with the same name. When disabled, duplicates are allowed and
     * the last column with a given name will be the one accessible by name
     * (earlier columns with the same name are effectively hidden).
     *<p>
     * Feature is enabled by default.
     *
     * @since 3.2
     */
    FAIL_ON_DUPLICATE_HEADER_COLUMNS(true),

    /**
     * Feature that enables case-insensitive matching of header column names
     * against schema column names. When enabled, a CSV header column named
     * "TEMP_MAX" will match a schema column named "temp_max" (and vice versa).
     *<p>
     * This is useful when used together with
     * {@code MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES} to allow
     * case-insensitive header matching at the parser level, preventing
     * {@link #FAIL_ON_MISSING_HEADER_COLUMNS} from incorrectly reporting
     * columns as missing when they differ only by case.
     *<p>
     * Feature is disabled by default.
     *
     * @since 3.2
     */
    CASE_INSENSITIVE_HEADERS(false),
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
