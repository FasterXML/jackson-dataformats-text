package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.base.DecorableTSFactory.DecorableTSFBuilder;

import com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker;

/**
 * {@link com.fasterxml.jackson.core.TokenStreamFactory.TSFBuilder}
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
        _quotingChecker = base._quotingChecker;
    }

    // // // Parser features NOT YET defined

    // // // Generator features

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

    public YAMLFactoryBuilder stringQuotingChecker(StringQuotingChecker sqc) {
        _quotingChecker = sqc;
        return this;
    }

    // // // Accessors

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
