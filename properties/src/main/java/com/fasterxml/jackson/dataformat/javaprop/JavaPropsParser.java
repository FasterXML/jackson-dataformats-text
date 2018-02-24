package com.fasterxml.jackson.dataformat.javaprop;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Properties;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.dataformat.javaprop.io.JPropReadContext;
import com.fasterxml.jackson.dataformat.javaprop.util.JPropNode;
import com.fasterxml.jackson.dataformat.javaprop.util.JPropNodeBuilder;

public class JavaPropsParser extends ParserMinimalBase
{
    protected final static JavaPropsSchema DEFAULT_SCHEMA = new JavaPropsSchema();

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Codec used for data binding when (if) requested.
     */
    protected ObjectCodec _objectCodec;

    /**
     * Although most massaging is done later, caller may be interested in the
     * ultimate source.
     */
    protected final Object _inputSource;

    /**
     * Actual {@link java.util.Properties} that were parsed and handed to us
     * for further processing.
     */
    protected final Properties _sourceProperties;
    
    /**
     * Schema we use for parsing Properties into structure of some kind.
     */
    protected JavaPropsSchema _schema = DEFAULT_SCHEMA;

    /*
    /**********************************************************
    /* Parsing state
    /**********************************************************
     */

    protected JPropReadContext _readContext;

    protected boolean _closed;

    /*
    /**********************************************************
    /* Recycled helper objects
    /**********************************************************
     */

    protected ByteArrayBuilder _byteArrayBuilder;

    protected byte[] _binaryValue;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public JavaPropsParser(IOContext ctxt, Object inputSource,
            int parserFeatures, ObjectCodec codec, Properties sourceProps)
    {
        super(parserFeatures);
        _objectCodec = codec;
        _inputSource = inputSource;
        _sourceProperties = sourceProps;
        
    }

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    @Override
    public void setSchema(FormatSchema schema)
    {
        if (schema instanceof JavaPropsSchema) {
            _schema = (JavaPropsSchema) schema;
        } else {
            super.setSchema(schema);
        }
    }

    @Override
    public JavaPropsSchema getSchema() {
        return _schema;
    }

    // we do not take byte-based input, so base impl would be fine
    /*
    @Override
    public int releaseBuffered(OutputStream out) throws IOException {
        return -1;
    }
    */

    // current implementation delegates to JDK `Properties, so we don't ever
    // see the input so:
    /*
    @Override
    public int releaseBuffered(Writer w) throws IOException {
        return -1;
    }
    */

    @Override
    public void close() throws IOException {
        _closed = true;
        _readContext = null;
    }

    @Override
    public boolean isClosed() {
        return _closed;
    }
    
    /*
    /**********************************************************
    /* Public API overrides
    /**********************************************************
     */
    
    @Override
    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }

    @Override
    public Object getInputSource() {
        return _inputSource;
    }

    /*
    /**********************************************************
    /* Overrides: capability introspection methods
    /**********************************************************
     */
    
    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return schema instanceof JavaPropsSchema;
    }

    @Override
    public boolean requiresCustomCodec() { return false;}

    @Override
    public boolean canReadObjectId() { return false; }

    @Override
    public boolean canReadTypeId() { return false; }

    /*
    /**********************************************************
    /* Public API, structural
    /**********************************************************
     */

    @Override
    public JsonStreamContext getParsingContext() {
        return _readContext;
    }

    @Override
    public void overrideCurrentName(String name) {
        _readContext.overrideCurrentName(name);
    }

    /*
    /**********************************************************
    /* Main parsing API, textual values
    /**********************************************************
     */

    @Override
    public String getCurrentName() throws IOException {
        if (_readContext == null) {
            return null;
        }
        // [JACKSON-395]: start markers require information from parent
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            JPropReadContext parent = _readContext.getParent();
            if (parent != null) {
                return parent.getCurrentName();
            }
        }
        return _readContext.getCurrentName();
    }

    @Override
    public JsonToken nextToken() throws IOException {
        _binaryValue = null;
        if (_readContext == null) {
            if (_closed) {
                return null;
            }
            _closed = true;
            JPropNode root = JPropNodeBuilder.build(_schema, _sourceProperties);
            _readContext = JPropReadContext.create(root);

            // 30-Mar-2016, tatu: For debugging can be useful:
            /*
System.err.println("SOURCE: ("+root.getClass().getName()+") <<\n"+new ObjectMapper().writerWithDefaultPrettyPrinter()
.writeValueAsString(root.asRaw()));
System.err.println("\n>>");
*/
        }
        while ((_currToken = _readContext.nextToken()) == null) {
            _readContext = _readContext.nextContext();
            if (_readContext == null) { // end of content
                return null;
            }
        }
        return _currToken;
    }

    @Override
    public String getText() throws IOException {
        JsonToken t = _currToken;
        if (t == JsonToken.VALUE_STRING) {
            return _readContext.getCurrentText();
        }
        if (t == JsonToken.FIELD_NAME) {
            return _readContext.getCurrentName();
        }
        // shouldn't have non-String scalar values so:
        return (t == null) ? null : t.asString();
    }

    @Override
    public boolean hasTextCharacters() {
        return false;
    }
    
    @Override
    public char[] getTextCharacters() throws IOException {
        String text = getText();
        return (text == null) ? null : text.toCharArray();
    }

    @Override
    public int getTextLength() throws IOException {
        String text = getText();
        return (text == null) ? 0 : text.length();
    }

    @Override
    public int getTextOffset() throws IOException {
        return 0;
    }

    @Override // since 2.8
    public int getText(Writer writer) throws IOException
    {
        String str = getText();
        if (str == null) {
            return 0;
        }
        writer.write(str);
        return str.length();
    }
    
    @SuppressWarnings("resource")
    @Override
    public byte[] getBinaryValue(Base64Variant variant) throws IOException
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
    /**********************************************************
    /* Other accessor overrides
    /**********************************************************
     */

    @Override
    public Object getEmbeddedObject() throws IOException {
        return null;
    }
    
    @Override
    public JsonLocation getTokenLocation() {
        return JsonLocation.NA;
    }

    @Override
    public JsonLocation getCurrentLocation() {
        return JsonLocation.NA;
    }

    /*
    /**********************************************************
    /* Main parsing API, textual values
    /**********************************************************
     */
    
    @Override
    public Number getNumberValue() throws IOException {
        return _noNumbers();
    }
    
    @Override
    public NumberType getNumberType() throws IOException {
        return _noNumbers();
    }

    @Override
    public int getIntValue() throws IOException {
        return _noNumbers();
    }

    @Override
    public long getLongValue() throws IOException {
        return _noNumbers();
    }

    @Override
    public BigInteger getBigIntegerValue() throws IOException {
        return _noNumbers();
    }

    @Override
    public float getFloatValue() throws IOException {
        return _noNumbers();
    }

    @Override
    public double getDoubleValue() throws IOException {
        return _noNumbers();
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException {
        return _noNumbers();
    }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    protected <T> T _noNumbers() throws IOException {
        _reportError("Current token ("+_currToken+") not numeric, can not use numeric value accessors");
        return null;
    }

    @Override
    protected void _handleEOF() throws JsonParseException {
        if ((_readContext != null) && !_readContext.inRoot()) {
            _reportInvalidEOF(": expected close marker for "+_readContext.typeDesc(), null);
        }
    }
}
