package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;
import java.net.URL;

import org.yaml.snakeyaml.DumperOptions;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker;
import org.yaml.snakeyaml.LoaderOptions;

@SuppressWarnings("resource")
public class YAMLFactory extends JsonFactory
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

    private final static byte UTF8_BOM_1 = (byte) 0xEF;
    private final static byte UTF8_BOM_2 = (byte) 0xBB;
    private final static byte UTF8_BOM_3 = (byte) 0xBF;

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected int _yamlParserFeatures = DEFAULT_YAML_PARSER_FEATURE_FLAGS;

    protected int _yamlGeneratorFeatures = DEFAULT_YAML_GENERATOR_FEATURE_FLAGS;

    /**
     * YAML version for underlying generator to follow, if specified.
     *
     * @since 2.12
     */
    protected final DumperOptions.Version _version;

    /**
     * Helper object used to determine whether property names, String values
     * must be quoted or not.
     *
     * @since 2.12
     */
    protected final StringQuotingChecker _quotingChecker;

    /**
     * Configuration for underlying parser to follow, if specified;
     * left as {@code null} for backwards compatibility (which means
     * whatever default settings {@code SnakeYAML} deems best).
     * <p>
     *     If you need to support parsing YAML files that are larger than 3Mb,
     *     it is recommended that you provide a LoaderOptions instance where
     *     you set the Codepoint Limit to a larger value than its 3Mb default.
     * </p>
     *
     * @since 2.14
     */
    protected final LoaderOptions _loaderOptions;

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
    public YAMLFactory() { this((ObjectCodec) null); }

    public YAMLFactory(ObjectCodec oc)
    {
        super(oc);
        _yamlParserFeatures = DEFAULT_YAML_PARSER_FEATURE_FLAGS;
        _yamlGeneratorFeatures = DEFAULT_YAML_GENERATOR_FEATURE_FLAGS;
        // 26-Jul-2013, tatu: Seems like we should force output as 1.1 but
        //   that adds version declaration which looks ugly...
        //_version = DumperOptions.Version.V1_1;
        _version = null;
        _quotingChecker = StringQuotingChecker.Default.instance();
        _loaderOptions = null;
    }

    /**
     * @since 2.2
     */
    public YAMLFactory(YAMLFactory src, ObjectCodec oc)
    {
        super(src, oc);
        _yamlParserFeatures = src._yamlParserFeatures;
        _yamlGeneratorFeatures = src._yamlGeneratorFeatures;
        _version = src._version;
        _quotingChecker = src._quotingChecker;
        _loaderOptions = src._loaderOptions;
    }

    /**
     * Constructors used by {@link YAMLFactoryBuilder} for instantiation.
     *
     * @since 3.0
     */
    protected YAMLFactory(YAMLFactoryBuilder b)
    {
        super(b, false);
        _yamlGeneratorFeatures = b.formatGeneratorFeaturesMask();
        _version = b.yamlVersionToWrite();
        _quotingChecker = b.stringQuotingChecker();
        _loaderOptions = b.loaderOptions();
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
    public YAMLFactory copy()
    {
        _checkInvalidCopy(YAMLFactory.class);
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
     * Also: must be overridden by sub-classes as well.
     */
    @Override
    protected Object readResolve() {
        return new YAMLFactory(this, _objectCodec);
    }

    /*                                                                                       
    /**********************************************************                              
    /* Versioned                                                                             
    /**********************************************************                              
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*
    /**********************************************************
    /* Capability introspection
    /**********************************************************
     */

    // No, we can't make use of char[] optimizations
    @Override
    public boolean canUseCharArrays() { return false; }

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
    /* Format detection functionality
    /**********************************************************
     */
    
    @Override
    public String getFormatName() {
        return FORMAT_NAME_YAML;
    }
    
    /**
     * Sub-classes need to override this method
     */
    @Override
    public MatchStrength hasFormat(InputAccessor acc) throws IOException
    {
        // Actually quite possible to do, thanks to (optional) "---"
        // indicator we may be getting...
        if (!acc.hasMoreBytes()) {
            return MatchStrength.INCONCLUSIVE;
        }
        byte b = acc.nextByte();
        // Very first thing, a UTF-8 BOM?
        if (b == UTF8_BOM_1) { // yes, looks like UTF-8 BOM
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (acc.nextByte() != UTF8_BOM_2) {
                return MatchStrength.NO_MATCH;
            }
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (acc.nextByte() != UTF8_BOM_3) {
                return MatchStrength.NO_MATCH;
            }
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            b = acc.nextByte();
        }
        // as far as I know, leading space is NOT allowed before "---" marker?
        if (b == '-' && (acc.hasMoreBytes() && acc.nextByte() == '-')
                && (acc.hasMoreBytes() && acc.nextByte() == '-')) {
            return MatchStrength.FULL_MATCH;
        }
        return MatchStrength.INCONCLUSIVE;
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

    @Override
    public int getFormatParserFeatures() {
        return _yamlParserFeatures;
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

    @Override
    public int getFormatGeneratorFeatures() {
        return _yamlGeneratorFeatures;
    }

    /*
    /**********************************************************
    /* Overridden parser factory methods
    /**********************************************************
     */

    @Override
    public YAMLParser createParser(String content) throws IOException {
        return createParser(new StringReader(content));
    }

    @Override
    public YAMLParser createParser(File f) throws IOException {
        IOContext ctxt = _createContext(_createContentReference(f), true);
        return _createParser(_decorate(new FileInputStream(f), ctxt), ctxt);
    }

    @Override
    public YAMLParser createParser(URL url) throws IOException
    {
        IOContext ctxt = _createContext(_createContentReference(url), true);
        return _createParser(_decorate(_optimizedStreamFromURL(url), ctxt), ctxt);
    }

    @Override
    public YAMLParser createParser(InputStream in) throws IOException
    {
        IOContext ctxt = _createContext(_createContentReference(in), false);
        return _createParser(_decorate(in, ctxt), ctxt);
    }

    @Override
    public YAMLParser createParser(Reader r) throws IOException
    {
        IOContext ctxt = _createContext(_createContentReference(r), false);
        return _createParser(_decorate(r, ctxt), ctxt);
    }

    @Override // since 2.4
    public YAMLParser createParser(char[] data) throws IOException {
        return createParser(data, 0, data.length);
    }
    
    @Override // since 2.4
    public YAMLParser createParser(char[] data, int offset, int len) throws IOException {
        return createParser(new CharArrayReader(data, offset, len));
    }

    @Override
    public YAMLParser createParser(byte[] data) throws IOException
    {
        IOContext ctxt = _createContext(_createContentReference(data), true);
        if (_inputDecorator != null) {
            InputStream in = _inputDecorator.decorate(ctxt, data, 0, data.length);
            if (in != null) {
                return _createParser(in, ctxt);
            }
        }
        return _createParser(data, 0, data.length, ctxt);
    }

    @Override
    public YAMLParser createParser(byte[] data, int offset, int len) throws IOException
    {
        IOContext ctxt = _createContext(_createContentReference(data, offset, len), true);
        if (_inputDecorator != null) {
            InputStream in = _inputDecorator.decorate(ctxt, data, offset, len);
            if (in != null) {
                return _createParser(in, ctxt);
            }
        }
        return _createParser(data, offset, len, ctxt);
    }
    
    /*
    /**********************************************************
    /* Overridden generator factory methods (2.1)
    /**********************************************************
     */

    @Override
    public YAMLGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ctxt = _createContext(_createContentReference(out), false);
        ctxt.setEncoding(enc);
        return _createGenerator(_createWriter(_decorate(out, ctxt), enc, ctxt), ctxt);
    }

    @Override
    public YAMLGenerator createGenerator(OutputStream out) throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ctxt = _createContext(_createContentReference(out), false);
        return _createGenerator(_createWriter(_decorate(out, ctxt),
                JsonEncoding.UTF8, ctxt), ctxt);
    }

    @Override
    public YAMLGenerator createGenerator(Writer out) throws IOException
    {
        IOContext ctxt = _createContext(_createContentReference(out), false);
        return _createGenerator(_decorate(out, ctxt), ctxt);
    }

    @Override
    public JsonGenerator createGenerator(File f, JsonEncoding enc) throws IOException
    {
        OutputStream out = new FileOutputStream(f);
        // true -> yes, we have to manage the stream since we created it
        IOContext ctxt = _createContext(_createContentReference(f), true);
        ctxt.setEncoding(enc);
        return _createGenerator(_createWriter(_decorate(out, ctxt), enc, ctxt), ctxt);
    }    

    /*
    /******************************************************
    /* Overridden internal factory methods
    /******************************************************
     */

    //protected IOContext _createContext(Object srcRef, boolean resourceManaged)

    @Override
    protected YAMLParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        return new YAMLParser(ctxt, _parserFeatures, _yamlParserFeatures,
                _loaderOptions, _objectCodec, _createReader(in, null, ctxt));
    }

    @Override
    protected YAMLParser _createParser(Reader r, IOContext ctxt) throws IOException {
        return new YAMLParser(ctxt, _parserFeatures, _yamlParserFeatures,
                _loaderOptions, _objectCodec, r);
    }

    // since 2.4
    @Override
    protected YAMLParser _createParser(char[] data, int offset, int len, IOContext ctxt,
            boolean recyclable) throws IOException {
        return new YAMLParser(ctxt, _parserFeatures, _yamlParserFeatures,
                _loaderOptions, _objectCodec, new CharArrayReader(data, offset, len));
    }

    @Override
    protected YAMLParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        return new YAMLParser(ctxt, _parserFeatures, _yamlParserFeatures,
                _loaderOptions, _objectCodec, _createReader(data, offset, len, null, ctxt));
    }

    @Override
    protected YAMLGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
        int feats = _yamlGeneratorFeatures;
        YAMLGenerator gen = new YAMLGenerator(ctxt, _generatorFeatures, feats,
                _quotingChecker, _objectCodec, out, _version);
        // any other initializations? No?
        return gen;
    }

    @Override
    protected YAMLGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        // should never get called; ensure
        throw new IllegalStateException();
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
