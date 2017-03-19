package com.fasterxml.jackson.dataformat.javaprop;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.dataformat.javaprop.io.JPropEscapes;
import com.fasterxml.jackson.dataformat.javaprop.io.JPropWriteContext;
import com.fasterxml.jackson.dataformat.javaprop.util.Markers;

public abstract class JavaPropsGenerator extends GeneratorBase
{
    // As an optimization we try coalescing short writes into
    // buffer; but pass longer directly.
    final protected static int SHORT_WRITE = 100;

    /**
     * Since our context object does NOT implement standard write context, need
     * to do something like use a placeholder...
     */
    protected final static JsonWriteContext BOGUS_WRITE_CONTEXT = JsonWriteContext.createRootContext(null);

    private final static JavaPropsSchema EMPTY_SCHEMA;
    static {
        EMPTY_SCHEMA = JavaPropsSchema.emptySchema();
    }

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    final protected IOContext _ioContext;

    /**
     * Definition of columns being written, if available.
     */
    protected JavaPropsSchema _schema = EMPTY_SCHEMA;

    /*
    /**********************************************************
    /* Output state
    /**********************************************************
     */

    /**
     * Current context, in form we can use it (GeneratorBase has
     * untyped reference; left as null)
     */
    protected JPropWriteContext _jpropContext;

    /*
    /**********************************************************
    /* Output buffering
    /**********************************************************
     */

    protected final StringBuilder _basePath = new StringBuilder(50);

    protected boolean _headerChecked;

    protected int _indentLength;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public JavaPropsGenerator(IOContext ctxt, int stdFeatures, ObjectCodec codec)
    {
        super(stdFeatures, codec, BOGUS_WRITE_CONTEXT);
        _ioContext = ctxt;
        _jpropContext = JPropWriteContext.createRootContext();
    }

    @Override
    public Object getCurrentValue() {
        return _jpropContext.getCurrentValue();
    }

    @Override
    public void setCurrentValue(Object v) {
        _jpropContext.setCurrentValue(v);
    }

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*
    /**********************************************************
    /* Overridden methods, configuration
    /**********************************************************
     */

    // // No way to indent
    
    @Override
    public JsonGenerator useDefaultPrettyPrinter() {
        // could alternatively throw exception but let it fly for now
        return this;
    }

    @Override
    public JsonGenerator setPrettyPrinter(PrettyPrinter pp) {
        // could alternatively throw exception but let it fly for now
        return this;
    }

//    public abstract getOutputTarget()

    // Base impl fine
    /*
    @Override
    public int getOutputBuffered() {
        return -1;
    }
    */

    @Override
    public void setSchema(FormatSchema schema) {
        if (schema instanceof JavaPropsSchema) {
            _schema = (JavaPropsSchema) schema;
            // Indentation to use?
            if (_jpropContext.inRoot()) {
                String indent = _schema.lineIndentation();
                _indentLength = (indent == null) ? 0 : indent.length();
                if (_indentLength > 0) {
                    _basePath.setLength(0);
                    _basePath.append(indent);
                    _jpropContext = JPropWriteContext.createRootContext(_indentLength);
                }
            }
            return;
        }
        super.setSchema(schema);
    }

    @Override
    public FormatSchema getSchema() { return _schema; }
    
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
    public boolean canWriteObjectId() { return false; }

    @Override
    public boolean canWriteTypeId() { return false; }

    @Override
    public boolean canWriteBinaryNatively() { return false; }

    @Override
    public boolean canOmitFields() { return true; }

    @Override
    public boolean canWriteFormattedNumbers() { return true; }

    // No Format Features yet
/*
    
    @Override
    public int getFormatFeatures() {
        return _formatFeatures;
    }

    @Override
    public JsonGenerator overrideFormatFeatures(int values, int mask) { }
*/

    /*
    /**********************************************************
    /* Overridden methods: low-level I/O
    /**********************************************************
     */

//    public void close() throws IOException

//    public void flush() throws IOException

    @Override
    public JsonStreamContext getOutputContext() {
        return _jpropContext;
    }

    /*
    /**********************************************************************
    /* Overridden methods; writing field names
    /**********************************************************************
     */

