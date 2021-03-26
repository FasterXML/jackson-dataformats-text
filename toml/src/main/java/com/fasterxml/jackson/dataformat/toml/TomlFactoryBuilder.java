package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.base.DecorableTSFactory;

/**
 * {@link com.fasterxml.jackson.core.TokenStreamFactory.TSFBuilder}
 * implementation for constructing {@link TomlFactory}
 * instances.
 *
 * @since 3.0
 */
public class TomlFactoryBuilder extends DecorableTSFactory.DecorableTSFBuilder<TomlFactory, TomlFactoryBuilder> {
    TomlFactoryBuilder() {
        super(TomlFactory.DEFAULT_TOML_PARSER_FEATURE_FLAGS, TomlFactory.DEFAULT_TOML_GENERATOR_FEATURE_FLAGS);
    }

    TomlFactoryBuilder(TomlFactory base) {
        super(base);
    }

    @Override
    public TomlFactory build() {
        return new TomlFactory(this);
    }

    /*
    /**********************************************************
    /* Parser feature setting
    /**********************************************************
     */

    public TomlFactoryBuilder enable(TomlReadFeature f) {
        _formatReadFeatures |= f.getMask();
        return this;
    }

    public TomlFactoryBuilder enable(TomlReadFeature first, TomlReadFeature... other) {
        _formatReadFeatures |= first.getMask();
        for (TomlReadFeature f : other) {
            _formatReadFeatures |= f.getMask();
        }
        return this;
    }

    public TomlFactoryBuilder disable(TomlReadFeature f) {
        _formatReadFeatures &= ~f.getMask();
        return this;
    }

    public TomlFactoryBuilder disable(TomlReadFeature first, TomlReadFeature... other) {
        _formatReadFeatures &= ~first.getMask();
        for (TomlReadFeature f : other) {
            _formatReadFeatures &= ~f.getMask();
        }
        return this;
    }

    public TomlFactoryBuilder configure(TomlReadFeature f, boolean state) {
        return state ? enable(f) : disable(f);
    }
}
