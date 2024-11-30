package tools.jackson.dataformat.yaml;

import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.base.DecorableTSFactory.DecorableTSFBuilder;
import tools.jackson.dataformat.yaml.util.StringQuotingChecker;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.SpecVersion;

/**
 * {@link tools.jackson.core.TSFBuilder}
 * implementation for constructing {@link YAMLFactory}
 * instances.
 */
public class YAMLFactoryBuilder
    extends DecorableTSFBuilder<YAMLFactory, YAMLFactoryBuilder>
{
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Helper object used to determine whether property names, String values
     * must be quoted or not.
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
    protected SpecVersion _version;

    /**
     * Configuration for underlying parser to follow, if specified;
     * left as {@code null} for backwards compatibility (which means
     * whatever default settings {@code SnakeYAML} deems best).
     * <p>
     *     If you need to support parsing YAML files that are larger than 3Mb,
     *     it is recommended that you provide a LoaderOptions instance where
     *     you set the Codepoint Limit to a larger value than its 3Mb default.
     * </p>
     */
    protected LoadSettings _loadSettings;

    /**
     * Configuration for underlying generator to follow, if specified;
     * left as {@code null} for backwards compatibility (which means
     * the dumper options are derived based on {@link YAMLWriteFeature}s).
     * <p>
     *     These {@link YAMLWriteFeature}s are ignored if you provide your own DumperOptions:
     *     <ul>
     *         <li>{@code YAMLGenerator.Feature.ALLOW_LONG_KEYS}</li>
     *         <li>{@code YAMLGenerator.Feature.CANONICAL_OUTPUT}</li>
     *         <li>{@code YAMLGenerator.Feature.INDENT_ARRAYS}</li>
     *         <li>{@code YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR}</li>
     *         <li>{@code YAMLGenerator.Feature.SPLIT_LINES}</li>
     *         <li>{@code YAMLGenerator.Feature.USE_PLATFORM_LINE_BREAKS}</li>
     *     </ul>
     * </p>
     */
    protected DumpSettings _dumpSettings;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    protected YAMLFactoryBuilder() {
        super(StreamReadConstraints.defaults(), StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                0, YAMLFactory.DEFAULT_YAML_GENERATOR_FEATURE_FLAGS);
    }

    public YAMLFactoryBuilder(YAMLFactory base) {
        super(base);
        _version = base._version;
        _quotingChecker = base._quotingChecker;
        _loadSettings = base._loadSettings;
        _dumpSettings = base._dumpSettings;
    }

    /*
    /**********************************************************
    /* Generator feature setting
    /**********************************************************
     */

    public YAMLFactoryBuilder enable(YAMLWriteFeature f) {
        _formatWriteFeatures |= f.getMask();
        return this;
    }

    public YAMLFactoryBuilder enable(YAMLWriteFeature first, YAMLWriteFeature... other) {
        _formatWriteFeatures |= first.getMask();
        for (YAMLWriteFeature f : other) {
            _formatWriteFeatures |= f.getMask();
        }
        return this;
    }

    public YAMLFactoryBuilder disable(YAMLWriteFeature f) {
        _formatWriteFeatures &= ~f.getMask();
        return this;
    }

    public YAMLFactoryBuilder disable(YAMLWriteFeature first, YAMLWriteFeature... other) {
        _formatWriteFeatures &= ~first.getMask();
        for (YAMLWriteFeature f : other) {
            _formatWriteFeatures &= ~f.getMask();
        }
        return this;
    }

    public YAMLFactoryBuilder configure(YAMLWriteFeature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    /*
    /**********************************************************
    /* Generator feature setting
    /**********************************************************
     */

    public YAMLFactoryBuilder enable(YAMLReadFeature f) {
        _formatReadFeatures |= f.getMask();
        return this;
    }

    public YAMLFactoryBuilder enable(YAMLReadFeature first, YAMLReadFeature... other) {
        _formatReadFeatures |= first.getMask();
        for (YAMLReadFeature f : other) {
            _formatReadFeatures |= f.getMask();
        }
        return this;
    }

    public YAMLFactoryBuilder disable(YAMLReadFeature f) {
        _formatReadFeatures &= ~f.getMask();
        return this;
    }

    public YAMLFactoryBuilder disable(YAMLReadFeature first, YAMLReadFeature... other) {
        _formatReadFeatures &= ~first.getMask();
        for (YAMLReadFeature f : other) {
            _formatReadFeatures &= ~f.getMask();
        }
        return this;
    }

    public YAMLFactoryBuilder configure(YAMLReadFeature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    /*
    /**********************************************************************
    /* Other YAML-specific settings
    /**********************************************************************
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
    public YAMLFactoryBuilder yamlVersionToWrite(SpecVersion v) {
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
     * @param settings the {@code SnakeYAML} configuration to use when parsing YAML
     * @return This builder instance, to allow chaining
     */
    public YAMLFactoryBuilder loadSettings(LoadSettings settings) {
        _loadSettings = settings;

        // If the user wants to block duplicate keys this needs to be set in a different way to work
        if (!_loadSettings.getAllowDuplicateKeys()) {
            enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION);
        }
        return this;
    }

    /**
     * Configuration for underlying generator to follow, if specified;
     * left as {@code null} for backwards compatibility (which means
     * the dumper options are derived based on {@link YAMLWriteFeature}s).
     * <p>
     *     These {@link YAMLWriteFeature}s are ignored if you provide your own DumperOptions:
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
     */
    public YAMLFactoryBuilder dumperOptions(DumpSettings dumperOptions) {
        _dumpSettings = dumperOptions;
        return this;
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    public SpecVersion yamlVersionToWrite() {
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
     */
    public LoadSettings loadSettings() {
        return _loadSettings;
    }

    /**
     * Configuration for underlying generator to follow, if specified;
     * left as {@code null} for backwards compatibility (which means
     * the dumper options are derived based on {@link YAMLWriteFeature}s).
     * <p>
     *     These {@link YAMLWriteFeature}s are ignored if you provide your own DumperOptions:
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
     */
    public DumpSettings dumpSettings() {
        return _dumpSettings;
    }

    @Override
    public YAMLFactory build() {
        return new YAMLFactory(this);
    }
}
