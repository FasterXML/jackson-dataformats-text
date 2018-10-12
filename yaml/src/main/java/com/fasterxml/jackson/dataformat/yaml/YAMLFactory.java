package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;
import java.nio.charset.Charset;

import org.yaml.snakeyaml.DumperOptions;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.TextualTSFactory;
import com.fasterxml.jackson.core.io.IOContext;

@SuppressWarnings("resource")
public class YAMLFactory
    extends TextualTSFactory
    implements java.io.Serializable
{
	private static final long serialVersionUID = 1L;

	protected final static Charset UTF8 = Charset.forName("UTF-8");

	/**
     * Name used to identify YAML format.
     * (and returned by {@link #getFormatName()}
     */
    public final static String FORMAT_NAME_YAML = "YAML";

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */    
    protected final static int DEFAULT_YAML_GENERATOR_FEATURE_FLAGS = YAMLGenerator.Feature.collectDefaults();

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /*
    /**********************************************************************
    /* Factory construction, configuration
    /**********************************************************************
     */

    protected DumperOptions.Version _version; // enum, is serializable
    
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
    public YAMLFactory()
    {
        super(0, DEFAULT_YAML_GENERATOR_FEATURE_FLAGS);
        // 26-Jul-2013, tatu: Seems like we should force output as 1.1 but
        //  that adds version declaration which looks ugly...
        //_version = DumperOptions.Version.V1_1;
        _version = null;
    }

    public YAMLFactory(YAMLFactory src)
    {
        super(src);
        _version = src._version;
    }

    /**
     * Constructors used by {@link YAMLFactoryBuilder} for instantiation.
     *
     * @since 3.0
     */
    protected YAMLFactory(YAMLFactoryBuilder b)
    {
        super(b);
    }

    @Override
    public YAMLFactoryBuilder rebuild() {
        return new YAMLFactoryBuilder(this);
    }

    /**
     * Main factory method to use for constructing {@link YAMLFactory} instances with
     * different configuration.
     */
    public static YAMLFactoryBuilder builder() {
        return new YAMLFactoryBuilder();
    }

    @Override
    public YAMLFactory copy() {
        return new YAMLFactory(this);
    }

    /**
     * Instances are immutable so just return `this`
     */
    @Override
    public TokenStreamFactory snapshot() {
        return this;
    }

    /*
    /**********************************************************
    /* Serializable overrides
    /**********************************************************
     */

    /**
     * Method that we need to override to actually make restoration go
     * through constructors etc.
     */
    protected Object readResolve() {
        return new YAMLFactory(this);
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

    // No, we can't make use of char[] optimizations
    @Override
    public boolean canUseCharArrays() { return false; }

    @Override
    public boolean canParseAsync() {
        // 31-May-2017, tatu: No async parsing yet
        return false;
    }

    /*
    /**********************************************************
    /* Format support
    /**********************************************************
     */

    @Override
    public String getFormatName() {
        return FORMAT_NAME_YAML;
    }

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return false;
    }

    @Override
    public Class<YAMLGenerator.Feature> getFormatWriteFeatureType() {
        return YAMLGenerator.Feature.class;
    }

    /**
     * Check whether specified generator feature is enabled.
     */
    public final boolean isEnabled(YAMLGenerator.Feature f) {
        return (_formatWriteFeatures & f.getMask()) != 0;
    }

    // 04-Feb-2018, tatu: None defined yet:
    /*
    @Override
    public Class<YAMLParser.Feature> getFormatReadFeatureType() {
        return YAMLParser.Feature.class;
    }

    public final boolean isEnabled(YAMLParser.Feature f) {
        return (_formatParserFeatures & f.getMask()) != 0;
    }
    */

    @Override
    public int getFormatParserFeatures() { return 0; }

    @Override
    public int getFormatGeneratorFeatures() { return _formatWriteFeatures; }
    
    /*
    /******************************************************
    /* Factory methods: parsers
    /******************************************************
     */

    @Override
    protected YAMLParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            InputStream in) throws IOException {
        return new YAMLParser(readCtxt, ioCtxt,
                _getBufferRecycler(),
                readCtxt.getParserFeatures(_streamReadFeatures),
                _createReader(in, null, ioCtxt));
    }

    @Override
    protected YAMLParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            Reader r) throws IOException {
        return new YAMLParser(readCtxt, ioCtxt,
                _getBufferRecycler(), 
                readCtxt.getParserFeatures(_streamReadFeatures),
                r);
    }

    @Override
    protected YAMLParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            char[] data, int offset, int len,
            boolean recyclable) throws IOException {
        return new YAMLParser(readCtxt, ioCtxt, _getBufferRecycler(),
                readCtxt.getParserFeatures(_streamReadFeatures),
                new CharArrayReader(data, offset, len));
    }

    @Override
    protected YAMLParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            byte[] data, int offset, int len) throws IOException {
        return new YAMLParser(readCtxt, ioCtxt, _getBufferRecycler(),
                readCtxt.getParserFeatures(_streamReadFeatures),
                _createReader(data, offset, len, null, ioCtxt));
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            DataInput input) throws IOException {
        return _unsupported();
    }

    /*
    /******************************************************
    /* Factory methods: generators
    /******************************************************
     */

    @Override
    protected YAMLGenerator _createGenerator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, Writer out) throws IOException
    {
        return new YAMLGenerator(writeCtxt, ioCtxt,
                writeCtxt.getGeneratorFeatures(_streamWriteFeatures),
                writeCtxt.getFormatWriteFeatures(_formatWriteFeatures),
                out, _version);
    }

    @Override
    protected YAMLGenerator _createUTF8Generator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, OutputStream out) throws IOException
    {
        return _createGenerator(writeCtxt, ioCtxt,
                _createWriter(ioCtxt, out, JsonEncoding.UTF8));
    }

    @Override
    protected Writer _createWriter(IOContext ioCtxt, OutputStream out, JsonEncoding enc) throws IOException {
        if (enc == JsonEncoding.UTF8) {
            return new UTF8Writer(out);
        }
        return new OutputStreamWriter(out, enc.getJavaName());
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected Reader _createReader(InputStream in, JsonEncoding enc, IOContext ctxt) throws IOException
    {
        if (enc == null) {
            enc = JsonEncoding.UTF8;
        }
        // default to UTF-8 if encoding missing
        if (enc == JsonEncoding.UTF8) {
            boolean autoClose = ctxt.isResourceManaged() || isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE);
            return new UTF8Reader(in, autoClose);
//          return new InputStreamReader(in, UTF8);
        }
        return new InputStreamReader(in, enc.getJavaName());
    }

    protected Reader _createReader(byte[] data, int offset, int len,
            JsonEncoding enc, IOContext ctxt) throws IOException
    {
        if (enc == null) {
            enc = JsonEncoding.UTF8;
        }
        // default to UTF-8 if encoding missing
        if (enc == null || enc == JsonEncoding.UTF8) {
            return new UTF8Reader(data, offset, len, true);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(data, offset, len);
        return new InputStreamReader(in, enc.getJavaName());
    }
}
