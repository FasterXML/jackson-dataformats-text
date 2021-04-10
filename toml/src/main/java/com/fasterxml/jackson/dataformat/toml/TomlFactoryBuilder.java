package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.TSFBuilder;

/**
 * {@link com.fasterxml.jackson.core.TSFBuilder}
 * implementation for constructing {@link TomlFactory}
 * instances.
 *
 * @since 3.0
 */
public class TomlFactoryBuilder extends TSFBuilder<TomlFactory, TomlFactoryBuilder> {/*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    protected int _formatParserFeatures = TomlFactory.DEFAULT_TOML_PARSER_FEATURE_FLAGS;
    protected int _formatGeneratorFeatures = TomlFactory.DEFAULT_TOML_GENERATOR_FEATURE_FLAGS;

    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */

    TomlFactoryBuilder() {
        super();
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
        _formatParserFeatures |= f.getMask();
        return this;
    }

    public TomlFactoryBuilder enable(TomlReadFeature first, TomlReadFeature... other) {
        _formatParserFeatures |= first.getMask();
        for (TomlReadFeature f : other) {
            _formatParserFeatures |= f.getMask();
        }
        return this;
    }

    public TomlFactoryBuilder disable(TomlReadFeature f) {
        _formatParserFeatures &= ~f.getMask();
        return this;
    }

    public TomlFactoryBuilder disable(TomlReadFeature first, TomlReadFeature... other) {
        _formatParserFeatures &= ~first.getMask();
        for (TomlReadFeature f : other) {
            _formatParserFeatures &= ~f.getMask();
        }
        return this;
    }

    public TomlFactoryBuilder configure(TomlReadFeature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    /*
    /**********************************************************
    /* Generator feature setting
    /**********************************************************
     */

    public TomlFactoryBuilder enable(TomlWriteFeature f) {
        _formatGeneratorFeatures |= f.getMask();
        return this;
    }

    public TomlFactoryBuilder enable(TomlWriteFeature first, TomlWriteFeature... other) {
        _formatGeneratorFeatures |= first.getMask();
        for (TomlWriteFeature f : other) {
            _formatGeneratorFeatures |= f.getMask();
        }
        return this;
    }

    public TomlFactoryBuilder disable(TomlWriteFeature f) {
        _formatGeneratorFeatures &= ~f.getMask();
        return this;
    }

    public TomlFactoryBuilder disable(TomlWriteFeature first, TomlWriteFeature... other) {
        _formatGeneratorFeatures &= ~first.getMask();
        for (TomlWriteFeature f : other) {
            _formatGeneratorFeatures &= ~f.getMask();
        }
        return this;
    }

    public TomlFactoryBuilder configure(TomlWriteFeature f, boolean state) {
        return state ? enable(f) : disable(f);
    }
}
