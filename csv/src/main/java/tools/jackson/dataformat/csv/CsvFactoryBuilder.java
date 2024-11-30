package tools.jackson.dataformat.csv;

import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.base.DecorableTSFactory.DecorableTSFBuilder;

/**
 * {@link tools.jackson.core.TSFBuilder}
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
        super(StreamReadConstraints.defaults(), StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                CsvFactory.DEFAULT_CSV_PARSER_FEATURE_FLAGS,
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

    public CsvFactoryBuilder enable(CsvReadFeature f) {
        _formatReadFeatures |= f.getMask();
        return _this();
    }

    public CsvFactoryBuilder enable(CsvReadFeature first, CsvReadFeature... other) {
        _formatReadFeatures |= first.getMask();
        for (CsvReadFeature f : other) {
            _formatReadFeatures |= f.getMask();
        }
        return _this();
    }

    public CsvFactoryBuilder disable(CsvReadFeature f) {
        _formatReadFeatures &= ~f.getMask();
        return _this();
    }

    public CsvFactoryBuilder disable(CsvReadFeature first, CsvReadFeature... other) {
        _formatReadFeatures &= ~first.getMask();
        for (CsvReadFeature f : other) {
            _formatReadFeatures &= ~f.getMask();
        }
        return _this();
    }

    public CsvFactoryBuilder configure(CsvReadFeature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    // // // Generator features

    public CsvFactoryBuilder enable(CsvWriteFeature f) {
        _formatWriteFeatures |= f.getMask();
        return _this();
    }

    public CsvFactoryBuilder enable(CsvWriteFeature first, CsvWriteFeature... other) {
        _formatWriteFeatures |= first.getMask();
        for (CsvWriteFeature f : other) {
            _formatWriteFeatures |= f.getMask();
        }
        return _this();
    }

    public CsvFactoryBuilder disable(CsvWriteFeature f) {
        _formatWriteFeatures &= ~f.getMask();
        return _this();
    }
    
    public CsvFactoryBuilder disable(CsvWriteFeature first, CsvWriteFeature... other) {
        _formatWriteFeatures &= ~first.getMask();
        for (CsvWriteFeature f : other) {
            _formatWriteFeatures &= ~f.getMask();
        }
        return _this();
    }

    public CsvFactoryBuilder configure(CsvWriteFeature f, boolean state) {
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
