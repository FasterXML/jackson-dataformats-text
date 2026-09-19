package com.fasterxml.jackson.dataformat.javaprop;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
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
    
    public JavaPropsFactory() {
        super();
    }

    public JavaPropsFactory(ObjectCodec codec) {
        super(codec);
    }

    protected JavaPropsFactory(JavaPropsFactory src, ObjectCodec oc)
    {
        super(src, oc);
    }

    /**
     * Constructors used by {@link JavaPropsFactoryBuilder} for instantiation.
     *
     * @since 2.9
     */
    protected JavaPropsFactory(JavaPropsFactoryBuilder b)
    {
        super(b, false);
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
     * @since 2.10
     */
    public JavaPropsParser createParser(Map<?,?> content) {
        return new JavaPropsParser(_createContext(_createContentReference(content), true),
                _parserFeatures, content, _objectCodec, content);
    }

    @Deprecated // since 2.10
    public JavaPropsParser createParser(Properties props) {
        return new JavaPropsParser(_createContext(_createContentReference(props), true),
                _parserFeatures, props, _objectCodec, props);
    }

    @Deprecated // since 2.10
    public JavaPropsGenerator createGenerator(Properties props) {
        IOContext ctxt = _createContext(_createContentReference(props), true);
        return new PropertiesBackedGenerator(ctxt,
                props, _generatorFeatures, _objectCodec);
    }

    /**
     * Convenience method to allow using a pre-constructed {@link Map}
     * instance as output target, so that serialized property values
     * are added.
     *
     * @since 2.10
     */
    public JavaPropsGenerator createGenerator(Map<?,?> target, JavaPropsSchema schema) {
        IOContext ctxt = _createContext(_createContentReference(target), true);
        return new PropertiesBackedGenerator(ctxt,
                target, _generatorFeatures, _objectCodec);
    }

    /*
    /**********************************************************
    /* Overridden parser factory methods
    /**********************************************************
     */

    @Override
    public JsonParser createParser(File f) throws IOException {
        IOContext ctxt = _createContext(_createContentReference(f), true);
        return _createParser(_decorate(new FileInputStream(f), ctxt), ctxt);
    }

    @Override
    public JsonParser createParser(URL url) throws IOException {
        IOContext ctxt = _createContext(_createContentReference(url), true);
        return _createParser(_decorate(_optimizedStreamFromURL(url), ctxt), ctxt);
    }

    @Override
    public JsonParser createParser(InputStream in) throws IOException {
        IOContext ctxt = _createContext(_createContentReference(in), false);
        return _createParser(_decorate(in, ctxt), ctxt);
    }

    @Override
    public JsonParser createParser(byte[] data) throws IOException {
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
    public JsonParser createParser(byte[] data, int offset, int len) throws IOException {
        IOContext ctxt = _createContext(_createContentReference(data, offset, len),
                true);
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
    /* Overridden generator factory methods
    /**********************************************************
     */

    @Override
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        IOContext ctxt = _createContext(_createContentReference(out), false);
        ctxt.setEncoding(enc);
        return _createJavaPropsGenerator(ctxt, _generatorFeatures, _objectCodec,
                _decorate(out, ctxt));
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
        IOContext ctxt = _createContext(_createContentReference(out), false);
        return _createJavaPropsGenerator(ctxt, _generatorFeatures, _objectCodec,
                _decorate(out, ctxt));
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

    /*
    public JavaPropsParser(IOContext ctxt, int parserFeatures, Object inputSource,
            ObjectCodec codec, Map<?,?> sourceMap)
     */
    
    @Override
    protected JsonParser _createParser(InputStream in, IOContext ctxt) throws IOException
    {
        Properties props = _loadProperties(in, ctxt);
        return new JavaPropsParser(ctxt, _parserFeatures, in, _objectCodec, props);
    }

    @Override
    protected JsonParser _createParser(Reader r, IOContext ctxt) throws IOException {
        Properties props = _loadProperties(r, ctxt);
        return new JavaPropsParser(ctxt, _parserFeatures, r, _objectCodec, props);
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
        // [dataformats-text#738]: `Properties.load()` reads input directly, so
        // to enforce max document length we need to count what it reads
        final StreamReadConstraints src = ctxt.streamReadConstraints();
        if (src.hasMaxDocumentLength()) {
            r0 = new ReadConstrainedReader(r0, src);
        }
        Properties props = new Properties();
        // May or may not want to close the reader, so...
        if (ctxt.isResourceManaged() || isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE)) {
            try (Reader r = r0) {
                props.load(r);
            } catch (IllegalArgumentException e) {
                _reportReadException("Invalid content, problem: "+e.getMessage(), e);
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

    // @since 2.14
    protected <T> T _reportReadException(String msg, Exception rootCause)
        throws IOException
    {
        throw new JsonParseException((JsonParser) null, msg, rootCause);
    }

    /*
    /******************************************************
    /* Helper classes
    /******************************************************
     */

    /**
     * {@link Reader} decorator that enforces
     * {@link StreamReadConstraints#getMaxDocumentLength()} by counting characters
     * read through it. Needed since actual decoding is done by
     * {@link java.util.Properties#load(Reader)}, reading content directly from
     * a {@link Reader}, so the parser itself does not see input as it is consumed.
     *<p>
     * Only installed when constraints define a maximum document length
     * (see {@link StreamReadConstraints#hasMaxDocumentLength()}).
     *
     * @since 2.18.11
     */
    private static class ReadConstrainedReader extends Reader
    {
        private final Reader _delegate;

        private final StreamReadConstraints _constraints;

        /**
         * Total number of characters read (or skipped) so far.
         */
        private long _charsRead;

        public ReadConstrainedReader(Reader delegate, StreamReadConstraints constraints) {
            _delegate = delegate;
            _constraints = constraints;
        }

        private void _count(long n) throws StreamConstraintsException {
            if (n > 0) {
                _charsRead += n;
                _constraints.validateDocumentLength(_charsRead);
            }
        }

        @Override
        public int read() throws IOException {
            int c = _delegate.read();
            if (c >= 0) {
                _count(1);
            }
            return c;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int n = _delegate.read(cbuf, off, len);
            _count(n);
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = _delegate.skip(n);
            _count(skipped);
            return skipped;
        }

        @Override
        public boolean ready() throws IOException {
            return _delegate.ready();
        }

        @Override
        public void close() throws IOException {
            _delegate.close();
        }
    }
}
