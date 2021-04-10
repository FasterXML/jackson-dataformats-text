package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.FormatFeature;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enumeration that defines all togglable features for TOML generators.
 */
public enum TomlWriteFeature implements FormatFeature {
    /**
     * The TOML spec does not allow null values. We instead write an empty string when
     * {@link JsonGenerator#writeNull()} by default.
     * <p>
     * When this option is set, any attempt to write a null value will error instead.
     */
    FAIL_ON_NULL_WRITE(false);

    /**
     * Internal option for unit tests: Prohibit allocating internal buffers, except through the buffer recycler
     */
    static final int INTERNAL_PROHIBIT_INTERNAL_BUFFER_ALLOCATE = 0x80000000;

    final boolean _defaultState;
    final int _mask;

    // Method that calculates bit set (flags) of all features that
    // are enabled by default.
    public static int collectDefaults()
    {
        int flags = 0;
        for (TomlWriteFeature f : values()) {
            if (f.enabledByDefault()) {
                flags |= f.getMask();
            }
        }
        return flags;
    }

    private TomlWriteFeature(boolean defaultState) {
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
