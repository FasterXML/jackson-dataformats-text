package com.fasterxml.jackson.dataformat.toml;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.VersionUtil;

final class TomlGenerator extends GeneratorBase
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
     * Underlying {@link Writer} used for output.
     */
    protected final Writer _out;


    /*
    /**********************************************************************
    /* Output state
    /**********************************************************************
     */

    /**
     * Current context, in form we can use it (GeneratorBase has
     * untyped reference; left as null)
     */
    protected TomlWriteContext _streamWriteContext;

    /*
    /**********************************************************************
    /* Output buffering
    /**********************************************************************
     */

    /**
     * Intermediate buffer in which contents are buffered before
     * being written using {@code _out}.
     */
    protected char[] _outputBuffer;

    /**
     * Pointer to the next available location in {@code _outputBuffer}
     */
    protected int _outputTail = 0;

    /**
     * Offset to index after the last valid index in {@code _outputBuffer}.
     * Typically same as length of the buffer.
     */
    protected final int _outputEnd;

    protected final StringBuilder _basePath = new StringBuilder(50);

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public TomlGenerator(IOContext ioCtxt, int stdFeatures, ObjectCodec codec, Writer out) {
        super(stdFeatures, codec);
        _ioContext = ioCtxt;
        _streamWriteContext = TomlWriteContext.createRootContext();
        _out = out;
        _outputBuffer = ioCtxt.allocConcatBuffer();
        _outputEnd = _outputBuffer.length;
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
    /* Overridden methods: low-level I/O
    /**********************************************************************
     */

    @Override
    public void close() throws IOException {
        super.close();
        _flushBuffer();
        _outputTail = 0; // just to ensure we don't think there's anything buffered

        if (_out != null) {
            if (_ioContext.isResourceManaged() || isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET)) {
                _out.close();
            } else if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
                // If we can't close it, we should at least flush
                _out.flush();
            }
        }
        // Internal buffer(s) generator has can now be released as well
        _releaseBuffers();
    }

    @Override
    public void flush() throws IOException {
        _flushBuffer();
        if (_out != null) {
            if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
                _out.flush();
            }
        }
    }

    /*
    /**********************************************************************
    /* Implementations for methods from base class
    /**********************************************************************
     */

    @Override
    protected void _releaseBuffers() {
        char[] buf = _outputBuffer;
        if (buf != null) {
            _outputBuffer = null;
            _ioContext.releaseConcatBuffer(buf);
        }
    }

    protected void _flushBuffer() throws IOException {
        if (_outputTail > 0) {
            _out.write(_outputBuffer, 0, _outputTail);
            _outputTail = 0;
        }
    }

    /*
    /**********************************************************************
    /* Internal methods; raw writes
    /**********************************************************************
     */

    protected void _writeRaw(char c) throws IOException {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = c;
    }

    protected void _writeRaw(String text) throws IOException {
        // Nothing to check, can just output as is
        int len = text.length();
        int room = _outputEnd - _outputTail;

        if (room == 0) {
            _flushBuffer();
            room = _outputEnd - _outputTail;
        }
        // But would it nicely fit in? If yes, it's easy
        if (room >= len) {
            text.getChars(0, len, _outputBuffer, _outputTail);
            _outputTail += len;
        } else {
            _writeRawLong(text);
        }
    }

    protected void _writeRaw(StringBuilder text) throws IOException {
        // Nothing to check, can just output as is
        int len = text.length();
        int room = _outputEnd - _outputTail;

        if (room == 0) {
            _flushBuffer();
            room = _outputEnd - _outputTail;
        }
        // But would it nicely fit in? If yes, it's easy
        if (room >= len) {
            text.getChars(0, len, _outputBuffer, _outputTail);
            _outputTail += len;
        } else {
            _writeRawLong(text);
        }
    }

    protected void _writeRaw(char[] text, int offset, int len) throws IOException {
        // Only worth buffering if it's a short write?
        if (len < SHORT_WRITE) {
            int room = _outputEnd - _outputTail;
            if (len > room) {
                _flushBuffer();
            }
            System.arraycopy(text, offset, _outputBuffer, _outputTail, len);
            _outputTail += len;
            return;
        }
        // Otherwise, better just pass through:
        _flushBuffer();
        _out.write(text, offset, len);
    }

    protected void _writeRawLong(String text) throws IOException {
        int room = _outputEnd - _outputTail;
        text.getChars(0, room, _outputBuffer, _outputTail);
        _outputTail += room;
        _flushBuffer();
        int offset = room;
        int len = text.length() - room;

        while (len > _outputEnd) {
            int amount = _outputEnd;
            text.getChars(offset, offset + amount, _outputBuffer, 0);
            _outputTail = amount;
            _flushBuffer();
            offset += amount;
            len -= amount;
        }
        // And last piece (at most length of buffer)
        text.getChars(offset, offset + len, _outputBuffer, 0);
        _outputTail = len;
    }

    protected void _writeRawLong(StringBuilder text) throws IOException {
        int room = _outputEnd - _outputTail;
        text.getChars(0, room, _outputBuffer, _outputTail);
        _outputTail += room;
        _flushBuffer();
        int offset = room;
        int len = text.length() - room;

        while (len > _outputEnd) {
            int amount = _outputEnd;
            text.getChars(offset, offset + amount, _outputBuffer, 0);
            _outputTail = amount;
            _flushBuffer();
            offset += amount;
            len -= amount;
        }
        // And last piece (at most length of buffer)
        text.getChars(offset, offset + len, _outputBuffer, 0);
        _outputTail = len;
    }

    /*
    /**********************************************************************
    /* Overridden output state handling methods
    /**********************************************************************
     */

    @Override
    public Object currentValue() {
        return _streamWriteContext.getCurrentValue();
    }

    @Override
    public void assignCurrentValue(Object v) {
        _streamWriteContext.setCurrentValue(v);
    }

    /*
    /**********************************************************************
    /* Overrides: capability introspection methods
    /**********************************************************************
     */

    @Override
    public boolean canWriteObjectId() {
        return false;
    }

    @Override
    public boolean canWriteTypeId() {
        return false;
    }

    /*
    /**********************************************************************
    /* Overridden methods; writing property names
    /**********************************************************************
     */

    @Override
    public void writeFieldName(String name) throws IOException {
        if (!_streamWriteContext.writeName(name)) {
            _reportError("Cannot write a property name, expecting a value");
        }

        if (_streamWriteContext._inline) {
            if (_streamWriteContext.hasCurrentIndex()) {
                _writeRaw(", ");
            }
            _writeStringImpl(StringOutputUtil.MASK_SIMPLE_KEY, name);
        } else {
            // Ok; append to base path at this point.
            // First: ensure possibly preceding property name is removed:
            _streamWriteContext.truncatePath(_basePath);
            if (_basePath.length() > 0) {
                _basePath.append('.');
            }
            _appendPropertyName(_basePath, name);
        }
    }

    /*
    /**********************************************************************
    /* Public API: structural output
    /**********************************************************************
     */

    @Override
    public void writeStartArray() throws IOException {
        writeStartArray(null);
    }

    @Override
    public void writeStartArray(Object currValue) throws IOException {
        // arrays are always inline, force writing the current key
        // NOTE: if this ever changes, we need to add empty array handling in writeEndArray
        _verifyValueWrite("start an array", true);
        _streamWriteContext = _streamWriteContext.createChildArrayContext(currValue,
                _basePath.length());
        if (_streamWriteContext._inline) {
            _writeRaw('[');
        }
    }

    @Override
    public void writeEndArray() throws IOException {
        if (!_streamWriteContext.inArray()) {
            _reportError("Current context not an Array but " + _streamWriteContext.typeDesc());
        }
        if (_streamWriteContext._inline) {
            _writeRaw(']');
        } else if (!_streamWriteContext.hasCurrentIndex()) {
            // empty array
            VersionUtil.throwInternal();
        }
        _streamWriteContext = _streamWriteContext.getParent();
        writeValueEnd();
    }

    @Override
    public void writeStartObject() throws IOException {
        writeStartObject(null);
    }

    @Override
    public void writeStartObject(Object forValue) throws IOException {
        // objects aren't always materialized right now
        _verifyValueWrite("start an object", false);
        _streamWriteContext = _streamWriteContext.createChildObjectContext(forValue, _basePath.length());
        if (_streamWriteContext._inline) {
            writeRaw('{');
        }
    }

    @Override
    public void writeEndObject() throws IOException {
        if (!_streamWriteContext.inObject()) {
            _reportError("Current context not an Ibject but " + _streamWriteContext.typeDesc());
        }
        if (_streamWriteContext._inline) {
            writeRaw('}');
            _streamWriteContext = _streamWriteContext.getParent();
            writeValueEnd();
        } else {
            if (!_streamWriteContext.hasCurrentIndex()) {
                // empty object
                writeCurrentPath();
                _writeRaw("{}");
                writeValueEnd();
            }
            _streamWriteContext = _streamWriteContext.getParent();
        }
    }

    /*
    /**********************************************************************
    /* Output method implementations, textual
    /**********************************************************************
     */

    @Override
    public void writeString(String text) throws IOException {
        if (text == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write String value");
        _writeStringImpl(StringOutputUtil.MASK_STRING, text);
        writeValueEnd();
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException {
        _verifyValueWrite("write String value");
        _writeStringImpl(StringOutputUtil.MASK_STRING, text, offset, len);
        writeValueEnd();
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int len) throws IOException {
        writeString(new String(text, offset, len, StandardCharsets.UTF_8));
        writeValueEnd();
    }

    /*
    /**********************************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************************
     */

    @Override
    public void writeRaw(String text) throws IOException {
        _writeRaw(text);
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        _writeRaw(text.substring(offset, offset + len));
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
    public void writeRaw(SerializableString text) throws IOException {
        writeRaw(text.toString());
    }

    /*
    /**********************************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************************
     */

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len)
            throws IOException {
        if (data == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write Binary value");
        // ok, better just Base64 encode as a String...
        if (offset > 0 || (offset + len) != data.length) {
            data = Arrays.copyOfRange(data, offset, offset + len);
        }
        String encoded = b64variant.encode(data);
        // base64 needs no escaping
        _writeRaw('\'');
        _writeRaw(encoded);
        _writeRaw('\'');
        writeValueEnd();
    }

    /*
    /**********************************************************************
    /* Output method implementations, scalars
    /**********************************************************************
     */

    @Override
    public void writeBoolean(boolean state) throws IOException {
        _verifyValueWrite("write boolean value");
        _writeRaw(state ? "true" : "false");
        writeValueEnd();
    }

    @Override
    public void writeNumber(short v) throws IOException {
        writeNumber((int) v);
        writeValueEnd();
    }

    @Override
    public void writeNumber(int i) throws IOException {
        _verifyValueWrite("write number");
        _writeRaw(String.valueOf(i));
        writeValueEnd();
    }

    @Override
    public void writeNumber(long l) throws IOException {
        _verifyValueWrite("write number");
        _writeRaw(String.valueOf(l));
        writeValueEnd();
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException {
        if (v == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writeRaw(String.valueOf(v));
        writeValueEnd();
    }

    @Override
    public void writeNumber(double d) throws IOException {
        _verifyValueWrite("write number");
        _writeRaw(String.valueOf(d));
        writeValueEnd();
    }

    @Override
    public void writeNumber(float f) throws IOException {
        _verifyValueWrite("write number");
        _writeRaw(String.valueOf(f));
        writeValueEnd();
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException {
        if (dec == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        String str = isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN) ? dec.toPlainString() : dec.toString();
        _writeRaw(str);
        writeValueEnd();
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException {
        if (encodedValue == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writeRaw(encodedValue);
        writeValueEnd();
    }

    @Override
    public void writeNull() throws IOException {
        throw new UnsupportedOperationException("Nulls are not supported by TOML");
    }

    /*
    /**********************************************************************
    /* Implementations for methods from base class
    /**********************************************************************
     */

    @Override
    protected void _verifyValueWrite(String typeMsg) throws IOException {
        _verifyValueWrite(typeMsg, true);
    }

    protected void _verifyValueWrite(String typeMsg, boolean forceMaterializeKey) throws IOException {
        // check that name/value cadence works
        if (!_streamWriteContext.writeValue()) {
            _reportError("Cannot " + typeMsg + ", expecting a property name");
        }
        if (_streamWriteContext._inline) {
            // write separators, if we're inline the key is already there
            if (_streamWriteContext.inArray()) {
                if (_streamWriteContext.getCurrentIndex() != 0) {
                    _writeRaw(", ");
                }
            } else {
                _writeRaw(" = ");
            }
        } else {
            // write the key if necessary
            if (forceMaterializeKey) {
                writeCurrentPath();
            }
        }
    }

    private void writeCurrentPath() throws IOException {
        _writeRaw(_basePath);
        _writeRaw(" = ");
    }

    private void writeValueEnd() throws IOException {
        if (!_streamWriteContext._inline) {
            writeRaw('\n');
        }
    }

    /*
    /**********************************************************************
    /* String support
    /**********************************************************************
     */

    private void _appendPropertyName(StringBuilder path, String name) throws IOException {
        int cat = StringOutputUtil.categorize(name) & StringOutputUtil.MASK_SIMPLE_KEY;
        if ((cat & StringOutputUtil.UNQUOTED_KEY) != 0) {
            path.append(name);
        } else if ((cat & StringOutputUtil.LITERAL_STRING) != 0) {
            path.append('\'').append(name).append('\'');
        } else if ((cat & StringOutputUtil.BASIC_STRING_NO_ESCAPE) != 0) {
            path.append('"').append(name).append('"');
        } else if ((cat & StringOutputUtil.BASIC_STRING) != 0) {
            path.append('"');
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                String escape = StringOutputUtil.getBasicStringEscape(c);
                if (escape == null) {
                    path.append(c);
                } else {
                    path.append(escape);
                }
            }
            path.append('"');
        } else {
            throw new TomlStreamWriteException("Key contains unsupported characters", this);
        }
        // NOTE: we do NOT yet write the key; wait until we have value; just append to path
    }

    private void _writeStringImpl(int categoryMask, String name) throws IOException {
        int cat = StringOutputUtil.categorize(name) & categoryMask;
        if ((cat & StringOutputUtil.UNQUOTED_KEY) != 0) {
            _writeRaw(name);
        } else if ((cat & StringOutputUtil.LITERAL_STRING) != 0) {
            _writeRaw('\'');
            _writeRaw(name);
            _writeRaw('\'');
        } else if ((cat & StringOutputUtil.BASIC_STRING_NO_ESCAPE) != 0) {
            _writeRaw('"');
            _writeRaw(name);
            _writeRaw('"');
        } else if ((cat & StringOutputUtil.BASIC_STRING) != 0) {
            _writeRaw('"');
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                String escape = StringOutputUtil.getBasicStringEscape(c);
                if (escape == null) {
                    _writeRaw(c);
                } else {
                    _writeRaw(escape);
                }
            }
            _writeRaw('"');
        } else {
            throw new TomlStreamWriteException("Key contains unsupported characters", this);
        }
    }

    private void _writeStringImpl(int categoryMask, char[] text, int offset, int len) throws IOException {
        int cat = StringOutputUtil.categorize(text, offset, len) & categoryMask;
        if ((cat & StringOutputUtil.UNQUOTED_KEY) != 0) {
            _writeRaw(text, offset, len);
        } else if ((cat & StringOutputUtil.LITERAL_STRING) != 0) {
            _writeRaw('\'');
            _writeRaw(text, offset, len);
            _writeRaw('\'');
        } else if ((cat & StringOutputUtil.BASIC_STRING_NO_ESCAPE) != 0) {
            _writeRaw('"');
            _writeRaw(text, offset, len);
            _writeRaw('"');
        } else if ((cat & StringOutputUtil.BASIC_STRING) != 0) {
            _writeRaw('"');
            for (int i = 0; i < len; i++) {
                char c = text[offset + len];
                String escape = StringOutputUtil.getBasicStringEscape(c);
                if (escape == null) {
                    _writeRaw(c);
                } else {
                    _writeRaw(escape);
                }
            }
            _writeRaw('"');
        } else {
            throw new TomlStreamWriteException("Key contains unsupported characters", this);
        }
    }

    /*
    /**********************************************************************
    /* Time types
    /**********************************************************************
     */

    @Override
    public void writePOJO(Object value) throws IOException {
        if (value == null) {
            // important: call method that does check value write:
            writeNull();
        } else if (
                value instanceof LocalDate ||
                value instanceof LocalTime ||
                value instanceof LocalDateTime ||
                value instanceof OffsetDateTime) {
            _verifyValueWrite("write local date");
            _writeRaw(value.toString());
            writeValueEnd();
        } else {
            _objectCodec.writeValue(this, value);
        }
    }
}
