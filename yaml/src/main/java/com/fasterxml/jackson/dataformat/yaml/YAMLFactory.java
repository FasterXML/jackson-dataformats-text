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

	/**
     * Name used to identify YAML format.
     * (and returned by {@link #getFormatName()}
     */
    public final static String FORMAT_NAME_YAML = "YAML";

    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
    protected final static int DEFAULT_YAML_PARSER_FEATURE_FLAGS = YAMLParser.Feature.collectDefaults();

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

    protected int _yamlParserFeatures = DEFAULT_YAML_PARSER_FEATURE_FLAGS;

    protected int _yamlGeneratorFeatures = DEFAULT_YAML_GENERATOR_FEATURE_FLAGS;
    
    /*
    /**********************************************************************
    /* Factory construction, configuration
    /**********************************************************************
     */

    protected DumperOptions.Version _version;
    
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
    public YAMLFactory() { this(null); }

    public YAMLFactory(ObjectCodec oc)
    {
        super(oc);
        _yamlParserFeatures = DEFAULT_YAML_PARSER_FEATURE_FLAGS;
        _yamlGeneratorFeatures = DEFAULT_YAML_GENERATOR_FEATURE_FLAGS;
        /* 26-Jul-2013, tatu: Seems like we should force output as 1.1 but
         *   that adds version declaration which looks ugly...
         */
        //_version = DumperOptions.Version.V1_1;
        _version = null;
    }

    public YAMLFactory(YAMLFactory src, ObjectCodec oc)
    {
        super(src, oc);
        _version = src._version;
        _yamlParserFeatures = src._yamlParserFeatures;
        _yamlGeneratorFeatures = src._yamlGeneratorFeatures;
    }

    @Override
    public YAMLFactory copy()
    {
        return new YAMLFactory(this, null);
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
        return new YAMLFactory(this, _objectCodec);
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

    @Override
    public Class<YAMLParser.Feature> getFormatReadFeatureType() {
        return YAMLParser.Feature.class;
    }

    @Override
    public Class<YAMLGenerator.Feature> getFormatWriteFeatureType() {
        return YAMLGenerator.Feature.class;
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

    /*
    /**********************************************************
    /* Configuration, parser settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link YAMLParser.Feature} for list of features)
     */
    public final YAMLFactory configure(YAMLParser.Feature f, boolean state)
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
     * (check {@link YAMLParser.Feature} for list of features)
     */
    public YAMLFactory enable(YAMLParser.Feature f) {
        _yamlParserFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified parser features
     * (check {@link YAMLParser.Feature} for list of features)
     */
    public YAMLFactory disable(YAMLParser.Feature f) {
        _yamlParserFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(YAMLParser.Feature f) {
        return (_yamlParserFeatures & f.getMask()) != 0;
    }

    /*
    /**********************************************************
    /* Configuration, generator settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified generator feature
     * (check {@link YAMLGenerator.Feature} for list of features)
     */
    public final YAMLFactory configure(YAMLGenerator.Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }


    /**
     * Method for enabling specified generator features
     * (check {@link YAMLGenerator.Feature} for list of features)
     */
    public YAMLFactory enable(YAMLGenerator.Feature f) {
        _yamlGeneratorFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified generator feature
     * (check {@link YAMLGenerator.Feature} for list of features)
     */
    public YAMLFactory disable(YAMLGenerator.Feature f) {
        _yamlGeneratorFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Check whether specified generator feature is enabled.
     */
    public final boolean isEnabled(YAMLGenerator.Feature f) {
        return (_yamlGeneratorFeatures & f.getMask()) != 0;
    }

    /*
    /******************************************************
    /* Factory methods: parsers
    /******************************************************
     */

    @Override
    protected YAMLParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        return new YAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures,
                _objectCodec, _createReader(in, null, ctxt));
    }

    @Override
    protected YAMLParser _createParser(Reader r, IOContext ctxt) throws IOException {
        return new YAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures,
                _objectCodec, r);
    }

    @Override
    protected YAMLParser _createParser(char[] data, int offset, int len, IOContext ctxt,
            boolean recyclable) throws IOException {
        return new YAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures,
                _objectCodec, new CharArrayReader(data, offset, len));
    }

    @Override
    protected YAMLParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        return new YAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures,
                _objectCodec, _createReader(data, offset, len, null, ctxt));
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
    protected YAMLGenerator _createGenerator(ObjectWriteContext writeCtxt,
            Writer out, IOContext ctxt) throws IOException {
        return new YAMLGenerator(ctxt,
                writeCtxt.getGeneratorFeatures(_generatorFeatures),
                writeCtxt.getFormatWriteFeatures(_yamlGeneratorFeatures),
                _objectCodec, out, _version);
    }

    @Override
    protected YAMLGenerator _createUTF8Generator(ObjectWriteContext writeCtxt,
            OutputStream out, IOContext ctxt) throws IOException {
        return _createGenerator(writeCtxt,
                _createWriter(out, JsonEncoding.UTF8, ctxt), ctxt);
    }

    @Override
    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException {
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

    protected final Charset UTF8 = Charset.forName("UTF-8");

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
