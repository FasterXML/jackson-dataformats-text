package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.TSFBuilder;
import com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker;

/**
 * {@link com.fasterxml.jackson.core.TSFBuilder}
 * implementation for constructing {@link YAMLFactory}
 * instances.
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

    /**
     * Helper object used to determine whether property names, String values
     * must be quoted or not.
     *
     * @since 2.12
     */
    protected StringQuotingChecker _quotingChecker;

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
        _quotingChecker = base._quotingChecker;
    }

    // // // Parser features NOT YET defined

    // // // Generator features

    public YAMLFactoryBuilder enable(YAMLGenerator.Feature f) {
        _formatGeneratorFeatures |= f.getMask();
        return this;
    }

    public YAMLFactoryBuilder enable(YAMLGenerator.Feature first, YAMLGenerator.Feature... other) {
        _formatGeneratorFeatures |= first.getMask();
        for (YAMLGenerator.Feature f : other) {
            _formatGeneratorFeatures |= f.getMask();
        }
        return this;
    }

    public YAMLFactoryBuilder disable(YAMLGenerator.Feature f) {
        _formatGeneratorFeatures &= ~f.getMask();
        return this;
    }
    
    public YAMLFactoryBuilder disable(YAMLGenerator.Feature first, YAMLGenerator.Feature... other) {
        _formatGeneratorFeatures &= ~first.getMask();
        for (YAMLGenerator.Feature f : other) {
            _formatGeneratorFeatures &= ~f.getMask();
        }
        return this;
    }

    public YAMLFactoryBuilder configure(YAMLGenerator.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }
    
    public YAMLFactoryBuilder stringQuotingChecker(StringQuotingChecker sqc) {
        _quotingChecker = sqc;
        return this;
    }

    // // // Accessors

//    public int formatParserFeaturesMask() { return _formatParserFeatures; }
    public int formatGeneratorFeaturesMask() { return _formatGeneratorFeatures; }

    public StringQuotingChecker stringQuotingChecker() {
        if (_quotingChecker != null) {
            return _quotingChecker;
        }
        return StringQuotingChecker.Default.instance();
    }

    @Override
    public YAMLFactory build() {
        return new YAMLFactory(this);
    }
}
