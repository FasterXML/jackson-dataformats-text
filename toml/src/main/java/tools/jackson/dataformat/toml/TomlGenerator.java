package tools.jackson.dataformat.toml;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Arrays;

import tools.jackson.core.*;
import tools.jackson.core.base.GeneratorBase;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.JacksonFeatureSet;
import tools.jackson.core.util.VersionUtil;
import tools.jackson.databind.JsonNode;

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

    /**
     * Underlying {@link Writer} used for output.
     */
    protected final Writer _out;

    private final int _tomlFeatures;

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

    public TomlGenerator(ObjectWriteContext writeCtxt, IOContext ioCtxt,
            int stdFeatures, int tomlFeatures, Writer out) {
        super(writeCtxt, ioCtxt, stdFeatures);
        _tomlFeatures = tomlFeatures;
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
    /* Overridden methods, configuration
    /**********************************************************************
     */

    @Override
    public Object streamWriteOutputTarget() {
        return _out;
    }

    @Override
    public int streamWriteOutputBuffered() {
        return _outputTail;
    }

    /*
    /**********************************************************************
    /* Overridden methods: low-level I/O
    /**********************************************************************
     */

    @Override
    public void close() {
        if (!isClosed()) {
            try {
                _flushBuffer();
            } finally {
                super.close();
                _outputTail = 0; // just to ensure we don't think there's anything buffered
            }
        }
    }

    @Override
    protected void _closeInput() throws IOException
    {
        if (_out != null) {
            if (_ioContext.isResourceManaged() || isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET)) {
                _out.close();
            } else if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
                // If we can't close it, we should at least flush
                _out.flush();
            }
        }
    }

    @Override
    public void flush() {
        _flushBuffer();
        if (_out != null) {
            if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
                try {
                    _out.flush();
                } catch (IOException e) {
                    throw _wrapIOFailure(e);
                }
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

    protected void _flushBuffer() throws JacksonException {
        if (_outputTail > 0 && _out != null) {
            try {
                _out.write(_outputBuffer, 0, _outputTail);
            } catch (IOException e) {
                throw _wrapIOFailure(e);
            }
            _outputTail = 0;
        }
    }

    /*
    /**********************************************************************
    /* Internal methods; raw writes
    /**********************************************************************
     */

    protected JsonGenerator _writeRaw(char c) throws JacksonException {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = c;
        return this;
    }

    protected JsonGenerator _writeRaw(String text) throws JacksonException {
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
        return this;
    }

    protected JsonGenerator _writeRaw(StringBuilder text) throws JacksonException {
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
        return this;
    }

    protected JsonGenerator _writeRaw(char[] text, int offset, int len) throws JacksonException {
        // Only worth buffering if it's a short write?
        if (len < SHORT_WRITE) {
            int room = _outputEnd - _outputTail;
            if (len > room) {
                _flushBuffer();
            }
            System.arraycopy(text, offset, _outputBuffer, _outputTail, len);
            _outputTail += len;
            return this;
        }
        // Otherwise, better just pass through:
        _flushBuffer();
        try {
            _out.write(text, offset, len);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
        return this;
    }

    protected void _writeRawLong(String text) throws JacksonException {
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

    protected void _writeRawLong(StringBuilder text) throws JacksonException {
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

    @Override
    public boolean canOmitProperties() {
        return true;
    }

    @Override
    public JacksonFeatureSet<StreamWriteCapability> streamWriteCapabilities() {
        return DEFAULT_WRITE_CAPABILITIES;
    }

    /*
    /**********************************************************************
    /* Overrides: config access
    /**********************************************************************
     */

    @Override
    public PrettyPrinter getPrettyPrinter() {
        return null;
    }

    /*
    /**********************************************************************
    /* Overridden methods; writing property names
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeName(String name) throws JacksonException {
        if (!_streamWriteContext.writeName(name)) {
            _reportError("Cannot write a property name, expecting a value");
        }

        if (_streamWriteContext.isInline()) {
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
        return this;
    }

    @Override
    public JsonGenerator writePropertyId(long id) throws JacksonException {
        // 15-Aug-2019, tatu: should be improved to avoid String generation
        return writeName(Long.toString(id));
    }

    /*
    /**********************************************************************
    /* Public API: structural output
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeStartArray() throws JacksonException {
        return writeStartArray(null);
    }

    @Override
    public JsonGenerator writeStartArray(Object currValue) throws JacksonException {
        // Section/AOT syntax is only valid in non-inline scopes. Inside an
        // inline array or inline table the only valid form is a regular
        // inline array, so a flagged TomlArrayNode reused there must fall
        // through to plain serialization.
        if (TomlNodes.isArrayOfTables(currValue)
                && !_streamWriteContext.isInline()
                && _hasSectionAnchor()) {
            TomlArrayNode arr = (TomlArrayNode) currValue;
            // TOML has no empty array-of-tables form ([[path]] requires at least
            // one element). If a parsed [[items]] has been emptied (or a caller
            // started one without elements), drop AOT mode and emit an inline
            // empty array so the field still survives in the output.
            if (arr.size() > 0) {
                _verifyArrayOfTablesElements(arr);
                return writeStartArrayOfTables(currValue);
            }
        }
        // arrays are always inline, force writing the current key
        // NOTE: if this ever changes, we need to add empty array handling in writeEndArray
        _verifyValueWrite("start an array", true);
        _streamWriteContext = _streamWriteContext.createChildArrayContext(currValue,
                _basePath.length(), TomlWriteContext.Kind.INLINE_ARRAY);
        streamWriteConstraints().validateNestingDepth(_streamWriteContext.getNestingDepth());
        if (_streamWriteContext.isInline()) {
            _writeRaw('[');
        }
        return this;
    }

    private void _verifyArrayOfTablesElements(TomlArrayNode array) throws JacksonException {
        for (int i = 0, n = array.size(); i < n; ++i) {
            JsonNode el = array.get(i);
            if (!el.isObject()) {
                throw new TomlStreamWriteException(this,
                        "Array marked as TOML array-of-tables (parsed `[[path]]`) cannot contain "
                        + "non-object element at index " + i + " (was " + el.getNodeType()
                        + "); replace the elements with objects, or remove the array and add a "
                        + "fresh inline array, before serialization");
            }
        }
    }

    @Override
    public JsonGenerator writeEndArray() throws JacksonException {
        if (!_streamWriteContext.inArray()) {
            _reportError("Current context not an Array but " + _streamWriteContext.typeDesc());
        }
        TomlWriteContext.Kind kind = _streamWriteContext._kind;
        if (kind == TomlWriteContext.Kind.INLINE_ARRAY) {
            _writeRaw(']');
            _streamWriteContext = _streamWriteContext.getParent();
            return writeValueEnd();
        }
        if (kind == TomlWriteContext.Kind.ARRAY_OF_TABLES) {
            if (!_streamWriteContext.hasCurrentIndex()) {
                throw new TomlStreamWriteException(this,
                        "Cannot end an empty TOML array-of-tables: TOML has no "
                        + "empty [[path]] form, write at least one element or "
                        + "use writeStartArray for an inline empty array");
            }
            _restoreOuterBasePath(_streamWriteContext);
            _streamWriteContext = _streamWriteContext.getParent();
            return this;
        }
        if (!_streamWriteContext.hasCurrentIndex()) {
            VersionUtil.throwInternal();
        }
        _streamWriteContext = _streamWriteContext.getParent();
        return writeValueEnd();
    }

    @Override
    public JsonGenerator writeStartObject() throws JacksonException {
        return writeStartObject(null);
    }

    @Override
    public JsonGenerator writeStartObject(Object forValue) throws JacksonException {
        if (_streamWriteContext._kind == TomlWriteContext.Kind.ARRAY_OF_TABLES) {
            return _startArrayOfTablesElement(forValue);
        }
        // See writeStartArray: section headers are illegal inside inline
        // scopes, so a flagged TomlObjectNode reused there falls through to
        // plain inline-object serialization.
        if (TomlNodes.isTableHeader(forValue)
                && !_streamWriteContext.isInline()
                && _hasSectionAnchor()) {
            return writeStartTable(forValue);
        }
        // objects aren't always materialized right now
        _verifyValueWrite("start an object", false);
        _streamWriteContext = _streamWriteContext.createChildObjectContext(forValue, _basePath.length());
        streamWriteConstraints().validateNestingDepth(_streamWriteContext.getNestingDepth());
        if (_streamWriteContext.isInline()) {
            writeRaw('{');
        }
        return this;
    }

    @Override
    public JsonGenerator writeEndObject() throws JacksonException {
        if (!_streamWriteContext.inObject()) {
            _reportError("Current context not an Object but " + _streamWriteContext.typeDesc());
        }
        TomlWriteContext.Kind kind = _streamWriteContext._kind;
        if (kind == TomlWriteContext.Kind.INLINE_OBJECT) {
            writeRaw('}');
            _streamWriteContext = _streamWriteContext.getParent();
            return writeValueEnd();
        }
        if (kind == TomlWriteContext.Kind.TABLE) {
            _restoreOuterBasePath(_streamWriteContext);
            _streamWriteContext = _streamWriteContext.getParent();
            return this;
        }
        if (!_streamWriteContext.hasCurrentIndex()) {
            writeCurrentPath();
            _writeRaw("{}");
            writeValueEnd();
        }
        _streamWriteContext = _streamWriteContext.getParent();
        return this;
    }

    /*
    /**********************************************************************
    /* TOML-specific structural output: tables and array of tables
    /**********************************************************************
     */

    /**
     * Begin a TOML standard table (e.g. {@code [foo.bar]}). Must be called
     * after a {@link #writeName(String)} that supplies the table's key.
     * Locks the parent context against further scalar writes: TOML requires
     * scalars to precede sub-tables.
     */
    public TomlGenerator writeStartTable() throws JacksonException {
        return writeStartTable(null);
    }

    public TomlGenerator writeStartTable(Object forValue) throws JacksonException {
        String fullPath = _openSection("table", false);
        _writeRaw('[');
        _writeRaw(fullPath);
        _writeRaw(']');
        _writeRaw('\n');

        String outerBasePath = _basePath.length() == 0 ? null : _basePath.toString();
        _basePath.setLength(0);
        _streamWriteContext = _streamWriteContext.createChildObjectContext(
                forValue, _basePath.length(), TomlWriteContext.Kind.TABLE);
        _streamWriteContext._savedPath = fullPath;
        _streamWriteContext._savedOuterBasePath = outerBasePath;
        streamWriteConstraints().validateNestingDepth(_streamWriteContext.getNestingDepth());
        return this;
    }

    /**
     * Begin a TOML array of tables (e.g. {@code [[foo.bar]]}). Each subsequent
     * {@link #writeStartObject()} inside the array emits another {@code [[path]]}
     * header.
     */
    public TomlGenerator writeStartArrayOfTables() throws JacksonException {
        return writeStartArrayOfTables(null);
    }

    public TomlGenerator writeStartArrayOfTables(Object currValue) throws JacksonException {
        String fullPath = _openSection("array of tables", true);
        // Per-element [[path]] headers are emitted lazily in _startArrayOfTablesElement.
        // Snapshot _basePath now: per-element TABLE setup will zero it, so the
        // sibling-write path after this AOT closes needs the outer prefix back.
        String outerBasePath = _basePath.length() == 0 ? null : _basePath.toString();
        _streamWriteContext = _streamWriteContext.createChildArrayContext(
                currValue, _basePath.length(), TomlWriteContext.Kind.ARRAY_OF_TABLES);
        _streamWriteContext._savedPath = fullPath;
        _streamWriteContext._savedOuterBasePath = outerBasePath;
        streamWriteConstraints().validateNestingDepth(_streamWriteContext.getNestingDepth());
        return this;
    }

    /**
     * Shared prologue for {@link #writeStartTable} and
     * {@link #writeStartArrayOfTables}: validates context, consumes the
     * pending name, builds the absolute header path, locks the parent
     * against further scalars, and (for tables) emits a leading blank-line
     * separator. Returns the absolute path for the new section.
     *
     * @param what            human-readable kind for error messages
     * @param suppressLeadGap when true, the caller is responsible for the
     *                        per-element separator (array-of-tables case)
     */
    private String _openSection(String what, boolean suppressLeadGap) throws JacksonException {
        if (_streamWriteContext.isInline()) {
            throw new TomlStreamWriteException(this,
                    "Cannot start a TOML " + what + " inside an inline context");
        }
        if (!_streamWriteContext.writeValue()) {
            _reportError("Cannot start a " + what + ", expecting a property name");
        }
        String fullPath = _buildSectionPath(_currentEnclosingPath(), _basePath);
        if (fullPath.isEmpty()) {
            throw new TomlStreamWriteException(this,
                    "Cannot start a TOML " + what + " without a name");
        }
        // Lock every scope from here up to (but not including) the nearest
        // enclosing TABLE/AOT against further scalars: once a `[header]` has
        // been emitted, any later scalar at any of those levels would land
        // inside the new section in TOML's grammar.
        for (TomlWriteContext c = _streamWriteContext; c != null; c = c.getParent()) {
            c._scalarsClosed = true;
            if (c._kind == TomlWriteContext.Kind.TABLE
                    || c._kind == TomlWriteContext.Kind.ARRAY_OF_TABLES) {
                break;
            }
        }
        if (!suppressLeadGap && _needsLeadingBlankLine()) {
            _writeRaw('\n');
        }
        return fullPath;
    }

    private boolean _needsLeadingBlankLine() {
        return _streamWriteContext.getCurrentIndex() > 0
                || _streamWriteContext._kind != TomlWriteContext.Kind.ROOT;
    }

    // True when the current position has a name to anchor a `[section]` header:
    // either an enclosing TABLE/AOT path, or a buffered relative key from a
    // pending writeName(). When false (e.g. parsed subtree written as the root
    // value with no field name), table-flagged metadata cannot trigger
    // table-syntax output, so we fall through to plain serialization.
    private boolean _hasSectionAnchor() {
        return !_currentEnclosingPath().isEmpty() || _basePath.length() > 0;
    }

    private static String _buildSectionPath(String parent, StringBuilder relative) {
        if (parent.isEmpty()) {
            return relative.toString();
        }
        if (relative.length() == 0) {
            return parent;
        }
        StringBuilder sb = new StringBuilder(parent.length() + 1 + relative.length());
        sb.append(parent).append('.').append(relative);
        return sb.toString();
    }

    // Restores _basePath to the prefix that existed before the section opened,
    // so the parent's in-progress dotted-key sequence survives across the section.
    private void _restoreOuterBasePath(TomlWriteContext closing) {
        _basePath.setLength(0);
        if (closing._savedOuterBasePath != null) {
            _basePath.append(closing._savedOuterBasePath);
        }
    }

    /**
     * Section path that scopes content written into the current context, or
     * {@code ""} for the document root. For TABLE/ARRAY_OF_TABLES this is
     * the context's own {@code _savedPath}; otherwise it is the cached
     * enclosing path (inherited at construction). O(1).
     */
    private String _currentEnclosingPath() {
        TomlWriteContext ctxt = _streamWriteContext;
        if (ctxt._kind == TomlWriteContext.Kind.TABLE
                || ctxt._kind == TomlWriteContext.Kind.ARRAY_OF_TABLES) {
            return ctxt._savedPath == null ? "" : ctxt._savedPath;
        }
        return ctxt._enclosingSectionPath;
    }

    private JsonGenerator _startArrayOfTablesElement(Object forValue) throws JacksonException {
        if (!_streamWriteContext.writeValue()) {
            _reportError("Cannot start an array-of-tables element, expecting array context");
        }
        String fullPath = _streamWriteContext._savedPath;

        TomlWriteContext parent = _streamWriteContext.getParent();
        boolean atDocStart = _streamWriteContext.getCurrentIndex() == 0
                && (parent == null || parent._kind == TomlWriteContext.Kind.ROOT);
        if (!atDocStart) {
            _writeRaw('\n');
        }
        _writeRaw("[[");
        _writeRaw(fullPath);
        _writeRaw("]]");
        _writeRaw('\n');

        _basePath.setLength(0);
        _streamWriteContext = _streamWriteContext.createChildObjectContext(
                forValue, _basePath.length(), TomlWriteContext.Kind.TABLE);
        _streamWriteContext._savedPath = fullPath;
        streamWriteConstraints().validateNestingDepth(_streamWriteContext.getNestingDepth());
        return this;
    }

    /*
    /**********************************************************************
    /* Output method implementations, textual
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeString(String text) throws JacksonException {
        if (text == null) {
            return writeNull();
        }
        _verifyValueWrite("write String value");
        _writeStringImpl(StringOutputUtil.MASK_STRING, text);
        return writeValueEnd();
    }

    @Override
    public JsonGenerator writeString(char[] text, int offset, int len) throws JacksonException {
        _verifyValueWrite("write String value");
        _writeStringImpl(StringOutputUtil.MASK_STRING, text, offset, len);
        return writeValueEnd();
    }

    @Override
    public JsonGenerator writeRawUTF8String(byte[] text, int offset, int len) throws JacksonException {
        return _reportUnsupportedOperation();
    }

    @Override
    public JsonGenerator writeUTF8String(byte[] text, int offset, int len) throws JacksonException {
        writeString(new String(text, offset, len, StandardCharsets.UTF_8));
        return writeValueEnd();
    }

    /*
    /**********************************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeRaw(String text) throws JacksonException {
        return _writeRaw(text);
    }

    @Override
    public JsonGenerator writeRaw(String text, int offset, int len) throws JacksonException {
        return _writeRaw(text.substring(offset, offset + len));
    }

    @Override
    public JsonGenerator writeRaw(char[] text, int offset, int len) throws JacksonException {
        return _writeRaw(text, offset, len);
    }

    @Override
    public JsonGenerator writeRaw(char c) throws JacksonException {
        return _writeRaw(c);
    }

    @Override
    public JsonGenerator writeRaw(SerializableString text) throws JacksonException {
        return writeRaw(text.toString());
    }

    /*
    /**********************************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeBinary(Base64Variant b64variant, byte[] data, int offset, int len)
            throws JacksonException {
        if (data == null) {
            return writeNull();
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
        return writeValueEnd();
    }

    /*
    /**********************************************************************
    /* Output method implementations, scalars
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeBoolean(boolean state) throws JacksonException {
        _verifyValueWrite("write boolean value");
        _writeRaw(state ? "true" : "false");
        return writeValueEnd();
    }

    @Override
    public JsonGenerator writeNumber(short v) throws JacksonException {
        writeNumber((int) v);
        return writeValueEnd();
    }

    @Override
    public JsonGenerator writeNumber(int i) throws JacksonException {
        _verifyValueWrite("write number");
        _writeRaw(String.valueOf(i));
        return writeValueEnd();
    }

    @Override
    public JsonGenerator writeNumber(long l) throws JacksonException {
        _verifyValueWrite("write number");
        _writeRaw(String.valueOf(l));
        return writeValueEnd();
    }

    @Override
    public JsonGenerator writeNumber(BigInteger v) throws JacksonException {
        if (v == null) {
            return writeNull();
        }
        _verifyValueWrite("write number");
        _writeRaw(String.valueOf(v));
        return writeValueEnd();
    }

    @Override
    public JsonGenerator writeNumber(double d) throws JacksonException {
        _verifyValueWrite("write number");
        _writeRaw(String.valueOf(d));
        return writeValueEnd();
    }

    @Override
    public JsonGenerator writeNumber(float f) throws JacksonException {
        _verifyValueWrite("write number");
        _writeRaw(String.valueOf(f));
        return writeValueEnd();
    }

    @Override
    public JsonGenerator writeNumber(BigDecimal dec) throws JacksonException {
        if (dec == null) {
            return writeNull();
        }
        _verifyValueWrite("write number");
        String str = isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN) ? dec.toPlainString() : dec.toString();
        _writeRaw(str);
        return writeValueEnd();
    }

    @Override
    public JsonGenerator writeNumber(String encodedValue) throws JacksonException {
        if (encodedValue == null) {
            return writeNull();
        }
        _verifyValueWrite("write number");
        _writeRaw(encodedValue);
        return writeValueEnd();
    }

    @Override
    public JsonGenerator writeNull() throws JacksonException {
        if (TomlWriteFeature.FAIL_ON_NULL_WRITE.enabledIn(_tomlFeatures)) {
            throw new TomlStreamWriteException(this, "TOML null writing disabled (TomlWriteFeature.FAIL_ON_NULL_WRITE)");
        }
        _verifyValueWrite("write null value");
        _writeStringImpl(StringOutputUtil.MASK_STRING, "");
        return writeValueEnd();
    }

    /*
    /**********************************************************************
    /* Implementations for methods from base class
    /**********************************************************************
     */

    @Override
    protected void _verifyValueWrite(String typeMsg) throws JacksonException {
        _verifyValueWrite(typeMsg, true);
    }

    protected void _verifyValueWrite(String typeMsg, boolean forceMaterializeKey) throws JacksonException {
        if (_streamWriteContext._kind == TomlWriteContext.Kind.ARRAY_OF_TABLES) {
            throw new TomlStreamWriteException(this,
                    "Cannot " + typeMsg + " inside a TOML array-of-tables: each "
                    + "element must be an object (writeStartObject)");
        }
        // Strict ordering: scalars (and inline values) cannot follow a sub-table at the same level.
        if (_streamWriteContext._scalarsClosed) {
            throw new TomlStreamWriteException(this,
                    "Cannot " + typeMsg + " after a sub-table has been opened at this level "
                    + "(TOML requires scalars to precede sub-tables / arrays of tables)");
        }
        // check that name/value cadence works
        if (!_streamWriteContext.writeValue()) {
            _reportError("Cannot " + typeMsg + ", expecting a property name");
        }
        if (_streamWriteContext.isInline()) {
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

    private void writeCurrentPath() {
        _writeRaw(_basePath);
        _writeRaw(" = ");
    }

    private JsonGenerator writeValueEnd() {
        if (!_streamWriteContext.isInline()) {
            writeRaw('\n');
        }
        return this;
    }

    /*
    /**********************************************************************
    /* String support
    /**********************************************************************
     */

    private void _appendPropertyName(StringBuilder path, String name) {
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
            throw _constructWriteException("Key contains unsupported characters");
        }
        // NOTE: we do NOT yet write the key; wait until we have value; just append to path
    }

    private void _writeStringImpl(int categoryMask, String name) {
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
            throw _constructWriteException("Key contains unsupported characters");
        }
    }

    private void _writeStringImpl(int categoryMask, char[] text, int offset, int len) {
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
            throw _constructWriteException("Key contains unsupported characters");
        }
    }

    /*
    /**********************************************************************
    /* Time types
    /**********************************************************************
     */

    @Override
    public JsonGenerator writePOJO(Object value) throws JacksonException {
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
            _objectWriteContext.writeValue(this, value);
        }
        return this;
    }
}
