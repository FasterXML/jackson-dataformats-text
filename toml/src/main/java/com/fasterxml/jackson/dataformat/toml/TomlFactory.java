package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.TextualTSFactory;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.UTF8Writer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import java.io.*;
import java.nio.charset.StandardCharsets;

public final class TomlFactory extends TextualTSFactory {

    public final static String FORMAT_NAME_TOML = "toml";

    /*
    /**********************************************************************
    /* Factory construction, configuration
    /**********************************************************************
     */

    public TomlFactory() {
        // No format-specific features yet so:
        super(0, 0);
    }

    TomlFactory(TomlFactory src) {
        super(src);
    }

    /**
     * Constructors used by {@link TomlFactoryBuilder} for instantiation.
     *
     * @since 3.0
     */
    TomlFactory(TomlFactoryBuilder b) {
        super(b);
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
        return new TomlFactory(this);
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
        return null;
    }

    @Override
    public Class<? extends FormatFeature> getFormatWriteFeatureType() {
        return null;
    }

    @Override
    public int getFormatReadFeatures() {
        return 0;
    }

    @Override
    public int getFormatWriteFeatures() {
        return 0;
    }

    /*
    /**********************************************************************
    /* Overridden internal factory methods, parser
    /**********************************************************************
     */

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ctxt, InputStream in) throws JacksonException {
        // "A TOML file must be a valid UTF-8 encoded Unicode document."
        return _createParser(readCtxt, ctxt, new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ctxt, Reader r) throws JacksonException {
        ObjectNode node = parse(r, ctxt);
        return new TreeTraversingParser(node, readCtxt);
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ctxt, byte[] data, int offset, int len) throws JacksonException {
        return _createParser(readCtxt, ctxt, new ByteArrayInputStream(data, offset, len));
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ctxt, char[] data, int offset, int len, boolean recyclable) throws JacksonException {
        return _createParser(readCtxt, ctxt, new CharArrayReader(data, offset, len));
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ctxt, DataInput input) throws JacksonException {
        return _unsupported();
    }

    /*
    /**********************************************************************
    /* Overridden internal factory methods, generator
    /**********************************************************************
     */

    @Override
    protected JsonGenerator _createGenerator(ObjectWriteContext writeCtxt, IOContext ioCtxt, Writer out) throws JacksonException {
        return _unsupported(); // TODO
    }

    @Override
    protected JsonGenerator _createUTF8Generator(ObjectWriteContext writeCtxt, IOContext ioCtxt, OutputStream out) throws JacksonException {
        return _unsupported(); // TODO
    }

    @Override
    protected Writer _createWriter(IOContext ioCtxt, OutputStream out, JsonEncoding enc) throws JacksonException {
        // "A TOML file must be a valid UTF-8 encoded Unicode document."
        return new UTF8Writer(ioCtxt, out);
    }

    /*
    /**********************************************************************
    /* Low-level methods for reading/writing TOML
    /**********************************************************************
     */

    private ObjectNode parse(Reader r0, IOContext ctxt) {
        JacksonTomlParseException.ErrorContext errorContext = new JacksonTomlParseException.ErrorContext(ctxt.sourceReference(), null);
        ParserOptions options = ParserOptions.DEFAULT;
        try {
            if (ctxt.isResourceManaged() || isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE)) {
                try (Reader r = r0) {
                    return Parser.parse(errorContext, options, r);
                }
            } else {
                return Parser.parse(errorContext, options, r0);
            }
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
    }
}
