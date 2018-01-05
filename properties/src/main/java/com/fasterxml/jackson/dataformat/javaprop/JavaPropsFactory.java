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

    /*
    /**********************************************************
    /* Factory construction, configuration
    /**********************************************************
     */
    
    public JavaPropsFactory() { super(); }

    protected JavaPropsFactory(JavaPropsFactory src)
    {
        super(src);
    }

    /**
     * Constructors used by {@link CsvFactoryBuilder} for instantiation.
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
    public JavaPropsFactory copy()
    {
        return new JavaPropsFactory(this);
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

    /*
    /**********************************************************************
    /* Extended parser/generator factory methods
    /**********************************************************************
     */

    /**
     * Convenience method to allow feeding a pre-parsed {@link Properties}
     * instance as input.
     */
    public JavaPropsParser createParser(ObjectReadContext readCtxt, Properties props) {
        return new JavaPropsParser(readCtxt, _createContext(props, true),
                readCtxt.getParserFeatures(_parserFeatures),
                _getSchema(readCtxt),
                props, props);
    }

    /**
     * Convenience method to allow using a pre-constructed {@link Properties}
     * instance as output target, so that serialized property values
     * are added.
     */
    public JavaPropsGenerator createGenerator(ObjectWriteContext writeCtxt,
            Properties props)
    {
        return new PropertiesBackedGenerator(writeCtxt,
                _createContext(props, true),
                writeCtxt.getGeneratorFeatures(_generatorFeatures),
                _getSchema(writeCtxt),
                props);
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
                readCtxt.getParserFeatures(_parserFeatures),
                _getSchema(readCtxt),
                in, props);
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            Reader r) throws IOException {
        Properties props = _loadProperties(r, ioCtxt);
        return new JavaPropsParser(readCtxt, ioCtxt,
                readCtxt.getParserFeatures(_parserFeatures),
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
                writeCtxt.getGeneratorFeatures(_generatorFeatures),
                _getSchema(writeCtxt),
                out);
    }

    @Override
    protected JsonGenerator _createUTF8Generator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, OutputStream out) throws IOException
    {
        return new WriterBackedGenerator(writeCtxt, ioCtxt,
                writeCtxt.getGeneratorFeatures(_generatorFeatures),
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
        if (ctxt.isResourceManaged()) {
            try (Reader r = r0) {
                props.load(r);
            }
        } else {
            props.load(r0);
        }
        return props;
    }
}
