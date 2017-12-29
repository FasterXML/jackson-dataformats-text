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
    public CsvFactory() { super(); }

    protected CsvFactory(CsvFactory src)
    {
        super(src);
        _csvParserFeatures = src._csvParserFeatures;
        _csvGeneratorFeatures = src._csvGeneratorFeatures;
    }

    /**
     * Constructors used by {@link CsvFactoryBuilder} for instantiation.
     *
     * @since 3.0
     */
    protected CsvFactory(CsvFactoryBuilder b)
    {
        super(b);
    }

    @Override
    public CsvFactoryBuilder rebuild() {
        return new CsvFactoryBuilder(this);
    }

    /**
     * Main factory method to use for constructing {@link CsvFactory} instances with
     * different configuration.
     */
    public static CsvFactoryBuilder builder() {
        return new CsvFactoryBuilder();
    }

    @Override
    public CsvFactory copy()
    {
        return new CsvFactory(this);
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
        return new CsvFactory(this);
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
    protected CsvParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            InputStream in) throws IOException {
        return new CsvParserBootstrapper(ioCtxt, in)
            .constructParser(readCtxt,
                    readCtxt.getParserFeatures(_parserFeatures),
                    readCtxt.getFormatReadFeatures(_csvParserFeatures),
                    _getSchema(readCtxt));
    }

    @Override
    protected CsvParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            byte[] data, int offset, int len) throws IOException {
        return new CsvParserBootstrapper(ioCtxt, data, offset, len)
               .constructParser(readCtxt,
                       readCtxt.getParserFeatures(_parserFeatures),
                       readCtxt.getFormatReadFeatures(_csvParserFeatures),
                       _getSchema(readCtxt));
    }

    /**
     * Overridable factory method that actually instantiates desired parser.
     */
    @Override
    protected CsvParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            Reader r) throws IOException {
        return new CsvParser(readCtxt, (CsvIOContext) ioCtxt,
                readCtxt.getParserFeatures(_parserFeatures),
                readCtxt.getFormatReadFeatures(_csvParserFeatures),
                _getSchema(readCtxt),
                r);
    }

    @Override
    protected CsvParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            char[] data, int offset, int len,
            boolean recyclable) throws IOException
    {
        return new CsvParser(readCtxt, (CsvIOContext) ioCtxt,
                readCtxt.getParserFeatures(_parserFeatures),
                readCtxt.getFormatReadFeatures(_csvParserFeatures),
                _getSchema(readCtxt),
                new CharArrayReader(data, offset, len));
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            DataInput input) throws IOException {
        return _unsupported();
    }

    private final CsvSchema _getSchema(ObjectReadContext readCtxt) {
        FormatSchema sch = readCtxt.getSchema();
        if (sch == null) {
            return DEFAULT_SCHEMA;
        }
        return (CsvSchema) sch;
    }
    
    /*
    /******************************************************
    /* Factory methods: generators
    /******************************************************
     */
    
    @Override
    protected CsvGenerator _createGenerator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, Writer out) throws IOException
    {
        return new CsvGenerator(writeCtxt, ioCtxt,
                writeCtxt.getGeneratorFeatures(_generatorFeatures),
                writeCtxt.getFormatWriteFeatures(_csvGeneratorFeatures),
                out, _getSchema(writeCtxt));
    }

    @SuppressWarnings("resource")
    @Override
    protected CsvGenerator _createUTF8Generator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, OutputStream out) throws IOException
    {
        return new CsvGenerator(writeCtxt, ioCtxt,
                writeCtxt.getGeneratorFeatures(_generatorFeatures),
                writeCtxt.getFormatWriteFeatures(_csvGeneratorFeatures),
                new UTF8Writer(ioCtxt, out), _getSchema(writeCtxt));
    }

    private final CsvSchema _getSchema(ObjectWriteContext writeCtxt) {
        FormatSchema sch = writeCtxt.getSchema();
        if (sch == null) {
            return DEFAULT_SCHEMA;
        }
        return (CsvSchema) sch;
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
