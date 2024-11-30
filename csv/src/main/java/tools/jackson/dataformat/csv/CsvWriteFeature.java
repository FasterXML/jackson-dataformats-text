package tools.jackson.dataformat.csv;

import tools.jackson.core.FormatFeature;

/**
 * Enumeration that defines all togglable features for CSV writers
 *<p>
 * NOTE: in Jackson 2.x this was named {@code CsvGenerator.Feature}.
 */
public enum CsvWriteFeature
    implements FormatFeature
{
    /**
     * Feature that determines how much work is done before determining that
     * a column value requires quoting: when set as <code>true</code>, full
     * check is made to only use quoting when it is strictly necessary;
     * but when <code>false</code>, a faster but more conservative check
     * is made, and possibly quoting is used for values that might not need it.
     * Trade-offs is basically between optimal/minimal quoting (true), and
     * faster handling (false).
     * Faster check involves only checking first N characters of value, as well
     * as possible looser checks.
     *<p>
     * Note, however, that regardless setting, all values that need to be quoted
     * will be: it is just that when set to <code>false</code>, other values may
     * also be quoted (to avoid having to do more expensive checks).
     *<p>
     * Default value is <code>false</code> for "loose" (approximate, conservative)
     * checking.
     */
    STRICT_CHECK_FOR_QUOTING(false),

    /**
     * Feature that determines whether columns without matching value may be omitted,
     * when they are the last values of the row.
     * If <code>true</code>, values and separators between values may be omitted, to slightly reduce
     * length of the row; if <code>false</code>, separators need to stay in place and values
     * are indicated by empty Strings.
     */
    OMIT_MISSING_TAIL_COLUMNS(false),

    /**
     * Feature that determines whether values written as Strings (from <code>java.lang.String</code>
     * valued POJO properties) should be forced to be quoted, regardless of whether they
     * actually need this.
     * Note that this feature has precedence over {@link #STRICT_CHECK_FOR_QUOTING}, when
     * both would be applicable.
     * Note that this setting does NOT affect quoting of typed values like {@code Number}s
     * or {@code Boolean}s.
     */
    ALWAYS_QUOTE_STRINGS(false),

    /**
     * Feature that determines whether values written as empty Strings (from <code>java.lang.String</code>
     * valued POJO properties) should be forced to be quoted.
     */
    ALWAYS_QUOTE_EMPTY_STRINGS(false),

    /**
     * Feature that determines whether values written as Nymbers (from {@code java.lang.Number}
     * valued POJO properties) should be forced to be quoted, regardless of whether they
     * actually need this.
     */
    ALWAYS_QUOTE_NUMBERS(false),
    
    /**
     * Feature that determines whether quote characters within quoted String values are escaped
     * using configured escape character, instead of being "doubled up" (that is: a quote character
     * is written twice in a row).
     *<p>
     * Default value is false so that quotes are doubled as necessary, not escaped.
     */
    ESCAPE_QUOTE_CHAR_WITH_ESCAPE_CHAR(false),

    /**
     * Feature that determines whether control characters (non-printable) are escaped using the
     * configured escape character. This feature allows LF and CR characters to be output as <pre>\n</pre>
     * and <pre>\r</pre> instead of being echoed out. This is a compatibility feature for some
     * parsers that can not read such output back in.
     * <p>
     * Default value is false so that control characters are echoed out (backwards compatible).
     */
    ESCAPE_CONTROL_CHARS_WITH_ESCAPE_CHAR(false),

    /**
     * Feature that determines whether a line-feed will be written at the end of content,
     * after the last row of output.
     *<p>
     * NOTE! When disabling this feature it is important that
     * {@link CsvGenerator#flush()} is NOT called before {@link CsvGenerator#close()} is called;
     * the current implementation relies on ability to essentially remove the
     * last linefeed that was appended in the output buffer.
     *<p>
     * Default value is {@code true} so all rows, including the last, are terminated by
     * a line feed.
     */
    WRITE_LINEFEED_AFTER_LAST_ROW(true)
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
        for (CsvWriteFeature f : values()) {
            if (f.enabledByDefault()) {
                flags |= f.getMask();
            }
        }
        return flags;
    }

    private CsvWriteFeature(boolean defaultState) {
        _defaultState = defaultState;
        _mask = (1 << ordinal());
    }

    @Override
    public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
    @Override
    public boolean enabledByDefault() { return _defaultState; }
    @Override
    public int getMask() { return _mask; }
}
