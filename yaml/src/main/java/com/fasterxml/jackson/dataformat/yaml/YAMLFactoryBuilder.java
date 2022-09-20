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

    //copied from https://bitbucket.org/snakeyaml/snakeyaml/src/26624702fab8e0a1c301d7fad723c048528f75c3/src/main/java/org/yaml/snakeyaml/LoaderOptions.java#lines-26
    private final static int DEFAULT_CODEPOINT_LIMIT = 3 * 1024 * 1024; // 3 MB
    private int _codePointLimit = DEFAULT_CODEPOINT_LIMIT;

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

    public static int getDefaultCodepointLimit() {
        return DEFAULT_CODEPOINT_LIMIT;
    }

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
     * {@code StringQuotingChecker.Default.instance()}) is to be used
     * (when passing {@code null}
     *
     * @param sqc Checker to use (if non-null), or {@code null} to use the
     *   default one (see {@code StringQuotingChecker.Default.instance()})
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

    /**
     * @param codePointLimit the limit on number of codepoints when parsing YAML (default is 3Mb)
     * @return This builder instance, to allow chaining
     * @since 2.14
     */
    public YAMLFactoryBuilder codePointLimit(int codePointLimit) {
        this._codePointLimit = codePointLimit;
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

    public int codePointLimit() {
        return _codePointLimit;
    }

    @Override
    public YAMLFactory build() {
        return new YAMLFactory(this);
    }
}
