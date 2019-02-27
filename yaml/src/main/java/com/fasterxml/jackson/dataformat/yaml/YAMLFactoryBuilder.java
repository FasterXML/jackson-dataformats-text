package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.base.DecorableTSFactory.DecorableTSFBuilder;

/**
 * {@link com.fasterxml.jackson.core.TokenStreamFactory.TSFBuilder}
 * implementation for constructing {@link YAMLFactory}
 * instances.
 *
 * @since 3.0
 */
public class YAMLFactoryBuilder extends DecorableTSFBuilder<YAMLFactory, YAMLFactoryBuilder>
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */

    protected YAMLFactoryBuilder() {
        super(0, YAMLFactory.DEFAULT_YAML_GENERATOR_FEATURE_FLAGS);
    }

    public YAMLFactoryBuilder(YAMLFactory base) {
        super(base);
    }

    // // // Parser features NOT YET defined

    // // // Generator features

    public YAMLFactoryBuilder enable(YAMLGenerator.Feature f) {
        _formatWriteFeatures |= f.getMask();
        return _this();
    }

    public YAMLFactoryBuilder enable(YAMLGenerator.Feature first, YAMLGenerator.Feature... other) {
        _formatWriteFeatures |= first.getMask();
        for (YAMLGenerator.Feature f : other) {
            _formatWriteFeatures |= f.getMask();
        }
        return _this();
    }

    public YAMLFactoryBuilder disable(YAMLGenerator.Feature f) {
        _formatWriteFeatures &= ~f.getMask();
        return _this();
    }
    
    public YAMLFactoryBuilder disable(YAMLGenerator.Feature first, YAMLGenerator.Feature... other) {
        _formatWriteFeatures &= ~first.getMask();
        for (YAMLGenerator.Feature f : other) {
            _formatWriteFeatures &= ~f.getMask();
        }
        return _this();
    }

    public YAMLFactoryBuilder configure(YAMLGenerator.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    @Override
    public YAMLFactory build() {
        return new YAMLFactory(this);
    }
}
