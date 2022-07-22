package tools.jackson.dataformat.javaprop;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import tools.jackson.core.*;
import tools.jackson.core.base.ParserMinimalBase;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.ByteArrayBuilder;
import tools.jackson.core.util.JacksonFeatureSet;

import tools.jackson.dataformat.javaprop.io.JPropReadContext;
import tools.jackson.dataformat.javaprop.util.JPropNode;
import tools.jackson.dataformat.javaprop.util.JPropNodeBuilder;

public class JavaPropsParser extends ParserMinimalBase
{
    protected final static JavaPropsSchema DEFAULT_SCHEMA = new JavaPropsSchema();

    /**
     * Properties capabilities slightly different from defaults, having
     * untyped (text-only) scalars
     *
     * @since 2.12
     */
    protected final static JacksonFeatureSet<StreamReadCapability> STREAM_READ_CAPABILITIES =
            DEFAULT_READ_CAPABILITIES
                .with(StreamReadCapability.UNTYPED_SCALARS)
            ;

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Although most massaging is done later, caller may be interested in the
     * ultimate source.
     */
    protected final Object _inputSource;

    /**
     * Actual {@link java.util.Properties} (or, actually, any {@link java.util.Map}
     * with String keys, values) that were parsed and handed to us
     * for further processing.
     */
    protected final Map<?,?> _sourceContent;
    
    /**
     * Schema we use for parsing Properties into structure of some kind.
     */
    protected JavaPropsSchema _schema = DEFAULT_SCHEMA;

    /*
    /**********************************************************************
    /* Parsing state
    /**********************************************************************
     */

    protected JPropReadContext _streamReadContext;

    protected boolean _closed;

    /*
    /**********************************************************************
    /* Recycled helper objects
    /**********************************************************************
     */

    protected ByteArrayBuilder _byteArrayBuilder;

