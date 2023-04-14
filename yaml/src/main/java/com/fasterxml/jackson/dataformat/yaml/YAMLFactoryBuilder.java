package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.StreamReadFeature;
import org.yaml.snakeyaml.DumperOptions;

import com.fasterxml.jackson.core.TSFBuilder;
import com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker;
import org.yaml.snakeyaml.LoaderOptions;

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

    /**
     * Set of {@link YAMLGenerator.Feature}s enabled, as bitmask.
     */
    protected int _formatGeneratorFeatures;

    /**
     * Set of {@link YAMLParser.Feature}s enabled, as bitmask.
     *
     * @since 2.15
     */
    protected int _formatParserFeatures;

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
     * <p>
     *     Ignored if you provide your own {@code DumperOptions}.
     * </p>
     */
    protected DumperOptions.Version _version;

    /**
     * Configuration for underlying parser to follow, if specified;
     * left as {@code null} for backwards compatibility (which means
     * whatever default settings {@code SnakeYAML} deems best).
     * <p>
     *     If you need to support parsing YAML files that are larger than 3Mb,
     *     it is recommended that you provide a LoaderOptions instance where
     *     you set the Codepoint Limit to a larger value than its 3Mb default.
     * </p>
     *
     * @since 2.14
     */
    protected LoaderOptions _loaderOptions;

    /**
     * Configuration for underlying generator to follow, if specified;
     * left as {@code null} for backwards compatibility (which means
     * the dumper options are derived based on {@link YAMLGenerator.Feature}s).
     * <p>
     *     These {@link YAMLGenerator.Feature}s are ignored if you provide your own DumperOptions:
     *     <ul>
     *         <li>{@code YAMLGenerator.Feature.ALLOW_LONG_KEYS}</li>
     *         <li>{@code YAMLGenerator.Feature.CANONICAL_OUTPUT}</li>
     *         <li>{@code YAMLGenerator.Feature.INDENT_ARRAYS}</li>
     *         <li>{@code YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR}</li>
     *         <li>{@code YAMLGenerator.Feature.SPLIT_LINES}</li>
     *         <li>{@code YAMLGenerator.Feature.USE_PLATFORM_LINE_BREAKS}</li>
     *     </ul>
     * </p>
     *
     * @since 2.14
     */
    protected DumperOptions _dumperOptions;

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
        _formatParserFeatures = base._yamlParserFeatures;
        _version = base._version;
        _quotingChecker = base._quotingChecker;
    }

    /*
    /**********************************************************
    /* Parser feature setting
    /**********************************************************
     */

    public YAMLFactoryBuilder enable(YAMLParser.Feature f) {
        _formatParserFeatures |= f.getMask();
        return this;
    }

    public YAMLFactoryBuilder enable(YAMLParser.Feature first, YAMLParser.Feature... other) {
        _formatParserFeatures |= first.getMask();
        for (YAMLParser.Feature f : other) {
            _formatParserFeatures |= f.getMask();
        }
        return this;
    }

    public YAMLFactoryBuilder disable(YAMLParser.Feature f) {
        _formatParserFeatures &= ~f.getMask();
        return this;
    }
    
    public YAMLFactoryBuilder disable(YAMLParser.Feature first, YAMLParser.Feature... other) {
        _formatParserFeatures &= ~first.getMask();
        for (YAMLParser.Feature f : other) {
            _formatParserFeatures &= ~f.getMask();
        }
        return this;
    }

    public YAMLFactoryBuilder configure(YAMLParser.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

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
     * Configuration for underlying parser to follow, if specified;
     * left as {@code null} for backwards compatibility (which means
     * whatever default settings {@code SnakeYAML} deems best).
     * <p>
     *     If you need to support parsing YAML files that are larger than 3Mb,
     *     it is recommended that you provide a LoaderOptions instance where
     *     you set the Codepoint Limit to a larger value than its 3Mb default.
     * </p>
     *
     * @param loaderOptions the {@code SnakeYAML} configuration to use when parsing YAML
     * @return This builder instance, to allow chaining
     * @since 2.14
     */
    public YAMLFactoryBuilder loaderOptions(LoaderOptions loaderOptions) {
        _loaderOptions = loaderOptions;

        // If the user wants to block duplicate keys this needs to be set in a different way to work
        if (!_loaderOptions.isAllowDuplicateKeys()) {
            enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION);
        }
        return this;
    }

    /**
     * Configuration for underlying generator to follow, if specified;
     * left as {@code null} for backwards compatibility (which means
     * the dumper options are derived based on {@link YAMLGenerator.Feature}s).
     * <p>
     *     These {@link YAMLGenerator.Feature}s are ignored if you provide your own DumperOptions:
     *     <ul>
     *         <li>{@code YAMLGenerator.Feature.ALLOW_LONG_KEYS}</li>
     *         <li>{@code YAMLGenerator.Feature.CANONICAL_OUTPUT}</li>
     *         <li>{@code YAMLGenerator.Feature.INDENT_ARRAYS}</li>
     *         <li>{@code YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR}</li>
     *         <li>{@code YAMLGenerator.Feature.SPLIT_LINES}</li>
     *         <li>{@code YAMLGenerator.Feature.USE_PLATFORM_LINE_BREAKS}</li>
     *     </ul>
     * </p>
     *
     * @param dumperOptions the {@code SnakeYAML} configuration to use when generating YAML
     * @return This builder instance, to allow chaining
     * @since 2.14
     */
    public YAMLFactoryBuilder dumperOptions(DumperOptions dumperOptions) {
        _dumperOptions = dumperOptions;
        return this;
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    public int formatParserFeaturesMask() { return _formatParserFeatures; }
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

    /**
     * Configuration for underlying parser to follow, if specified;
     * left as {@code null} for backwards compatibility (which means
     * whatever default settings {@code SnakeYAML} deems best).
     * <p>
     *     If you need to support parsing YAML files that are larger than 3Mb,
     *     it is recommended that you provide a LoaderOptions instance where
     *     you set the Codepoint Limit to a larger value than its 3Mb default.
     * </p>
     *
     * @return the {@code SnakeYAML} configuration to use when parsing YAML
     * @since 2.14
     */
    public LoaderOptions loaderOptions() {
        return _loaderOptions;
    }

    /**
     * Configuration for underlying generator to follow, if specified;
     * left as {@code null} for backwards compatibility (which means
     * the dumper options are derived based on {@link YAMLGenerator.Feature}s).
     * <p>
     *     These {@link YAMLGenerator.Feature}s are ignored if you provide your own DumperOptions:
     *     <ul>
     *         <li>{@code YAMLGenerator.Feature.ALLOW_LONG_KEYS}</li>
     *         <li>{@code YAMLGenerator.Feature.CANONICAL_OUTPUT}</li>
     *         <li>{@code YAMLGenerator.Feature.INDENT_ARRAYS}</li>
     *         <li>{@code YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR}</li>
     *         <li>{@code YAMLGenerator.Feature.SPLIT_LINES}</li>
     *         <li>{@code YAMLGenerator.Feature.USE_PLATFORM_LINE_BREAKS}</li>
     *     </ul>
     * </p>
     *
     * @return the {@code SnakeYAML} configuration to use when generating YAML
     * @since 2.14
     */
    public DumperOptions dumperOptions() {
        return _dumperOptions;
    }

    @Override
    public YAMLFactory build() {
        return new YAMLFactory(this);
    }
}
