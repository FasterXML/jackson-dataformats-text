package tools.jackson.dataformat.csv;

import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.base.DecorableTSFactory.DecorableTSFBuilder;

import tools.jackson.dataformat.csv.impl.CsvEncoder;

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

    /**
     * Maximum length of a column value for which quoting-need check is performed
     * when not using strict ({@link CsvWriteFeature#STRICT_CHECK_FOR_QUOTING}) checking:
     * values longer than this threshold will always be quoted.
     * Default value is {@value tools.jackson.dataformat.csv.impl.CsvEncoder#DEFAULT_MAX_QUOTE_CHECK}.
     *
     * @since 3.2
     */
    protected int _maxQuoteCheckChars = CsvEncoder.DEFAULT_MAX_QUOTE_CHECK;
    
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
        _characterEscapes = base._characterEscapes;
        _maxQuoteCheckChars = base._maxQuoteCheckChars;
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

    /**
     * Method for configuring the maximum length of column values that are
     * checked for quoting necessity when not using strict quoting
     * ({@link CsvWriteFeature#STRICT_CHECK_FOR_QUOTING}).
     * Values longer than this threshold are always quoted without checking content.
     *<p>
     * Default value is {@value tools.jackson.dataformat.csv.impl.CsvEncoder#DEFAULT_MAX_QUOTE_CHECK}.
     *
     * @param maxChars Maximum number of characters to check; values longer
     *   than this will always be quoted. Negative values are normalized to
     *   the default ({@value tools.jackson.dataformat.csv.impl.CsvEncoder#DEFAULT_MAX_QUOTE_CHECK}).
     * @return this builder (for call chaining)
     *
     * @since 3.2
     */
    public CsvFactoryBuilder maxQuoteCheckChars(int maxChars) {
        _maxQuoteCheckChars = (maxChars >= 0) ? maxChars : CsvEncoder.DEFAULT_MAX_QUOTE_CHECK;
        return this;
    }

    /**
     * @return Currently configured maximum quote check length; default
     *   is {@value tools.jackson.dataformat.csv.impl.CsvEncoder#DEFAULT_MAX_QUOTE_CHECK}.
     *
     * @since 3.2
     */
    public int maxQuoteCheckChars() {
        return _maxQuoteCheckChars;
    }
}