    protected byte[] _binaryValue;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public JavaPropsParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            int parserFeatures, JavaPropsSchema schema,
            Object inputSource, Map<?,?> sourceMap)
    {
        super(readCtxt, parserFeatures);
        _inputSource = inputSource;
        _sourceContent = sourceMap;
        _schema = schema;
    }
    
    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*// No need in 3.0
    @Override
    public void setSchema(FormatSchema schema)
    {
        if (schema instanceof JavaPropsSchema) {
            _schema = (JavaPropsSchema) schema;
        } else {
            super.setSchema(schema);
        }
    }
    */

    @Override
    public JavaPropsSchema getSchema() {
        return _schema;
    }

    // we do not take byte-based input, so base impl would be fine
    /*
    @Override
    public int releaseBuffered(OutputStream out) {
        return -1;
    }
    */

    // current implementation delegates to JDK `Properties, so we don't ever
    // see the input so:
    /*
    @Override
    public int releaseBuffered(Writer w) {
        return -1;
    }
    */

    @Override
    public void close() {
        _closed = true;
        _streamReadContext = null;
    }

    @Override
    public boolean isClosed() {
        return _closed;
    }

    /*
    /**********************************************************************
    /* Public API overrides
    /**********************************************************************
     */

    @Override
    public Object streamReadInputSource() {
        return _inputSource;
    }

    /*
    /**********************************************************************
    /* Overrides: capability introspection methods
    /**********************************************************************
     */

    @Override
    public boolean canReadObjectId() { return false; }

    @Override
    public boolean canReadTypeId() { return false; }

    @Override
    public JacksonFeatureSet<StreamReadCapability> streamReadCapabilities() {
        return STREAM_READ_CAPABILITIES;
    }

    /*
    /**********************************************************************
    /* Public API, structural
    /**********************************************************************
     */

    @Override
    public TokenStreamContext streamReadContext() { return _streamReadContext; }
    @Override public void assignCurrentValue(Object v) { _streamReadContext.assignCurrentValue(v); }
    @Override public Object currentValue() { return _streamReadContext.currentValue(); }

    /*
    /**********************************************************************
    /* Main parsing API, textual values
    /**********************************************************************
     */

    @Override
    public String currentName() {
        if (_streamReadContext == null) {
            return null;
        }
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            JPropReadContext parent = _streamReadContext.getParent();
            if (parent != null) {
                return parent.currentName();
            }
        }
        return _streamReadContext.currentName();
    }

    @Override
    public JsonToken nextToken() throws JacksonException {
        _binaryValue = null;
        if (_streamReadContext == null) {
            if (_closed) {
                return null;
            }
            _closed = true;
            JPropNode root = JPropNodeBuilder.build(_sourceContent, _schema);
            _streamReadContext = JPropReadContext.create(root);

            // 30-Mar-2016, tatu: For debugging can be useful:
            /*
System.err.println("SOURCE: ("+root.getClass().getName()+") <<\n"+new ObjectMapper().writerWithDefaultPrettyPrinter()
.writeValueAsString(root.asRaw()));
System.err.println("\n>>");
*/
        }
        while ((_currToken = _streamReadContext.nextToken()) == null) {
            _streamReadContext = _streamReadContext.nextContext();
            if (_streamReadContext == null) { // end of content
                return null;
            }
        }
        return _currToken;
    }

    @Override
    public String getText() throws JacksonException {
        JsonToken t = _currToken;
        if (t == JsonToken.VALUE_STRING) {
            return _streamReadContext.getCurrentText();
        }
        if (t == JsonToken.PROPERTY_NAME) {
            return _streamReadContext.currentName();
        }
        // shouldn't have non-String scalar values so:
        return (t == null) ? null : t.asString();
    }

    @Override
    public boolean hasTextCharacters() {
        return false;
    }
    
    @Override
    public char[] getTextCharacters() throws JacksonException {
        String text = getText();
        return (text == null) ? null : text.toCharArray();
    }

    @Override
    public int getTextLength() throws JacksonException {
        String text = getText();
        return (text == null) ? 0 : text.length();
    }

    @Override
    public int getTextOffset() throws JacksonException {
        return 0;
    }

    @Override
    public int getText(Writer writer) throws JacksonException
    {
        String str = getText();
        if (str == null) {
            return 0;
        }
        try {
            writer.write(str);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
        return str.length();
    }

    @SuppressWarnings("resource")
    @Override
    public byte[] getBinaryValue(Base64Variant variant) throws JacksonException
    {
        if (_binaryValue == null) {
            if (_currToken != JsonToken.VALUE_STRING) {
                _reportError("Current token ("+_currToken+") not VALUE_STRING, can not access as binary");
            }
            ByteArrayBuilder builder = _getByteArrayBuilder();
            _decodeBase64(getText(), builder, variant);
            _binaryValue = builder.toByteArray();
        }
        return _binaryValue;
    }

    public ByteArrayBuilder _getByteArrayBuilder()
    {
        if (_byteArrayBuilder == null) {
            _byteArrayBuilder = new ByteArrayBuilder();
        } else {
            _byteArrayBuilder.reset();
        }
        return _byteArrayBuilder;
    }

    /*
    /**********************************************************************
    /* Other accessor overrides
    /**********************************************************************
     */

    @Override
    public Object getEmbeddedObject() throws JacksonException {
        return null;
    }
    
    @Override
    public JsonLocation currentTokenLocation() {
        return JsonLocation.NA;
    }

    @Override
    public JsonLocation currentLocation() {
        return JsonLocation.NA;
    }

    /*
    /**********************************************************************
    /* Main parsing API, textual values
    /**********************************************************************
     */
    
    @Override
    public Number getNumberValue() throws JacksonException {
        return _noNumbers();
    }
    
    @Override
    public NumberType getNumberType() throws JacksonException {
        return _noNumbers();
    }

    @Override
    public int getIntValue() throws JacksonException {
        return _noNumbers();
    }

    @Override
    public long getLongValue() throws JacksonException {
        return _noNumbers();
    }

    @Override
    public BigInteger getBigIntegerValue() throws JacksonException {
        return _noNumbers();
    }

    @Override
    public float getFloatValue() throws JacksonException {
        return _noNumbers();
    }

    @Override
    public double getDoubleValue() throws JacksonException {
        return _noNumbers();
    }

    @Override
    public BigDecimal getDecimalValue() throws JacksonException {
        return _noNumbers();
    }

    @Override
    public boolean isNaN() {
        return false;
    }

    /*
    /**********************************************************************
    /* Internal helper methods
    /**********************************************************************
     */

    protected <T> T _noNumbers() throws StreamReadException {
        _reportError("Current token ("+_currToken+") not numeric, can not use numeric value accessors");
        return null;
    }

    @Override
    protected void _handleEOF() throws StreamReadException {
        if ((_streamReadContext != null) && !_streamReadContext.inRoot()) {
            _reportInvalidEOF(": expected close marker for "+_streamReadContext.typeDesc(), null);
        }
    }
}
