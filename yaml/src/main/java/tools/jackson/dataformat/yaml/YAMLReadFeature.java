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
     * Feature that determines whether an empty {@link String} will be parsed
     * as {@code null}. Logic is part of YAML 1.1 
     * <a href="https://yaml.org/type/null.html">Null Language-Independent Type</a>.
     *<p>
     * Feature is enabled by default for backwards-compatibility reasons.
     */
    EMPTY_STRING_AS_NULL(true)
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
