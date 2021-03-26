package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.FormatFeature;

/**
 * Enumeration that defines all togglable features for TOML parsers.
 */
public enum TomlReadFeature
        implements FormatFeature {
    USE_BIG_INTEGER_FOR_INTS(false),
    USE_BIG_DECIMAL_FOR_FLOATS(false),
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
