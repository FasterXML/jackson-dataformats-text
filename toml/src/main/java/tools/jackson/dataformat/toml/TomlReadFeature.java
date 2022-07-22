package tools.jackson.dataformat.toml;

import tools.jackson.core.FormatFeature;

/**
 * Enumeration that defines all togglable features for TOML parsers.
 */
public enum TomlReadFeature
        implements FormatFeature {
    /**
     * TOML has special syntax for time types corresponding to {@link java.time.LocalDate}, {@link java.time.LocalTime},
     * {@link java.time.LocalDateTime} and {@link java.time.OffsetDateTime}. By default, the TOML parser just returns
     * them as strings.
     * <p>
     * When this option is set, these time types will be parsed to their proper {@code java.time} counterparts and
     * appear as {@link tools.jackson.core.JsonToken#VALUE_EMBEDDED_OBJECT} tokens.
     */
    PARSE_JAVA_TIME(false);

    final boolean _defaultState;
    final int _mask;

    // Method that calculates bit set (flags) of all features that
    // are enabled by default.
    public static int collectDefaults()
    {
        int flags = 0;
        for (TomlReadFeature f : values()) {
            if (f.enabledByDefault()) {
                flags |= f.getMask();
            }
        }
        return flags;
    }

    private TomlReadFeature(boolean defaultState) {
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
