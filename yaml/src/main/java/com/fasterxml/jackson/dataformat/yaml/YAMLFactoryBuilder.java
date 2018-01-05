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

    /**
     * Set of {@link YAMLParser.Feature}s enabled, as bitmask.
     */
    protected int _formatParserFeatures;

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
        _formatParserFeatures = YAMLFactory.DEFAULT_YAML_PARSER_FEATURE_FLAGS;
        _formatGeneratorFeatures = YAMLFactory.DEFAULT_YAML_GENERATOR_FEATURE_FLAGS;
    }

    public YAMLFactoryBuilder(YAMLFactory base) {
        super(base);
        _formatParserFeatures = base._formatParserFeatures;
        _formatGeneratorFeatures = base._formatGeneratorFeatures;
    }

    // // // Parser features

    public YAMLFactoryBuilder with(YAMLParser.Feature f) {
        _formatParserFeatures |= f.getMask();
        return _this();
    }

    public YAMLFactoryBuilder with(YAMLParser.Feature first, YAMLParser.Feature... other) {
        _formatParserFeatures |= first.getMask();
        for (YAMLParser.Feature f : other) {
            _formatParserFeatures |= f.getMask();
        }
        return _this();
    }

    public YAMLFactoryBuilder without(YAMLParser.Feature f) {
        _formatParserFeatures &= ~f.getMask();
        return _this();
    }

    public YAMLFactoryBuilder without(YAMLParser.Feature first, YAMLParser.Feature... other) {
        _formatParserFeatures &= ~first.getMask();
        for (YAMLParser.Feature f : other) {
            _formatParserFeatures &= ~f.getMask();
        }
        return _this();
    }

    public YAMLFactoryBuilder set(YAMLParser.Feature f, boolean state) {
        return state ? with(f) : without(f);
    }

    // // // Generator features

    public YAMLFactoryBuilder with(YAMLGenerator.Feature f) {
        _formatGeneratorFeatures |= f.getMask();
        return _this();
    }

    public YAMLFactoryBuilder with(YAMLGenerator.Feature first, YAMLGenerator.Feature... other) {
        _formatGeneratorFeatures |= first.getMask();
        for (YAMLGenerator.Feature f : other) {
            _formatGeneratorFeatures |= f.getMask();
        }
        return _this();
    }

    public YAMLFactoryBuilder without(YAMLGenerator.Feature f) {
        _formatGeneratorFeatures &= ~f.getMask();
        return _this();
    }
    
    public YAMLFactoryBuilder without(YAMLGenerator.Feature first, YAMLGenerator.Feature... other) {
        _formatGeneratorFeatures &= ~first.getMask();
        for (YAMLGenerator.Feature f : other) {
            _formatGeneratorFeatures &= ~f.getMask();
        }
        return _this();
    }

    public YAMLFactoryBuilder set(YAMLGenerator.Feature f, boolean state) {
        return state ? with(f) : without(f);
    }
    
    // // // Accessors

    public int formatParserFeaturesMask() { return _formatParserFeatures; }
    public int formatGeneratorFeaturesMask() { return _formatGeneratorFeatures; }

    @Override
    public YAMLFactory build() {
        return new YAMLFactory(this);
    }
}
