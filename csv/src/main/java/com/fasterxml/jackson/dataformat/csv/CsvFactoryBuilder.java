package com.fasterxml.jackson.dataformat.csv;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.base.DecorableTSFactory.DecorableTSFBuilder;

/**
 * {@link com.fasterxml.jackson.core.TokenStreamFactory.TSFBuilder}
 * implementation for constructing {@link CsvFactory}
 * instances.
 *
 * @since 3.0
 */
public class CsvFactoryBuilder extends DecorableTSFBuilder<CsvFactory, CsvFactoryBuilder>
{
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected CsvCharacterEscapes _characterEscapes;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    protected CsvFactoryBuilder() {
        super(CsvFactory.DEFAULT_CSV_PARSER_FEATURE_FLAGS,
                CsvFactory.DEFAULT_CSV_GENERATOR_FEATURE_FLAGS);
    }

    public CsvFactoryBuilder(CsvFactory base) {
        super(base);
    }

    @Override
    public CsvFactory build() {
        // 28-Dec-2017, tatu: No special settings beyond base class ones, so:
        return new CsvFactory(this);
    }

    // // // Parser features

    public CsvFactoryBuilder enable(CsvParser.Feature f) {
        _formatReadFeatures |= f.getMask();
        return _this();
    }

    public CsvFactoryBuilder enable(CsvParser.Feature first, CsvParser.Feature... other) {
        _formatReadFeatures |= first.getMask();
        for (CsvParser.Feature f : other) {
            _formatReadFeatures |= f.getMask();
        }
        return _this();
    }

    public CsvFactoryBuilder disable(CsvParser.Feature f) {
        _formatReadFeatures &= ~f.getMask();
        return _this();
    }

    public CsvFactoryBuilder disable(CsvParser.Feature first, CsvParser.Feature... other) {
        _formatReadFeatures &= ~first.getMask();
        for (CsvParser.Feature f : other) {
            _formatReadFeatures &= ~f.getMask();
        }
        return _this();
    }

    public CsvFactoryBuilder configure(CsvParser.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    // // // Generator features

    public CsvFactoryBuilder enable(CsvGenerator.Feature f) {
        _formatWriteFeatures |= f.getMask();
        return _this();
    }

    public CsvFactoryBuilder enable(CsvGenerator.Feature first, CsvGenerator.Feature... other) {
        _formatWriteFeatures |= first.getMask();
        for (CsvGenerator.Feature f : other) {
            _formatWriteFeatures |= f.getMask();
        }
        return _this();
    }

    public CsvFactoryBuilder disable(CsvGenerator.Feature f) {
        _formatWriteFeatures &= ~f.getMask();
        return _this();
    }
    
    public CsvFactoryBuilder disable(CsvGenerator.Feature first, CsvGenerator.Feature... other) {
        _formatWriteFeatures &= ~first.getMask();
        for (CsvGenerator.Feature f : other) {
            _formatWriteFeatures &= ~f.getMask();
        }
        return _this();
    }

    public CsvFactoryBuilder configure(CsvGenerator.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    // // // Other CSV-specific configuration
    
    /**
     * Method for defining custom escapes factory uses for {@link JsonGenerator}s
     * it creates.
     */
    public CsvFactoryBuilder characterEscapes(CsvCharacterEscapes esc) {
        _characterEscapes = esc;
        return this;
    }

    public CsvCharacterEscapes characterEscapes() {
        if (_characterEscapes == null) {
            
        }
        return _characterEscapes;
    }
}
