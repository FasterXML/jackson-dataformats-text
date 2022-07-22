package tools.jackson.dataformat.javaprop;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import tools.jackson.core.*;
import tools.jackson.core.base.GeneratorBase;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.JacksonFeatureSet;
import tools.jackson.dataformat.javaprop.PackageVersion;
import tools.jackson.dataformat.javaprop.io.JPropWriteContext;
import tools.jackson.dataformat.javaprop.util.Markers;

public abstract class JavaPropsGenerator
    extends GeneratorBase
{
    // As an optimization we try coalescing short writes into
    // buffer; but pass longer directly.
    protected final static int SHORT_WRITE = 100;

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected final IOContext _ioContext;

    /**
     * Definition of mapping of logically structured property names into actual
     * flattened property names.
     */
    protected final JavaPropsSchema _schema;

    /*
    /**********************************************************************
    /* Output state
    /**********************************************************************
     */

    /**
     * Current context, in form we can use it (GeneratorBase has
     * untyped reference; left as null)
     */
    protected JPropWriteContext _streamWriteContext;

    /*
    /**********************************************************************
    /* Output buffering
    /**********************************************************************
     */

    protected final StringBuilder _basePath = new StringBuilder(50);

    protected boolean _headerChecked;

    protected int _indentLength;
    
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public JavaPropsGenerator(ObjectWriteContext writeCtxt, IOContext ioCtxt,
            int stdFeatures, JavaPropsSchema schema)
    {
        super(writeCtxt, stdFeatures);
        _ioContext = ioCtxt;
        _streamWriteContext = JPropWriteContext.createRootContext();

        _schema = schema;
        // Indentation to use?
        if (_streamWriteContext.inRoot()) {
            String indent = _schema.lineIndentation();
            _indentLength = (indent == null) ? 0 : indent.length();
            if (_indentLength > 0) {
                _basePath.setLength(0);
                _basePath.append(indent);
                _streamWriteContext = JPropWriteContext.createRootContext(_indentLength);
            }
            // [dataformats-text#100]: Allow use of optional prefix
            final String prefix = _schema.prefix();
            if (prefix != null) {
                _basePath.append(prefix);
            }
        }
    }

    /*
    /**********************************************************************
    /* Versioned
    /**********************************************************************
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*
    /**********************************************************************
    /* Overridden output state handling methods
    /**********************************************************************
     */

    @Override
    public TokenStreamContext streamWriteContext() {
        return _streamWriteContext;
    }

    @Override
    public Object currentValue() {
        return _streamWriteContext.currentValue();
    }

    @Override
    public void assignCurrentValue(Object v) {
        _streamWriteContext.assignCurrentValue(v);
    }

    /*
    /**********************************************************************
    /* Overridden methods, configuration
    /**********************************************************************
     */

//    public abstract getOutputTarget()

    // Base impl fine
    /*
    @Override
    public int getOutputBuffered() {
        return -1;
    }
    */

    @Override
    public FormatSchema getSchema() { return _schema; }

    /*
    /**********************************************************************
    /* Overrides: capability introspection methods
    /**********************************************************************
     */

    @Override
    public boolean canWriteObjectId() { return false; }

    @Override
    public boolean canWriteTypeId() { return false; }

    @Override
    public boolean canOmitProperties() { return true; }

    @Override
    public JacksonFeatureSet<StreamWriteCapability> streamWriteCapabilities() {
        return DEFAULT_TEXTUAL_WRITE_CAPABILITIES;
    }

    /*
    /**********************************************************************
    /* Overridden methods; writing property names
    /**********************************************************************
     */

    @Override
    public void writeName(String name) throws JacksonException
    {
        if (!_streamWriteContext.writeName(name)) {
            _reportError("Cannot write a property name, expecting a value");
        }
        // also, may need to output header if this would be first write
        if (!_headerChecked) {
            _headerChecked = true;
            String header = _schema.header();
            if (header != null && !header.isEmpty()) {
                _writeRaw(header);
            }
        }

        // Ok; append to base path at this point.
        // First: ensure possibly preceding property name is removed:
        _streamWriteContext.truncatePath(_basePath);
        if (_basePath.length() > _indentLength) {
            String sep = _schema.pathSeparator();
            if (!sep.isEmpty()) {
                _basePath.append(sep);
            }
        }
        _appendPropertyName(_basePath, name);
    }

    @Override
    public void writePropertyId(long id) throws JacksonException {
        // 15-Aug-2019, tatu: should be improved to avoid String generation
        writeName(Long.toString(id));
    }

    protected abstract void _appendPropertyName(StringBuilder path, String name);

    /*
    /**********************************************************************
    /* Public API: structural output
    /**********************************************************************
     */

    @Override
    public void writeStartArray() throws JacksonException {
        _verifyValueWrite("start an array");
        _streamWriteContext = _streamWriteContext.createChildArrayContext(null,
                _basePath.length());
    }

    @Override
    public void writeStartArray(Object currValue) throws JacksonException {
        _verifyValueWrite("start an array");
        _streamWriteContext = _streamWriteContext.createChildArrayContext(currValue,
                _basePath.length());
    }
    
    @Override
    public void writeEndArray() throws JacksonException {
        if (!_streamWriteContext.inArray()) {
            _reportError("Current context not an Array but "+_streamWriteContext.typeDesc());
        }
        _streamWriteContext = _streamWriteContext.getParent();
    }

    @Override
    public void writeStartObject() throws JacksonException {
        _verifyValueWrite("start an object");
        _streamWriteContext = _streamWriteContext.createChildObjectContext(null, _basePath.length());
    }

    @Override
    public void writeStartObject(Object forValue) throws JacksonException {
        _verifyValueWrite("start an object");
        _streamWriteContext = _streamWriteContext.createChildObjectContext(forValue, _basePath.length());
    }

    @Override
    public void writeEndObject() throws JacksonException
    {
        if (!_streamWriteContext.inObject()) {
            _reportError("Current context not an Ibject but "+_streamWriteContext.typeDesc());
        }
        _streamWriteContext = _streamWriteContext.getParent();
    }

    /*
    /**********************************************************************
    /* Output method implementations, textual
    /**********************************************************************
     */

    @Override
    public void writeString(String text) throws JacksonException
    {
        if (text == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write String value");
        _writeEscapedEntry(text);
    }

    @Override
    public void writeString(char[] text, int offset, int len)
        throws JacksonException
    {
        _verifyValueWrite("write String value");
        _writeEscapedEntry(text, offset, len);
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int len)throws JacksonException
    {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int len) throws JacksonException
    {
        writeString(new String(text, offset, len, StandardCharsets.UTF_8));
    }

    /*
    /**********************************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************************
     */

    @Override
    public void writeRaw(String text) throws JacksonException {
        _writeRaw(text);
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws JacksonException {
        _writeRaw(text.substring(offset, offset+len));
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws JacksonException {
        _writeRaw(text, offset, len);
    }

    @Override
    public void writeRaw(char c) throws JacksonException {
        _writeRaw(c);
    }

    @Override
    public void writeRaw(SerializableString text) throws JacksonException {
        writeRaw(text.toString());
    }

    /*
    /**********************************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************************
     */
    
    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len)
        throws JacksonException
    {
        if (data == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write Binary value");
        // ok, better just Base64 encode as a String...
        if (offset > 0 || (offset+len) != data.length) {
            data = Arrays.copyOfRange(data, offset, offset+len);
        }
        String encoded = b64variant.encode(data);
        _writeEscapedEntry(encoded);
    }

    /*
    /**********************************************************************
    /* Output method implementations, scalars
    /**********************************************************************
     */

    @Override
    public void writeBoolean(boolean state) throws JacksonException
    {
        _verifyValueWrite("write boolean value");
        _writeUnescapedEntry(state ? "true" : "false");
    }

    @Override
    public void writeNumber(short v) throws JacksonException {
        writeNumber((int) v);
    }

    @Override
    public void writeNumber(int i) throws JacksonException
    {
        _verifyValueWrite("write number");
        _writeUnescapedEntry(String.valueOf(i));
    }

    @Override
    public void writeNumber(long l) throws JacksonException
    {
        _verifyValueWrite("write number");
        _writeUnescapedEntry(String.valueOf(l));
    }

    @Override
    public void writeNumber(BigInteger v) throws JacksonException
    {
        if (v == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writeUnescapedEntry(String.valueOf(v));
    }
    
    @Override
    public void writeNumber(double d) throws JacksonException
    {
        _verifyValueWrite("write number");
        _writeUnescapedEntry(String.valueOf(d));
    }    

    @Override
    public void writeNumber(float f) throws JacksonException
    {
        _verifyValueWrite("write number");
        _writeUnescapedEntry(String.valueOf(f));
    }

    @Override
    public void writeNumber(BigDecimal dec) throws JacksonException
    {
        if (dec == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        String str = isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN) ? dec.toPlainString() : dec.toString();
        _writeUnescapedEntry(str);
    }

    @Override
    public void writeNumber(String encodedValue) throws JacksonException
    {
        if (encodedValue == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writeUnescapedEntry(encodedValue);
    }

    @Override
    public void writeNull() throws JacksonException
    {
        _verifyValueWrite("write null value");
        _writeUnescapedEntry("");
    }

    /*
    /**********************************************************************
    /* Implementations for methods from base class
    /**********************************************************************
     */

//    protected void _releaseBuffers()

//    protected void _flushBuffer() throws JacksonException

    @Override
    protected void _verifyValueWrite(String typeMsg) throws JacksonException
    {
        // first, check that name/value cadence works
        if (!_streamWriteContext.writeValue()) {
            _reportError("Cannot "+typeMsg+", expecting a property name");
        }
        // and if so, update path if we are in array
        if (_streamWriteContext.inArray()) {
            // remove possible path remnants from an earlier sibling
            _streamWriteContext.truncatePath(_basePath);
            int ix = _streamWriteContext.getCurrentIndex() + _schema.firstArrayOffset();
            if (_schema.writeIndexUsingMarkers()) {
                Markers m = _schema.indexMarker();
                // no leading path separator, if using enclosed indexes
                _basePath.append(m.getStart());
                _basePath.append(ix);
                _basePath.append(m.getEnd());
            } else {
                // leading path separator, if using "simple" index markers
                if (_basePath.length() > 0) {
                    String sep = _schema.pathSeparator();
                    if (!sep.isEmpty()) {
                        _basePath.append(sep);
                    }
                }
                _basePath.append(ix);
            }
        }
    }

    /*
    /**********************************************************************
    /* Abstract methods for sub-classes
    /**********************************************************************
     */
    
    protected abstract void _writeEscapedEntry(String value) throws JacksonException;

    protected abstract void _writeEscapedEntry(char[] text, int offset, int len) throws JacksonException;

    protected abstract void _writeUnescapedEntry(String value) throws JacksonException;

    protected abstract void _writeRaw(char c) throws JacksonException;
    protected abstract void _writeRaw(String text) throws JacksonException;
    protected abstract void _writeRaw(StringBuilder text) throws JacksonException;
    protected abstract void _writeRaw(char[] text, int offset, int len) throws JacksonException;
    
}
