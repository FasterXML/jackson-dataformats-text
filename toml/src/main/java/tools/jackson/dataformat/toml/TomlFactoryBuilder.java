package tools.jackson.dataformat.toml;

import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.base.DecorableTSFactory;

/**
 * {@link tools.jackson.core.TSFBuilder}
 * implementation for constructing {@link TomlFactory}
 * instances.
 */
public class TomlFactoryBuilder extends DecorableTSFactory.DecorableTSFBuilder<TomlFactory, TomlFactoryBuilder>
{
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    TomlFactoryBuilder() {
        super(StreamReadConstraints.defaults(), StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                TomlFactory.DEFAULT_TOML_PARSER_FEATURE_FLAGS, TomlFactory.DEFAULT_TOML_GENERATOR_FEATURE_FLAGS);
    }

    TomlFactoryBuilder(TomlFactory base) {
        super(base);
    }

    @Override
    public TomlFactory build() {
        return new TomlFactory(this);
    }

    /*
    /**********************************************************************
    /* Parser feature setting
    /**********************************************************************
     */

    public TomlFactoryBuilder enable(TomlReadFeature f) {
        _formatReadFeatures |= f.getMask();
        return this;
    }

    public TomlFactoryBuilder enable(TomlReadFeature first, TomlReadFeature... other) {
        _formatReadFeatures |= first.getMask();
        for (TomlReadFeature f : other) {
            _formatReadFeatures |= f.getMask();
        }
        return this;
    }

    public TomlFactoryBuilder disable(TomlReadFeature f) {
        _formatReadFeatures &= ~f.getMask();
        return this;
    }

    public TomlFactoryBuilder disable(TomlReadFeature first, TomlReadFeature... other) {
        _formatReadFeatures &= ~first.getMask();
        for (TomlReadFeature f : other) {
            _formatReadFeatures &= ~f.getMask();
        }
        return this;
    }

    public TomlFactoryBuilder configure(TomlReadFeature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    /*
    /**********************************************************************
    /* Generator feature setting
    /**********************************************************************
     */

    public TomlFactoryBuilder enable(TomlWriteFeature f) {
        _formatWriteFeatures |= f.getMask();
        return this;
    }

    public TomlFactoryBuilder enable(TomlWriteFeature first, TomlWriteFeature... other) {
        _formatWriteFeatures |= first.getMask();
        for (TomlWriteFeature f : other) {
            _formatWriteFeatures |= f.getMask();
        }
        return this;
    }

    public TomlFactoryBuilder disable(TomlWriteFeature f) {
        _formatWriteFeatures &= ~f.getMask();
        return this;
    }

    public TomlFactoryBuilder disable(TomlWriteFeature first, TomlWriteFeature... other) {
        _formatWriteFeatures &= ~first.getMask();
        for (TomlWriteFeature f : other) {
            _formatWriteFeatures &= ~f.getMask();
        }
        return this;
    }

    public TomlFactoryBuilder configure(TomlWriteFeature f, boolean state) {
        return state ? enable(f) : disable(f);
    }
}
