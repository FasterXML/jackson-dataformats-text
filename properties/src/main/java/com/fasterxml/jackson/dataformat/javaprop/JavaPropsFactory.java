package com.fasterxml.jackson.dataformat.javaprop;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.TextualTSFactory;
import com.fasterxml.jackson.core.io.IOContext;

import com.fasterxml.jackson.dataformat.javaprop.impl.PropertiesBackedGenerator;
import com.fasterxml.jackson.dataformat.javaprop.impl.WriterBackedGenerator;
import com.fasterxml.jackson.dataformat.javaprop.io.Latin1Reader;

@SuppressWarnings("resource")
public class JavaPropsFactory
    extends TextualTSFactory
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public final static String FORMAT_NAME_JAVA_PROPERTIES = "java_properties";

    protected final static String CHARSET_ID_LATIN1 = "ISO-8859-1";

    final static JavaPropsSchema EMPTY_SCHEMA;
    static {
        EMPTY_SCHEMA = JavaPropsSchema.emptySchema();
    }

    /*
    /**********************************************************
    /* Factory construction, configuration
    /**********************************************************
     */
    
    public JavaPropsFactory() {
        // No format-specific features yet so:
        super(0, 0);
    }

    protected JavaPropsFactory(JavaPropsFactory src)
    {
        super(src);
    }

    /**
     * Constructors used by {@link JavaPropsFactoryBuilder} for instantiation.
     *
     * @since 3.0
     */
    protected JavaPropsFactory(JavaPropsFactoryBuilder b)
    {
        super(b);
    }

    @Override
    public JavaPropsFactoryBuilder rebuild() {
        return new JavaPropsFactoryBuilder(this);
    }

    /**
     * Main factory method to use for constructing {@link JavaPropsFactory} instances with
     * different configuration.
     */
    public static JavaPropsFactoryBuilder builder() {
        return new JavaPropsFactoryBuilder();
    }

    @Override
    public JavaPropsFactory copy() {
        return new JavaPropsFactory(this);
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

    // Not positional
    @Override
    public boolean requiresPropertyOrdering() {
        return false;
    }

    // Not using char[] internally
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
        return FORMAT_NAME_JAVA_PROPERTIES;
    }

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return schema instanceof JavaPropsSchema;
    }

    // No format-specific configuration, yet:
    @Override
    public Class<? extends FormatFeature> getFormatReadFeatureType() {
        return null;
    }

    @Override
    public Class<? extends FormatFeature> getFormatWriteFeatureType() {
        return null;
    }

    @Override
    public int getFormatReadFeatures() { return 0; }

    @Override
    public int getFormatWriteFeatures() { return 0; }

    /*
    /**********************************************************************
    /* Extended parser/generator factory methods
    /**********************************************************************
     */

    /**
     * Convenience method to allow feeding a pre-parsed {@link Properties}
     * (or, generally {@link java.util.Map}) instance as input.
     */
    public JavaPropsParser createParser(ObjectReadContext readCtxt,
            JavaPropsSchema schema, Map<?,?> content) {
        return new JavaPropsParser(readCtxt, _createContext(content, true),
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                schema, content, content);
    }

    /**
     * Convenience method to allow using a pre-constructed {@link Map}
     * instance as output target, so that serialized property values
     * are added.
     */
    public JavaPropsGenerator createGenerator(ObjectWriteContext writeCtxt,
            JavaPropsSchema schema, Map<?,?> target)
    {
        if (schema == null) {
            schema = EMPTY_SCHEMA;
        }
        return new PropertiesBackedGenerator(writeCtxt,
                _createContext(target, true),
                writeCtxt.getStreamWriteFeatures(_streamWriteFeatures),
                schema, target);
    }

    /*
    /**********************************************************************
    /* Overridden internal factory methods, parser
    /**********************************************************************
     */

    /* // fine as-is: 
    @Override
    protected IOContext _createContext(Object srcRef, boolean resourceManaged) {
        return super._createContext(srcRef, resourceManaged);
    }
    */

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            InputStream in) throws IOException
    {
        Properties props = _loadProperties(in, ioCtxt);
        return new JavaPropsParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                _getSchema(readCtxt),
                in, props);
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            Reader r) throws IOException {
        Properties props = _loadProperties(r, ioCtxt);
        return new JavaPropsParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                _getSchema(readCtxt),
                r, props);
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            char[] data, int offset, int len,
            boolean recyclable) throws IOException
    {
        return _createParser(readCtxt, ioCtxt, new CharArrayReader(data, offset, len));
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            byte[] data, int offset, int len) throws IOException
    {
        return _createParser(readCtxt, ioCtxt, new Latin1Reader(data, offset, len));
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ctxt,
            DataInput input) throws IOException {
        return _unsupported();
    }

    private final JavaPropsSchema _getSchema(ObjectReadContext readCtxt) {
        FormatSchema sch = readCtxt.getSchema();
        if (sch == null) {
            return JavaPropsParser.DEFAULT_SCHEMA;
        }
        return (JavaPropsSchema) sch;
    }
    
    /*
    /**********************************************************************
    /* Overridden internal factory methods, generator
    /**********************************************************************
     */
    
    @Override
    protected JsonGenerator _createGenerator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, Writer out) throws IOException
    {
        return new WriterBackedGenerator(writeCtxt, ioCtxt,
                writeCtxt.getStreamWriteFeatures(_streamWriteFeatures),
                _getSchema(writeCtxt),
                out);
    }

    @Override
    protected JsonGenerator _createUTF8Generator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, OutputStream out) throws IOException
    {
        return new WriterBackedGenerator(writeCtxt, ioCtxt,
                writeCtxt.getStreamWriteFeatures(_streamWriteFeatures),
                _getSchema(writeCtxt),
                _createWriter(ioCtxt, out, null));
    }

    @Override
    protected Writer _createWriter(IOContext ctioCtxtxt, OutputStream out, JsonEncoding enc) throws IOException
    {
        // 27-Jan-2016, tatu: Properties javadoc is quite clear on Latin-1 (ISO-8859-1) being
        //    the default, so let's actually override
        return new OutputStreamWriter(out, CHARSET_ID_LATIN1);
    }

    private final JavaPropsSchema _getSchema(ObjectWriteContext ctxt) {
        FormatSchema sch = ctxt.getSchema();
        if (sch == null) {
            return JavaPropsParser.DEFAULT_SCHEMA;
        }
        return (JavaPropsSchema) sch;
    }
    
    /*
    /**********************************************************************
    /* Low-level methods for reading/writing Properties; currently
    /* we simply delegate to `java.util.Properties`
    /**********************************************************************
     */

    protected Properties _loadProperties(InputStream in, IOContext ctxt)
        throws IOException
    {
        // NOTE: Properties default to ISO-8859-1 (aka Latin-1), NOT UTF-8; this
        // as per JDK documentation
        return _loadProperties(new Latin1Reader(ctxt, in), ctxt);
    }

    protected Properties _loadProperties(Reader r0, IOContext ctxt) throws IOException
    {
        Properties props = new Properties();
        // May or may not want to close the reader, so...
        if (ctxt.isResourceManaged() || isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE)) {
            try (Reader r = r0) {
                props.load(r);
            }
        } else {
            props.load(r0);
        }
        return props;
    }
}
