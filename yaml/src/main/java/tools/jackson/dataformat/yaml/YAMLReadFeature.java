package tools.jackson.dataformat.yaml;

import tools.jackson.core.FormatFeature;

/**
 * Enumeration that defines all togglable features for YAML parsers.
 *<p>
 * NOTE: in Jackson 2.x this was named {@code YAMLParser.Feature}.
 */
public enum YAMLReadFeature implements FormatFeature
{
    /**
     * Feature that determines whether empty YAML documents (documents with only
     * comments or whitespace, or completely empty) should be exposed as empty
     * Object ({@code START_OBJECT}/{@code END_OBJECT} token pair) instead of
     * causing "No content to map" error.
     *<p>
     * This is useful for example for deserializing to POJOs with default values,
     * where an  empty configuration file should create an object with all default
     * values rather than failing.
     *<p>
     * Feature is disabled by default for backwards-compatibility.
     */
    EMPTY_DOCUMENT_AS_EMPTY_OBJECT(false),

    /**
     * Feature that determines whether an empty {@link String} will be parsed
     * as {@code null}. Logic is part of YAML 1.1 
     * <a href="https://yaml.org/type/null.html">Null Language-Independent Type</a>.
     *<p>
     * Feature is enabled by default for backwards-compatibility reasons.
     */
    EMPTY_STRING_AS_NULL(true),

    /**
     * Feature that determines whether values starting with {@code 0} followed
     * by digits (like {@code 0444}) are interpreted as octal numbers.
     * When enabled (the default), {@code 0444} parses as decimal 292 (octal
     * interpretation). When disabled, {@code 0444} parses as decimal 444.
     *<p>
     * Note that explicit YAML 1.2 octal prefix {@code 0o} (like {@code 0o444})
     * is always recognized as octal regardless of this setting.
     *<p>
     * Note: this feature only has effect when using the
     * {@link YAMLSchema#CORE CORE} schema, since the default JSON schema
     * does not resolve {@code 0}-prefixed values as integers.
     *<p>
     * Feature is enabled by default for backwards-compatibility.
     *
     * @since 3.2
     */
    PARSE_OCTAL_NUMBERS(true)
    ;

    private final boolean _defaultState;
    private final int _mask;

    // Method that calculates bit set (flags) of all features that
    // are enabled by default.
    public static int collectDefaults()
    {
        int flags = 0;
        for (YAMLReadFeature f : values()) {
            if (f.enabledByDefault()) {
                flags |= f.getMask();
            }
        }
        return flags;
    }

    private YAMLReadFeature(boolean defaultState) {
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
