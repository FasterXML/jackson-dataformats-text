package com.fasterxml.jackson.dataformat.javaprop;

import java.io.*;
import java.net.URL;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.javaprop.impl.PropertiesBackedGenerator;
import com.fasterxml.jackson.dataformat.javaprop.impl.WriterBackedGenerator;
import com.fasterxml.jackson.dataformat.javaprop.io.Latin1Reader;

@SuppressWarnings("resource")
public class JavaPropsFactory extends JsonFactory
{
    private static final long serialVersionUID = 1L;

    public final static String FORMAT_NAME_JAVA_PROPERTIES = "java_properties";

    protected final static String CHARSET_ID_LATIN1 = "ISO-8859-1";

    /*
    /**********************************************************
    /* Factory construction, configuration
    /**********************************************************
     */
    
    public JavaPropsFactory() { }

    public JavaPropsFactory(ObjectCodec codec) {
        super(codec);
    }

    protected JavaPropsFactory(JavaPropsFactory src, ObjectCodec oc)
    {
        super(src, oc);
    }

    @Override
    public JavaPropsFactory copy()
    {
        _checkInvalidCopy(JavaPropsFactory.class);
        return new JavaPropsFactory(this, null);
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
    /* Format detection functionality
    /**********************************************************
     */
    
    @Override
    public String getFormatName() {
        return FORMAT_NAME_JAVA_PROPERTIES;
    }
    
    /**
     * Sub-classes need to override this method
     */
    @Override
    public MatchStrength hasFormat(InputAccessor acc) throws IOException
    {
        // TODO, if possible... probably isn't?
        return MatchStrength.INCONCLUSIVE;
    }

    /*
    /**********************************************************
    /* Capability introspection
    /**********************************************************
     */

    // Not positional
    @Override
    public boolean requiresPropertyOrdering() {
        return false;
    }

    // Can not handle raw binary data
    @Override
    public boolean canHandleBinaryNatively() {
        return false;
    }

    // Not using char[] internally
    @Override
    public boolean canUseCharArrays() { return false; }

    // No format-specific configuration, yet:
/*    
    @Override
    public Class<? extends FormatFeature> getFormatReadFeatureType() {
        return null;
    }

    @Override
    public Class<? extends FormatFeature> getFormatWriteFeatureType() {
        return null;
    }
*/

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return schema instanceof JavaPropsSchema;
    }

    /*
    /**********************************************************
    /* Extended parser/generator factory methods
    /**********************************************************
     */

    /**
     * Convenience method to allow feeding a pre-parsed {@link Properties}
     * instance as input.
     *
     * @since 2.9
     */
    public JavaPropsParser createParser(Properties props) {
        return new JavaPropsParser(_createContext(props, true),
                props, _parserFeatures, _objectCodec, props);
    }

    /**
     * Convenience method to allow using a pre-constructed {@link Properties}
     * instance as output target, so that serialized property values
     * are added.
     *
     * @since 2.9
     */
    public JavaPropsGenerator createGenerator(Properties props) {
        return new PropertiesBackedGenerator(_createContext(props, true),
                props, _generatorFeatures, _objectCodec);
    }

    /*
    /**********************************************************
    /* Overridden parser factory methods
    /**********************************************************
     */

    @Override
    public JsonParser createParser(File f) throws IOException {
        return _createParser(new FileInputStream(f), _createContext(f, true));
    }

    @Override
    public JsonParser createParser(URL url) throws IOException {
        return _createParser(_optimizedStreamFromURL(url), _createContext(url, true));
    }

    @Override
    public JsonParser createParser(InputStream in) throws IOException {
        return _createParser(in, _createContext(in, false));
    }

    @Override
    public JsonParser createParser(byte[] data) throws IOException {
        return _createParser(data, 0, data.length, _createContext(data, true));
    }

    @Override
    public JsonParser createParser(byte[] data, int offset, int len) throws IOException {
        return _createParser(data, offset, len, _createContext(data, true));
    }

    /*
    /**********************************************************
    /* Overridden generator factory methods
    /**********************************************************
     */

