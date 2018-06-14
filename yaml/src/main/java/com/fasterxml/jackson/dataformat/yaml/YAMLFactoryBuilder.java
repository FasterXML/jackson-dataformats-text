package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.TSFBuilder;

/**
 * {@link com.fasterxml.jackson.core.TSFBuilder}
 * implementation for constructing {@link YAMLFactory}
 * instances.
 *
 * @since 3.0
 */
public class YAMLFactoryBuilder extends TSFBuilder<YAMLFactory, YAMLFactoryBuilder>
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

//    protected int _formatParserFeatures;

    /**
     * Set of {@link YAMLGenerator.Feature}s enabled, as bitmask.
     */
    protected int _formatGeneratorFeatures;

    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */

    protected YAMLFactoryBuilder() {
        _formatGeneratorFeatures = YAMLFactory.DEFAULT_YAML_GENERATOR_FEATURE_FLAGS;
    }

    public YAMLFactoryBuilder(YAMLFactory base) {
        super(base);
        _formatGeneratorFeatures = base._yamlGeneratorFeatures;
    }

    // // // Parser features NOT YET defined

    // // // Generator features

    public YAMLFactoryBuilder enable(YAMLGenerator.Feature f) {
        _formatGeneratorFeatures |= f.getMask();
        return _this();
    }

    public YAMLFactoryBuilder enable(YAMLGenerator.Feature first, YAMLGenerator.Feature... other) {
        _formatGeneratorFeatures |= first.getMask();
        for (YAMLGenerator.Feature f : other) {
            _formatGeneratorFeatures |= f.getMask();
        }
        return _this();
    }

    public YAMLFactoryBuilder disable(YAMLGenerator.Feature f) {
        _formatGeneratorFeatures &= ~f.getMask();
        return _this();
    }
    
    public YAMLFactoryBuilder disable(YAMLGenerator.Feature first, YAMLGenerator.Feature... other) {
        _formatGeneratorFeatures &= ~first.getMask();
        for (YAMLGenerator.Feature f : other) {
            _formatGeneratorFeatures &= ~f.getMask();
        }
        return _this();
    }

    public YAMLFactoryBuilder configure(YAMLGenerator.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }
    
    // // // Accessors

//    public int formatParserFeaturesMask() { return _formatParserFeatures; }
    public int formatGeneratorFeaturesMask() { return _formatGeneratorFeatures; }

    @Override
    public YAMLFactory build() {
        return new YAMLFactory(this);
    }
}
