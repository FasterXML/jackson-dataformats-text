package com.fasterxml.jackson.dataformat.csv;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.TextualTSFactory;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.csv.impl.CsvIOContext;
import com.fasterxml.jackson.dataformat.csv.impl.CsvParserBootstrapper;
import com.fasterxml.jackson.dataformat.csv.impl.UTF8Writer;

public class CsvFactory
    extends TextualTSFactory
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Name used to identify CSV format.
     * (and returned by {@link #getFormatName()}
     */
    public final static String FORMAT_NAME_CSV = "CSV";
    
    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
    final static int DEFAULT_CSV_PARSER_FEATURE_FLAGS = CsvParser.Feature.collectDefaults();

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    final static int DEFAULT_CSV_GENERATOR_FEATURE_FLAGS = CsvGenerator.Feature.collectDefaults();

    // could make it use Platform default too but...
    protected final static char[] DEFAULT_LF = { '\n' };

    protected final static CsvSchema DEFAULT_SCHEMA = CsvSchema.emptySchema();
    
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected CsvSchema _schema = DEFAULT_SCHEMA;
    
    protected int _csvParserFeatures = DEFAULT_CSV_PARSER_FEATURE_FLAGS;

    protected int _csvGeneratorFeatures = DEFAULT_CSV_GENERATOR_FEATURE_FLAGS;

    /*
    protected char _cfgColumnSeparator = ',';

    protected char _cfgQuoteCharacter = '"';
    
    protected char[] _cfgLineSeparator = DEFAULT_LF;
    */
    
    /*
    /**********************************************************************
    /* Factory construction, configuration
    /**********************************************************************
     */

    /**
     * Default constructor used to create factory instances.
     * Creation of a factory instance is a light-weight operation,
     * but it is still a good idea to reuse limited number of
     * factory instances (and quite often just a single instance):
     * factories are used as context for storing some reused
     * processing objects (such as symbol tables parsers use)
     * and this reuse only works within context of a single
     * factory instance.
     */
    public CsvFactory() { this(null); }

    public CsvFactory(ObjectCodec oc) { super(oc); }

    protected CsvFactory(CsvFactory src, ObjectCodec oc)
    {
        super(src, oc);
        _csvParserFeatures = src._csvParserFeatures;
        _csvGeneratorFeatures = src._csvGeneratorFeatures;
        _schema = src._schema;
    }
    
    @Override
    public CsvFactory copy()
    {
        return new CsvFactory(this, null);
    }

    /*
    /**********************************************************
    /* Serializable overrides
    /**********************************************************
     */

    /**
     * Method that we need to override to actually make restoration go
     * through constructors etc.
     * Also: must be overridden by sub-classes as well.
     */
    protected Object readResolve() {
        return new CsvFactory(this, _objectCodec);
    }

    /*
    /**********************************************************
    /* Capability introspection
    /**********************************************************
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    // Yes; CSV is positional
    @Override
    public boolean requiresPropertyOrdering() {
        return true;
    }

    // No, we can't make use of char[] optimizations
    @Override
    public boolean canUseCharArrays() { return false; }

    @Override
    public boolean canParseAsync() {
        // 30-Sep-2017, tatu: No async parsing yet
        return false;
    }

    @Override
    public Class<CsvParser.Feature> getFormatReadFeatureType() {
        return CsvParser.Feature.class;
    }

    @Override
    public Class<CsvGenerator.Feature> getFormatWriteFeatureType() {
        return CsvGenerator.Feature.class;
    }

    /*
    /**********************************************************
    /* Format support
    /**********************************************************
     */

    @Override
    public String getFormatName() {
        return FORMAT_NAME_CSV;
    }

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return (schema instanceof CsvSchema);
    }

    /*
    /**********************************************************
    /* Configuration, parser settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link CsvParser.Feature} for list of features)
     */
    public final CsvFactory configure(CsvParser.Feature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /**
     * Method for enabling specified parser feature
     * (check {@link CsvParser.Feature} for list of features)
     */
    public CsvFactory enable(CsvParser.Feature f) {
        _csvParserFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified parser features
     * (check {@link CsvParser.Feature} for list of features)
     */
    public CsvFactory disable(CsvParser.Feature f) {
        _csvParserFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(CsvParser.Feature f) {
        return (_csvParserFeatures & f.getMask()) != 0;
    }

    /*
    /**********************************************************
    /* Configuration, generator settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified generator feature
     * (check {@link CsvGenerator.Feature} for list of features)
     */
    public final CsvFactory configure(CsvGenerator.Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /**
     * Method for enabling specified generator features
     * (check {@link CsvGenerator.Feature} for list of features)
     */
    public CsvFactory enable(CsvGenerator.Feature f) {
        _csvGeneratorFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified generator feature
     * (check {@link CsvGenerator.Feature} for list of features)
     */
    public CsvFactory disable(CsvGenerator.Feature f) {
        _csvGeneratorFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Check whether specified generator feature is enabled.
     */
    public final boolean isEnabled(CsvGenerator.Feature f) {
        return (_csvGeneratorFeatures & f.getMask()) != 0;
    }

    /*
    /******************************************************
    /* Factory methods: parsers
    /******************************************************
     */

    /**
     * Overridable factory method that actually instantiates desired parser.
     */
    @Override
    protected CsvParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        return new CsvParserBootstrapper(ctxt, _objectCodec, in)
            .constructParser(_parserFeatures, _csvParserFeatures);
    }

    @Override
    protected CsvParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        return new CsvParserBootstrapper(ctxt, _objectCodec, data, offset, len)
               .constructParser(_parserFeatures, _csvParserFeatures);
    }

    /**
     * Overridable factory method that actually instantiates desired parser.
     */
    @Override
    protected CsvParser _createParser(Reader r, IOContext ctxt) throws IOException {
        return new CsvParser((CsvIOContext) ctxt, _parserFeatures, _csvParserFeatures,
                _objectCodec, r);
    }

    @Override
    protected CsvParser _createParser(char[] data, int offset, int len, IOContext ctxt,
            boolean recyclable) throws IOException {
        return new CsvParser((CsvIOContext) ctxt, _parserFeatures, _csvParserFeatures,
                _objectCodec, new CharArrayReader(data, offset, len));
    }

    @Override
    protected JsonParser _createParser(DataInput input, IOContext ctxt) throws IOException {
        return _unsupported();
    }

    /*
    /******************************************************
    /* Factory methods: generators
    /******************************************************
     */
    
    @Override
    protected CsvGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
        return new CsvGenerator(ctxt, _generatorFeatures, _csvGeneratorFeatures,
                _objectCodec, out, _schema);
    }

    @SuppressWarnings("resource")
    @Override
    protected CsvGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        return new CsvGenerator(ctxt, _generatorFeatures, _csvGeneratorFeatures,
                _objectCodec, new UTF8Writer(ctxt, out), _schema);
    }
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    @Override
    protected IOContext _createContext(Object srcRef, boolean resourceManaged) {
        return new CsvIOContext(_getBufferRecycler(), srcRef, resourceManaged);
    }
}