    @Override
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        IOContext ctxt = _createContext(out, false);
        ctxt.setEncoding(enc);
        out = _decorate(out, ctxt);
        return _createJavaPropsGenerator(ctxt, _generatorFeatures, _objectCodec, out);
    }

    /**
     * Method for constructing {@link JsonGenerator} for generating
     * CBOR-encoded output.
     *<p>
     * Since CBOR format always uses UTF-8 internally, no encoding need
     * to be passed to this method.
     */
    @Override
    public JsonGenerator createGenerator(OutputStream out) throws IOException {
        IOContext ctxt = _createContext(out, false);
        out = _decorate(out, ctxt);
        return _createJavaPropsGenerator(ctxt, _generatorFeatures, _objectCodec, out);
    }

    /*
    /******************************************************
    /* Overridden internal factory methods, parser
    /******************************************************
     */

    /* // fine as-is: 
    @Override
    protected IOContext _createContext(Object srcRef, boolean resourceManaged) {
        return super._createContext(srcRef, resourceManaged);
    }
    */

    @Override
    protected JsonParser _createParser(InputStream in, IOContext ctxt) throws IOException
    {
        Properties props = _loadProperties(in, ctxt);
        return new JavaPropsParser(ctxt, in, _parserFeatures, _objectCodec, props);
    }

    @Override
    protected JsonParser _createParser(Reader r, IOContext ctxt) throws IOException {
        Properties props = _loadProperties(r, ctxt);
        return new JavaPropsParser(ctxt, r, _parserFeatures, _objectCodec, props);
    }

    @Override
    protected JsonParser _createParser(char[] data, int offset, int len, IOContext ctxt,
            boolean recyclable) throws IOException
    {
        return _createParser(new CharArrayReader(data, offset, len), ctxt);
    }

    @Override
    protected JsonParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException
    {
        return _createParser(new Latin1Reader(data, offset, len), ctxt);
    }

    /*
    /******************************************************
    /* Overridden internal factory methods, generator
    /******************************************************
     */
    
    @Override
    protected JsonGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException
    {
        return new WriterBackedGenerator(ctxt, out, _generatorFeatures, _objectCodec);
    }

    @Override
    protected JsonGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        return _createJavaPropsGenerator(ctxt, _generatorFeatures, _objectCodec, out);
    }

    @Override
    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException {
        // 27-Jan-2016, tatu: Properties javadoc is quite clear on Latin-1 (ISO-8859-1) being
        //    the default, so let's actually override
        return new OutputStreamWriter(out, CHARSET_ID_LATIN1);
    }

    /*
    /******************************************************
    /* Low-level methods for reading/writing Properties; currently
    /* we simply delegate to `java.util.Properties`
    /******************************************************
     */

    protected Properties _loadProperties(InputStream in, IOContext ctxt)
        throws IOException
    {
        // NOTE: Properties default to ISO-8859-1 (aka Latin-1), NOT UTF-8; this
        // as per JDK documentation
        return _loadProperties(new Latin1Reader(ctxt, in), ctxt);
    }

    protected Properties _loadProperties(Reader r0, IOContext ctxt)
        throws IOException
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

    private final JsonGenerator _createJavaPropsGenerator(IOContext ctxt,
            int stdFeat, ObjectCodec codec, OutputStream out) throws IOException
    {
        return new WriterBackedGenerator(ctxt, _createWriter(out, null, ctxt),
                stdFeat, _objectCodec);
                
    }

    /*
    public static void main(String[] args) throws Exception
    {
        args = new String[] { "test.properties" };
        Properties props = new Properties();
//        props.load(new FileInputStream(args[0]));
        props.load(new ByteArrayInputStream(new byte[0]));
        System.out.printf("%d entries:\n", props.size());
        int i = 1;
        for (Map.Entry<?,?> entry : props.entrySet()) {
            System.out.printf("#%d: %s -> %s\n", i++, entry.getKey(), entry.getValue());
        }
    }*/
}