    @Override
    public void writeFieldName(String name) throws IOException
    {
        if (!_jpropContext.writeFieldName(name)) {
            _reportError("Can not write a field name, expecting a value");
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
        // First: ensure possibly preceding field name is removed:
        _jpropContext.truncatePath(_basePath);
        if (_basePath.length() > _indentLength) {
            String sep = _schema.pathSeparator();
            if (!sep.isEmpty()) {
                _basePath.append(sep);
            }
        }
        // Note that escaping needs to be applied now...
        
        JPropEscapes.appendKey(_basePath, name);
        // NOTE: we do NOT yet write the key; wait until we have value; just append to path
    }

    /*
    /**********************************************************
    /* Public API: structural output
    /**********************************************************
     */

    @Override
    public void writeStartArray() throws IOException {
        _verifyValueWrite("start an array");
        _jpropContext = _jpropContext.createChildArrayContext(_basePath.length());
    }

    @Override
    public void writeEndArray() throws IOException {
        if (!_jpropContext.inArray()) {
            _reportError("Current context not an Array but "+_jpropContext.typeDesc());
        }
        _jpropContext = _jpropContext.getParent();
    }

    @Override
    public void writeStartObject() throws IOException {
        _verifyValueWrite("start an object");
        _jpropContext = _jpropContext.createChildObjectContext(_basePath.length());
    }

    @Override
    public void writeEndObject() throws IOException
    {
        if (!_jpropContext.inObject()) {
            _reportError("Current context not an Ibject but "+_jpropContext.typeDesc());
        }
        _jpropContext = _jpropContext.getParent();
    }

    /*
    /**********************************************************
    /* Output method implementations, textual
    /**********************************************************
     */

    @Override
    public void writeString(String text) throws IOException
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
        throws IOException
    {
        _verifyValueWrite("write String value");
        _writeEscapedEntry(text, offset, len);
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int len)throws IOException
    {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int len) throws IOException
    {
        writeString(new String(text, offset, len, "UTF-8"));
    }

    /*
    /**********************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************
     */

    @Override
    public void writeRaw(String text) throws IOException {
        _writeRaw(text);
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        _writeRaw(text.substring(offset, offset+len));
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        _writeRaw(text, offset, len);
    }

    @Override
    public void writeRaw(char c) throws IOException {
        _writeRaw(c);
    }

    @Override
    public void writeRaw(SerializableString text) throws IOException, JsonGenerationException {
        writeRaw(text.toString());
    }

    /*
    /**********************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************
     */
    
    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len)
            throws IOException
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
    /**********************************************************
    /* Output method implementations, scalars
    /**********************************************************
     */

    @Override
    public void writeBoolean(boolean state) throws IOException
    {
        _verifyValueWrite("write boolean value");
        _writeUnescapedEntry(state ? "true" : "false");
    }

    @Override
    public void writeNumber(int i) throws IOException
    {
        _verifyValueWrite("write number");
        _writeUnescapedEntry(String.valueOf(i));
    }

    @Override
    public void writeNumber(long l) throws IOException
    {
        _verifyValueWrite("write number");
        _writeUnescapedEntry(String.valueOf(l));
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException
    {
        if (v == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writeUnescapedEntry(String.valueOf(v));
    }
    
    @Override
    public void writeNumber(double d) throws IOException
    {
        _verifyValueWrite("write number");
        _writeUnescapedEntry(String.valueOf(d));
    }    

    @Override
    public void writeNumber(float f) throws IOException
    {
        _verifyValueWrite("write number");
        _writeUnescapedEntry(String.valueOf(f));
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException
    {
        if (dec == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        String str = isEnabled(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN) ? dec.toPlainString() : dec.toString();
        _writeUnescapedEntry(str);
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException
    {
        if (encodedValue == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writeUnescapedEntry(encodedValue);
    }

    @Override
    public void writeNull() throws IOException
    {
        _verifyValueWrite("write null value");
        _writeUnescapedEntry("");
    }

    /*
    /**********************************************************
    /* Implementations for methods from base class
    /**********************************************************
     */

//    protected void _releaseBuffers()

//    protected void _flushBuffer() throws IOException

    @Override
    protected void _verifyValueWrite(String typeMsg) throws IOException
    {
        // first, check that name/value cadence works
        if (!_jpropContext.writeValue()) {
            _reportError("Can not "+typeMsg+", expecting field name");
        }
        // and if so, update path if we are in array
        if (_jpropContext.inArray()) {
            // remove possible path remnants from an earlier sibling
            _jpropContext.truncatePath(_basePath);
            int ix = _jpropContext.getCurrentIndex() + _schema.firstArrayOffset();
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
    /**********************************************************
    /* Abstract methods for sub-classes
    /**********************************************************
     */
    
    protected abstract void _writeEscapedEntry(String value) throws IOException;

    protected abstract void _writeEscapedEntry(char[] text, int offset, int len) throws IOException;

    protected abstract void _writeUnescapedEntry(String value) throws IOException;

    protected abstract void _writeRaw(char c) throws IOException;
    protected abstract void _writeRaw(String text) throws IOException;
    protected abstract void _writeRaw(StringBuilder text) throws IOException;
    protected abstract void _writeRaw(char[] text, int offset, int len) throws IOException;
    
}
