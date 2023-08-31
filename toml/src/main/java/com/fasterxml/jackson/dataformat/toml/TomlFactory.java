package com.fasterxml.jackson.dataformat.toml;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import com.fasterxml.jackson.core.FormatFeature;
import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.UTF8Writer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

public final class TomlFactory extends JsonFactory
{
    private static final long serialVersionUID = 1L;

    public final static String FORMAT_NAME_TOML = "toml";

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    final static int DEFAULT_TOML_PARSER_FEATURE_FLAGS = TomlReadFeature.collectDefaults();

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    final static int DEFAULT_TOML_GENERATOR_FEATURE_FLAGS = TomlWriteFeature.collectDefaults();

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected int _tomlParserFeatures;
    protected int _tomlGeneratorFeatures;

    /*
    /**********************************************************************
    /* Factory construction, configuration
    /**********************************************************************
     */

    public TomlFactory() {
        super();
        _tomlParserFeatures = DEFAULT_TOML_PARSER_FEATURE_FLAGS;
        _tomlGeneratorFeatures = DEFAULT_TOML_GENERATOR_FEATURE_FLAGS;
    }

    TomlFactory(TomlFactory src, ObjectCodec oc) {
        super(src, oc);
        _tomlGeneratorFeatures = src._tomlGeneratorFeatures;
        _tomlParserFeatures = src._tomlParserFeatures;
    }

    /**
     * Constructors used by {@link TomlFactoryBuilder} for instantiation.
     *
     * @since 3.0
     */
    TomlFactory(TomlFactoryBuilder b) {
        super(b, false);
        _tomlGeneratorFeatures = b._formatGeneratorFeatures;
        _tomlParserFeatures = b._formatParserFeatures;
    }

    @Override
    public TomlFactoryBuilder rebuild() {
        return new TomlFactoryBuilder(this);
    }

    /**
     * Main factory method to use for constructing {@link TomlFactory} instances with
     * different configuration.
     */
    public static TomlFactoryBuilder builder() {
        return new TomlFactoryBuilder();
    }

    @Override
    public TomlFactory copy() {
        _checkInvalidCopy(TomlFactory.class);
        return new TomlFactory(this, null);
    }

    /*
    /**********************************************************************
    /* Introspection
    /**********************************************************************
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    @Override
    public boolean requiresPropertyOrdering() {
        return false;
    }

    @Override
    public boolean canUseCharArrays() {
        return false;
    }

    @Override
    public boolean canParseAsync() {
        return false;
    }

    /*
    /**********************************************************************
    /* Format support
    /**********************************************************************
     */

    @Override
    public String getFormatName() {
        return FORMAT_NAME_TOML;
    }

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return false;
    }

    @Override
    public Class<? extends FormatFeature> getFormatReadFeatureType() {
        return TomlReadFeature.class;
    }

    @Override
    public Class<? extends FormatFeature> getFormatWriteFeatureType() {
        return TomlWriteFeature.class;
    }

    @Override
    public MatchStrength hasFormat(InputAccessor acc) throws IOException {
        return MatchStrength.INCONCLUSIVE;
    }

    /*
    /**********************************************************************
    /* Configuration, parser settings
    /**********************************************************************
     */

    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link TomlReadFeature} for list of features)
     */
    public final TomlFactory configure(TomlReadFeature f, boolean state)
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
     * (check {@link TomlReadFeature} for list of features)
     */
    public TomlFactory enable(TomlReadFeature f) {
        _tomlParserFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified parser features
     * (check {@link TomlReadFeature} for list of features)
     */
    public TomlFactory disable(TomlReadFeature f) {
        _tomlParserFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(TomlReadFeature f) {
        return (_tomlParserFeatures & f.getMask()) != 0;
    }

    @Override
    public int getFormatParserFeatures() {
        return _tomlParserFeatures;
    }

    /*
    /**********************************************************************
    /* Configuration, generator settings
    /**********************************************************************
     */

    /**
     * Method for enabling or disabling specified generator feature
     * (check {@link TomlWriteFeature} for list of features)
     */
    public final TomlFactory configure(TomlWriteFeature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /**
     * Method for enabling specified generator feature
     * (check {@link TomlWriteFeature} for list of features)
     */
    public TomlFactory enable(TomlWriteFeature f) {
        _tomlGeneratorFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified generator features
     * (check {@link TomlWriteFeature} for list of features)
     */
    public TomlFactory disable(TomlWriteFeature f) {
        _tomlGeneratorFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified generator feature is enabled.
     */
    public final boolean isEnabled(TomlWriteFeature f) {
        return (_tomlGeneratorFeatures & f.getMask()) != 0;
    }

    @Override
    public int getFormatGeneratorFeatures() {
        return _tomlGeneratorFeatures;
    }

    /*
    /**********************************************************************
    /* Overridden internal factory methods, parser
    /**********************************************************************
     */

    @Override
    public JsonParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        // "A TOML file must be a valid UTF-8 encoded Unicode document."
        boolean autoClose = ctxt.isResourceManaged() || isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        return _createParser(UTF8Reader.construct(ctxt, in, autoClose),
                ctxt);
    }

    @Override
    public JsonParser _createParser(Reader r, IOContext ctxt) throws IOException {
        try {
            ObjectNode node = parse(ctxt, r);
            return new TreeTraversingParser(node); // don't pass our _objectCodec, this part shouldn't be customized
        } finally {
            ctxt.close();
        }
    }

    @Override
    public JsonParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        return _createParser(UTF8Reader.construct(data, offset, len), ctxt);
    }

    @Override
    protected JsonParser _createParser(char[] data, int offset, int len, IOContext ctxt, boolean recyclable) throws IOException {
        return _createParser(new CharArrayReader(data, offset, len), ctxt);
    }

    /*
    /**********************************************************************
    /* Overridden internal factory methods, generator
    /**********************************************************************
     */

    @Override
    protected JsonGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
        return new TomlGenerator(ctxt, _generatorFeatures, _tomlGeneratorFeatures,
                _objectCodec, out);
    }

    @Override
    protected JsonGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        return new TomlGenerator(ctxt, _generatorFeatures, _tomlGeneratorFeatures,
                _objectCodec, new UTF8Writer(ctxt, out));
    }

    /*
    /**********************************************************************
    /* Low-level methods for reading/writing TOML
    /**********************************************************************
     */

    private ObjectNode parse(IOContext ctxt, Reader r0) throws IOException {
        if (ctxt.isResourceManaged() || isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE)) {
            try (Reader r = r0) {
                return Parser.parse(this, ctxt, r);
            }
        } else {
            return Parser.parse(this, ctxt, r0);
        }
    }
}
