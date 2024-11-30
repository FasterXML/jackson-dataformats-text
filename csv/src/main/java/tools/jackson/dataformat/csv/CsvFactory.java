package tools.jackson.dataformat.csv;

import java.io.*;

import tools.jackson.core.*;
import tools.jackson.core.base.TextualTSFactory;
import tools.jackson.core.io.IOContext;

import tools.jackson.dataformat.csv.impl.CsvParserBootstrapper;
import tools.jackson.dataformat.csv.impl.UTF8Writer;

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
    protected final static int DEFAULT_CSV_PARSER_FEATURE_FLAGS = CsvReadFeature.collectDefaults();

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    protected final static int DEFAULT_CSV_GENERATOR_FEATURE_FLAGS = CsvWriteFeature.collectDefaults();

    // could make it use Platform default too but...
    protected final static char[] DEFAULT_LF = { '\n' };

    protected final static CsvSchema DEFAULT_SCHEMA = CsvSchema.emptySchema();
    
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected final CsvCharacterEscapes _characterEscapes;

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
    public CsvFactory() {
        super(StreamReadConstraints.defaults(), StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                DEFAULT_CSV_PARSER_FEATURE_FLAGS,
                DEFAULT_CSV_GENERATOR_FEATURE_FLAGS);
        _characterEscapes = null; // derive from flags
    }

    protected CsvFactory(CsvFactory src)
    {
        super(src);
        _characterEscapes = src._characterEscapes;
    }

    /**
     * Constructors used by {@link CsvFactoryBuilder} for instantiation.
     *
     * @since 3.0
     */
    protected CsvFactory(CsvFactoryBuilder b)
    {
        super(b);
        _characterEscapes = b.characterEscapes();
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
    public CsvFactory copy() {
        return new CsvFactory(this);
    }

    /**
     * Instances are immutable so just return `this`
     */
    @Override
    public TokenStreamFactory snapshot() {
        return this;
    }
    
    /*
    /**********************************************************************
    /* Serializable overrides
    /**********************************************************************
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
    /**********************************************************************
    /* Capability introspection
    /**********************************************************************
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

    /*
    /**********************************************************************
    /* Format support
    /**********************************************************************
     */

    @Override
    public String getFormatName() {
        return FORMAT_NAME_CSV;
    }

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return (schema instanceof CsvSchema);
    }

    @Override
    public Class<CsvReadFeature> getFormatReadFeatureType() {
        return CsvReadFeature.class;
    }

    @Override
    public Class<CsvWriteFeature> getFormatWriteFeatureType() {
        return CsvWriteFeature.class;
    }

    @Override
    public int getFormatReadFeatures() { return _formatReadFeatures; }

    @Override
    public int getFormatWriteFeatures() { return _formatWriteFeatures; }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(CsvReadFeature f) {
        return (_formatReadFeatures & f.getMask()) != 0;
    }

    /**
     * Check whether specified generator feature is enabled.
     */
    public boolean isEnabled(CsvWriteFeature f) {
        return (_formatWriteFeatures & f.getMask()) != 0;
    }

    /*
    /**********************************************************************
    /* Factory methods: parsers
    /**********************************************************************
     */

    /**
     * Overridable factory method that actually instantiates desired parser.
     */
    @Override
    protected CsvParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            InputStream in)
    {
        return new CsvParserBootstrapper(ioCtxt, in)
            .constructParser(readCtxt,
                    readCtxt.getStreamReadFeatures(_streamReadFeatures),
                    readCtxt.getFormatReadFeatures(_formatReadFeatures),
                    _getSchema(readCtxt));
    }

    @Override
    protected CsvParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            byte[] data, int offset, int len)
    {
        return new CsvParserBootstrapper(ioCtxt, data, offset, len)
               .constructParser(readCtxt,
                       readCtxt.getStreamReadFeatures(_streamReadFeatures),
                       readCtxt.getFormatReadFeatures(_formatReadFeatures),
                       _getSchema(readCtxt));
    }

    /**
     * Overridable factory method that actually instantiates desired parser.
     */
    @Override
    protected CsvParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            Reader r)
    {
        return new CsvParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                _getSchema(readCtxt),
                r);
    }

    @Override
    protected CsvParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            char[] data, int offset, int len,
            boolean recyclable)
    {
        return new CsvParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                _getSchema(readCtxt),
                new CharArrayReader(data, offset, len));
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            DataInput input)
    {
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
    /**********************************************************************
    /* Factory methods: generators
    /**********************************************************************
     */
    
    @Override
    protected CsvGenerator _createGenerator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, Writer out)
    {
        return new CsvGenerator(writeCtxt, ioCtxt,
                writeCtxt.getStreamWriteFeatures(_streamWriteFeatures),
                writeCtxt.getFormatWriteFeatures(_formatWriteFeatures),
                out, _getSchema(writeCtxt), _characterEscapes);
    }

    @SuppressWarnings("resource")
    @Override
    protected CsvGenerator _createUTF8Generator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, OutputStream out)
    {
        return new CsvGenerator(writeCtxt, ioCtxt,
                writeCtxt.getStreamWriteFeatures(_streamWriteFeatures),
                writeCtxt.getFormatWriteFeatures(_formatWriteFeatures),
                new UTF8Writer(ioCtxt, out), _getSchema(writeCtxt),
                _characterEscapes);
    }

    private final CsvSchema _getSchema(ObjectWriteContext writeCtxt) {
        FormatSchema sch = writeCtxt.getSchema();
        if (sch == null) {
            return DEFAULT_SCHEMA;
        }
        return (CsvSchema) sch;
    }
}
