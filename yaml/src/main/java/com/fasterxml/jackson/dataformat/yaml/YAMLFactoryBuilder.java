package com.fasterxml.jackson.dataformat.yaml;

import org.yaml.snakeyaml.DumperOptions;

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

    /**
     * YAML version for underlying generator to follow, if specified;
     * left as {@code null} for backwards compatibility (which means
     * whatever default settings {@code SnakeYAML} deems best).
     */
    protected DumperOptions.Version _version;

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
        _version = base._version;
        _quotingChecker = base._quotingChecker;
    }

    // // // Parser features NOT YET defined

    /*
    /**********************************************************
    /* Generator feature setting
    /**********************************************************
     */

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

    /*
    /**********************************************************
    /* Other YAML-specific settings
    /**********************************************************
     */

    /**
     * Method for specifying either custom {@link StringQuotingChecker}
     * to use instead of default one, or, that default one (see
     * {@link StringQuotingChecker.Default#instance()}) is to be used
     * (when passing {@code null}
     *
     * @param sqc Checker to use (if non-null), or {@code null} to use the
     *   default one (see {@link StringQuotingChecker.Default#instance()})
     *
     * @return This builder instance, to allow chaining
     */
    public YAMLFactoryBuilder stringQuotingChecker(StringQuotingChecker sqc) {
        _quotingChecker = sqc;
        return this;
    }

    /**
     * Method for specifying YAML version for generator to use (to produce
     * compliant output); if {@code null} passed, will let {@code SnakeYAML}
     * use its default settings.
     *
     * @param v YAML specification version to use for output, if not-null;
     *    {@code null} for default handling
     *
     * @return This builder instance, to allow chaining
     */
    public YAMLFactoryBuilder yamlVersionToWrite(DumperOptions.Version v) {
        _version = v;
        return this;
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

//    public int formatParserFeaturesMask() { return _formatParserFeatures; }
    public int formatGeneratorFeaturesMask() { return _formatGeneratorFeatures; }

    public DumperOptions.Version yamlVersionToWrite() {
        return _version;
    }

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
