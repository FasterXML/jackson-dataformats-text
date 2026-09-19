package tools.jackson.dataformat.javaprop;

import java.io.*;
import java.util.*;

import tools.jackson.core.*;
import tools.jackson.core.base.TextualTSFactory;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.IOContext;
import tools.jackson.dataformat.javaprop.impl.PropertiesBackedGenerator;
import tools.jackson.dataformat.javaprop.impl.WriterBackedGenerator;
import tools.jackson.dataformat.javaprop.io.Latin1Reader;

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
    /**********************************************************************
    /* Factory construction, configuration
    /**********************************************************************
     */
    
    public JavaPropsFactory() {
        // No format-specific features yet so:
        super(StreamReadConstraints.defaults(), StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                0, 0);
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
        return new JavaPropsParser(readCtxt,
                _createContext(_createContentReference(content), true),
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
                _createContext(_createContentReference(target), true),
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
            InputStream in)
    {
        Properties props = _loadProperties(in, ioCtxt);
        return new JavaPropsParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                _getSchema(readCtxt),
                in, props);
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            Reader r) {
        Properties props = _loadProperties(r, ioCtxt);
        return new JavaPropsParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                _getSchema(readCtxt),
                r, props);
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            char[] data, int offset, int len,
            boolean recyclable)
    {
        return _createParser(readCtxt, ioCtxt, new CharArrayReader(data, offset, len));
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            byte[] data, int offset, int len)
    {
        return _createParser(readCtxt, ioCtxt, new Latin1Reader(data, offset, len));
    }

    @Override
    protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ctxt,
            DataInput input) {
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
            IOContext ioCtxt, Writer out)
    {
        return new WriterBackedGenerator(writeCtxt, ioCtxt,
                writeCtxt.getStreamWriteFeatures(_streamWriteFeatures),
                _getSchema(writeCtxt),
                out);
    }

    @Override
    protected JsonGenerator _createUTF8Generator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, OutputStream out)
    {
        return new WriterBackedGenerator(writeCtxt, ioCtxt,
                writeCtxt.getStreamWriteFeatures(_streamWriteFeatures),
                _getSchema(writeCtxt),
                _createWriter(ioCtxt, out, null));
    }

    @Override
    protected Writer _createWriter(IOContext ctioCtxtxt, OutputStream out, JsonEncoding enc)
    {
        // 27-Jan-2016, tatu: Properties javadoc is quite clear on Latin-1 (ISO-8859-1) being
        //    the default, so let's actually override
        try {
            return new OutputStreamWriter(out, CHARSET_ID_LATIN1);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
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
    {
        // NOTE: Properties default to ISO-8859-1 (aka Latin-1), NOT UTF-8; this
        // as per JDK documentation
        return _loadProperties(new Latin1Reader(ctxt, in), ctxt);
    }

    protected Properties _loadProperties(Reader r0, IOContext ctxt)
    {
        // [dataformats-text#738]: `Properties.load()` reads input directly, so
        // to enforce max document length we need to count what it reads
        r0 = _constrainedReader(ctxt, r0);
        Properties props = new Properties();
        // May or may not want to close the reader, so...
        try {
            if (ctxt.isResourceManaged() || isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE)) {
                try (Reader r = r0) {
                    props.load(r);
                }
            } else {
                props.load(r0);
            }
        } catch (IllegalArgumentException e) {
            _reportReadException("Invalid content, problem: "+e.getMessage(), e);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
        return props;
    }

    protected <T> T _reportReadException(String msg, Exception rootCause)
    {
        throw new StreamReadException((JsonParser) null, msg, rootCause);
    }

    /**
     * Helper method for [dataformats-text#738]: {@link java.util.Properties#load(Reader)}
     * reads input directly from the {@link Reader} given, so to enforce maximum
     * document length we need to count what it reads. No wrapping (and no
     * overhead) if no maximum document length configured.
     *
     * @since 3.1.7
     */
    protected static Reader _constrainedReader(IOContext ioCtxt, Reader reader) {
        StreamReadConstraints constraints = ioCtxt.streamReadConstraints();
        if (constraints.hasMaxDocumentLength()) {
            return new ReadConstrainedReader(reader, constraints);
        }
        return reader;
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
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
     *<p>
     * NOTE: unlike in 2.x, {@link StreamConstraintsException} is unchecked in 3.x,
     * so it propagates out of {@link java.util.Properties#load(Reader)} as-is
     * (which only catches {@link IOException}) and needs no unwrapping by the caller.
     *
     * @since 3.1.7
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
