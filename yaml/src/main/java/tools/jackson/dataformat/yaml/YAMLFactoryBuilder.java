package tools.jackson.dataformat.yaml;

import tools.jackson.core.base.DecorableTSFactory.DecorableTSFBuilder;
import tools.jackson.dataformat.yaml.util.StringQuotingChecker;

import org.snakeyaml.engine.v2.common.SpecVersion;

/**
 * {@link tools.jackson.core.TokenStreamFactory.TSFBuilder}
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
     */
    protected SpecVersion _version;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    protected YAMLFactoryBuilder() {
        super(0, YAMLFactory.DEFAULT_YAML_GENERATOR_FEATURE_FLAGS);
    }

    public YAMLFactoryBuilder(YAMLFactory base) {
        super(base);
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
        _formatWriteFeatures |= f.getMask();
        return this;
    }

    public YAMLFactoryBuilder enable(YAMLGenerator.Feature first, YAMLGenerator.Feature... other) {
        _formatWriteFeatures |= first.getMask();
        for (YAMLGenerator.Feature f : other) {
            _formatWriteFeatures |= f.getMask();
        }
        return this;
    }

    public YAMLFactoryBuilder disable(YAMLGenerator.Feature f) {
        _formatWriteFeatures &= ~f.getMask();
        return this;
    }

    public YAMLFactoryBuilder disable(YAMLGenerator.Feature first, YAMLGenerator.Feature... other) {
        _formatWriteFeatures &= ~first.getMask();
        for (YAMLGenerator.Feature f : other) {
            _formatWriteFeatures &= ~f.getMask();
        }
        return this;
    }

    public YAMLFactoryBuilder configure(YAMLGenerator.Feature f, boolean state) {
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

    @Override
    public YAMLFactory build() {
        return new YAMLFactory(this);
    }
}
