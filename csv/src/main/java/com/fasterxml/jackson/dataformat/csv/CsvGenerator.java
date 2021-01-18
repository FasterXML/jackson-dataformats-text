package com.fasterxml.jackson.dataformat.csv;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.json.DupDetector;
import com.fasterxml.jackson.core.util.SimpleTokenWriteContext;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.JacksonFeatureSet;
import com.fasterxml.jackson.dataformat.csv.impl.CsvEncoder;

public class CsvGenerator extends GeneratorBase
{
    /**
     * Enumeration that defines all togglable features for CSV writers
     * (if any: currently none)
     */
    public enum Feature
        implements FormatFeature
    {
        /**
         * Feature that determines how much work is done before determining that
         * a column value requires quoting: when set as <code>true</code>, full
         * check is made to only use quoting when it is strictly necessary;
         * but when <code>false</code>, a faster but more conservative check
         * is made, and possibly quoting is used for values that might not need it.
         * Trade-offs is basically between optimal/minimal quoting (true), and
         * faster handling (false).
         * Faster check involves only checking first N characters of value, as well
         * as possible looser checks.
         *<p>
         * Note, however, that regardless setting, all values that need to be quoted
         * will be: it is just that when set to <code>false</code>, other values may
         * also be quoted (to avoid having to do more expensive checks).
         *<p>
         * Default value is <code>false</code> for "loose" (approximate, conservative)
         * checking.
         */
        STRICT_CHECK_FOR_QUOTING(false),

        /**
         * Feature that determines whether columns without matching value may be omitted,
         * when they are the last values of the row.
         * If <code>true</code>, values and separators between values may be omitted, to slightly reduce
         * length of the row; if <code>false</code>, separators need to stay in place and values
         * are indicated by empty Strings.
         */
        OMIT_MISSING_TAIL_COLUMNS(false),

        /**
         * Feature that determines whether values written as Strings (from <code>java.lang.String</code>
         * valued POJO properties) should be forced to be quoted, regardless of whether they
         * actually need this.
         * Note that this feature has precedence over {@link #STRICT_CHECK_FOR_QUOTING}, when
         * both would be applicable.
         */
        ALWAYS_QUOTE_STRINGS(false),

        /**
         * Feature that determines whether values written as empty Strings (from <code>java.lang.String</code>
         * valued POJO properties) should be forced to be quoted.
         */
        ALWAYS_QUOTE_EMPTY_STRINGS(false),

        /**
         * Feature that determines whether quote characters within quoted String values are escaped
         * using configured escape character, instead of being "doubled up" (that is: a quote character
         * is written twice in a row).
         *<p>
         * Default value is false so that quotes are doubled as necessary, not escaped.
         */
        ESCAPE_QUOTE_CHAR_WITH_ESCAPE_CHAR(false),

        /**
         * Feature that determines whether control characters (non-printable) are escaped using the
         * configured escape character. This feature allows LF and CR characters to be output as <pre>\n</pre>
         * and <pre>\r</pre> instead of being echoed out. This is a compatibility feature for some
         * parsers that can not read such output back in.
         * <p>
         * Default value is false so that control characters are echoed out (backwards compatible).
         */
        ESCAPE_CONTROL_CHARS_WITH_ESCAPE_CHAR(false)
        ;

        protected final boolean _defaultState;
        protected final int _mask;
        
        /**
         * Method that calculates bit set (flags) of all features that
         * are enabled by default.
         */
        public static int collectDefaults()
        {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) {
                    flags |= f.getMask();
                }
            }
            return flags;
        }

        private Feature(boolean defaultState) {
            _defaultState = defaultState;
            _mask = (1 << ordinal());
        }

        @Override
        public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
        @Override
        public boolean enabledByDefault() { return _defaultState; }
        @Override
        public int getMask() { return _mask; }
    }

    protected final static long MIN_INT_AS_LONG = Integer.MIN_VALUE;
    protected final static long MAX_INT_AS_LONG = Integer.MAX_VALUE;
    
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    private final static CsvSchema EMPTY_SCHEMA = CsvSchema.emptySchema();

    final protected IOContext _ioContext;

    /**
     * Bit flag composed of bits that indicate which
     * {@link CsvGenerator.Feature}s
     * are enabled.
     */
    protected int _formatFeatures;

    /**
     * Definition of columns being written, if available.
     */
    protected final CsvSchema _schema;

    // note: can not be final since we may need to re-create it for new schema
    protected CsvEncoder _writer;

    /*
    /**********************************************************************
    /* Output state
    /**********************************************************************
     */

    /**
     * Object that keeps track of the current contextual state of the generator.
     */
    protected SimpleTokenWriteContext _tokenWriteContext;

    /**
     * Flag that indicates that we need to write header line, if
     * one is needed. Used because schema may be specified after
     * instance is constructed.
     */
    protected boolean _handleFirstLine = true;
    
    /**
     * Index of column that we will be getting next, based on
     * field name call that was made.
     */
    protected int _nextColumnByName = -1;

    /**
     * Flag set when property to write is unknown, and the matching value
     * is to be skipped quietly.
     */
    protected boolean _skipValue;

    /**
     * Separator to use during writing of (simple) array value, to be encoded as a
     * single column value, if any.
     */
    protected String _arraySeparator = CsvSchema.NO_ARRAY_ELEMENT_SEPARATOR;

    /**
     * Accumulated contents of an array cell, if any
     */
    protected StringBuilder _arrayContents;

    /**
     * Additional counter that indicates number of value entries in the
     * array. Needed because `null` entries do not add content, but need
     * to be separated by array cell separator
     */
    protected int _arrayElements;

    /**
     * When skipping output (for "unknown" output), outermost write context
     * where skipping should occur
     */
    protected SimpleTokenWriteContext _skipWithin;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public CsvGenerator(ObjectWriteContext writeCtxt, IOContext ioCtxt,
            int streamWriteFeatures, int csvFeatures,
            Writer out, CsvSchema schema, CsvCharacterEscapes characterEscapes)
    {
        super(writeCtxt, streamWriteFeatures);
        _ioContext = ioCtxt;
        _formatFeatures = csvFeatures;
        final DupDetector dups = StreamWriteFeature.STRICT_DUPLICATE_DETECTION.enabledIn(streamWriteFeatures)
                ? DupDetector.rootDetector(this) : null;
        _tokenWriteContext = SimpleTokenWriteContext.createRootContext(dups);
        _schema = schema;

        if (characterEscapes == null) {
            characterEscapes = CsvCharacterEscapes.fromCsvFeatures(csvFeatures);
        }
        _writer = new CsvEncoder(ioCtxt, csvFeatures, out, schema, characterEscapes);
    }

    public CsvGenerator(ObjectWriteContext writeCtxt, IOContext ioCtxt,
            int streamWriteFeatures, int csvFeatures,
            CsvEncoder csvWriter)
    {
        super(writeCtxt, streamWriteFeatures);
        _ioContext = ioCtxt;
        _formatFeatures = csvFeatures;
        final DupDetector dups = StreamWriteFeature.STRICT_DUPLICATE_DETECTION.enabledIn(streamWriteFeatures)
                ? DupDetector.rootDetector(this) : null;
        _tokenWriteContext = SimpleTokenWriteContext.createRootContext(dups);
        _schema = EMPTY_SCHEMA;
        _writer = csvWriter;
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
    public final TokenStreamContext getOutputContext() { return _tokenWriteContext; }

    @Override
    public final Object getCurrentValue() {
        return _tokenWriteContext.getCurrentValue();
    }

    @Override
    public final void setCurrentValue(Object v) {
        _tokenWriteContext.setCurrentValue(v);
    }

    /*
    /**********************************************************************
    /* Overridden methods, configuration
    /**********************************************************************
     */

    @Override
    public Object getOutputTarget() {
        return _writer.getOutputTarget();
    }

    /**
     * NOTE: while this method will return some information on amount of data buffered, it
     * may be an incomplete view as some buffering happens at a higher level, as not-yet-serialized
     * values.
     */
    @Override
    public int getOutputBuffered() {
        return _writer.getOutputBuffered();
    }

    @Override
    public CsvGenerator setCharacterEscapes(CharacterEscapes esc) {
        if (esc != null) {
            _writer.setOutputEscapes(esc.getEscapeCodesForAscii());
        }
        return this;
    }

    @Override
    public CharacterEscapes getCharacterEscapes() {
        return null;
    }

    /*
    /**********************************************************************
    /* Public API, capability introspection methods
    /**********************************************************************
     */

    @Override
    public boolean canOmitFields() {
        // Nope: CSV requires at least a placeholder
        return false;
    }

    @Override
    public boolean canWriteFormattedNumbers() { return true; }

    @Override // @since 2.12
    public JacksonFeatureSet<StreamWriteCapability> getWriteCapabilities() {
        return DEFAULT_TEXTUAL_WRITE_CAPABILITIES;
    }

    /*
    /**********************************************************************
    /* Overridden methods; writing field names
    /**********************************************************************
     */
    
    /* And then methods overridden to make final, streamline some
     * aspects...
     */

    @Override
    public final void writeFieldName(String name) throws JacksonException
    {
        if (!_tokenWriteContext.writeFieldName(name)) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(name);
    }

    @Override
    public void writeFieldId(long id) throws JacksonException {
        // 15-Aug-2019, tatu: should be improved to avoid String generation
        writeFieldName(Long.toString(id));
    }

    @Override
    public final void writeFieldName(SerializableString name) throws JacksonException
    {
        // Object is a value, need to verify it's allowed
        if (!_tokenWriteContext.writeFieldName(name.getValue())) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(name.getValue());
    }

    private final void _writeFieldName(String name) throws JacksonException
    {
        // just find the matching index -- must have schema for that
        if (_schema == null) {
            // not a low-level error, so:
            _reportMappingError("Unrecognized column '"+name+"', can not resolve without CsvSchema");
        }
        if (_skipWithin != null) { // new in 2.7
            _skipValue = true;
            _nextColumnByName = -1;
            return;
        }
        // note: we are likely to get next column name, so pass it as hint
        CsvSchema.Column col = _schema.column(name, _nextColumnByName+1);
        if (col == null) {
            if (isEnabled(StreamWriteFeature.IGNORE_UNKNOWN)) {
                _skipValue = true;
                _nextColumnByName = -1;
                return;
            }
            // not a low-level error, so:
            _reportMappingError("Unrecognized column '"+name+"': known columns: "+_schema.getColumnDesc());
        }
        _skipValue = false;
        // and all we do is just note index to use for following value write
        _nextColumnByName = col.getIndex();
    }

    /*
    /**********************************************************************
    /* Extended API, configuration
    /**********************************************************************
     */

    public final boolean isEnabled(Feature f) {
        return (_formatFeatures & f.getMask()) != 0;
    }

    public CsvGenerator configure(Feature f, boolean state) {
        if (state) {
            return enable(f);
        }
        return disable(f);
    }

    public CsvGenerator enable(Feature f) {
        _formatFeatures |= f.getMask();
        _writer.overrideFormatFeatures(_formatFeatures);
        return this;
    }

    public CsvGenerator disable(Feature f) {
        _formatFeatures &= ~f.getMask();
        _writer.overrideFormatFeatures(_formatFeatures);
        return this;
    }

    /*
    /**********************************************************************
    /* Public API: low-level I/O
    /**********************************************************************
     */

    @Override
    public final void flush() {
        try {
            _writer.flush(isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM));
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
    }
    
    @Override
    public void close()
    {
        super.close();

        // Let's mark row as closed, if we had any...
        finishRow();
        
        // Write the header if necessary, occurs when no rows written
        if (_handleFirstLine) {
            _handleFirstLine();
        }
        try {
            _writer.close(_ioContext.isResourceManaged() || isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET),
                    isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM));
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
    }

    /*
    /**********************************************************************
    /* Public API: structural output
    /**********************************************************************
     */

    @Override
    public final void writeStartArray() throws JacksonException
    {
        _verifyValueWrite("start an array");
        // Ok to create root-level array to contain Objects/Arrays, but
        // can not nest arrays in objects
        if (_tokenWriteContext.inObject()) {
            if ((_skipWithin == null)
                    && _skipValue && isEnabled(StreamWriteFeature.IGNORE_UNKNOWN)) {
                _skipWithin = _tokenWriteContext;
            } else if (!_skipValue) {
                // First: column may have its own separator
                String sep;
                if (_nextColumnByName >= 0) {
                    CsvSchema.Column col = _schema.column(_nextColumnByName);
                    sep = col.isArray() ? col.getArrayElementSeparator() : CsvSchema.NO_ARRAY_ELEMENT_SEPARATOR;
                } else {
                    sep = CsvSchema.NO_ARRAY_ELEMENT_SEPARATOR;
                }
                if (sep.isEmpty()) {
                    if (!_schema.hasArrayElementSeparator()) {
                        _reportError("CSV generator does not support Array values for properties without setting 'arrayElementSeparator' in schema");
                    }
                    sep = _schema.getArrayElementSeparator();
                }
                _arraySeparator = sep;
                if (_arrayContents == null) {
                    _arrayContents = new StringBuilder();
                } else {
                    _arrayContents.setLength(0);
                }
                _arrayElements = 0;
            }
        } else {
            if (!_arraySeparator.isEmpty()) {
                // also: no nested arrays, yet
                _reportError("CSV generator does not support nested Array values");
            }
        }
        _tokenWriteContext = _tokenWriteContext.createChildArrayContext(null);
        // and that's about it, really
    }

    @Override
    public final void writeStartArray(Object currValue) throws JacksonException {
        writeStartArray();
        setCurrentValue(currValue);
    }

    @Override
    public final void writeEndArray() throws JacksonException
    {
        if (!_tokenWriteContext.inArray()) {
            _reportError("Current context not Array but "+_tokenWriteContext.typeDesc());
        }
        _tokenWriteContext = _tokenWriteContext.getParent();
        // 14-Dec-2015, tatu: To complete skipping of ignored structured value, need this:
        if (_skipWithin != null) {
            if (_tokenWriteContext == _skipWithin) {
                _skipWithin = null;
            }
            return;
        }
        if (!_arraySeparator.isEmpty()) {
            _arraySeparator = CsvSchema.NO_ARRAY_ELEMENT_SEPARATOR;
            _writer.write(_columnIndex(), _arrayContents.toString());
        }
        // 20-Nov-2014, tatu: When doing "untyped"/"raw" output, this means that row
        //    is now done. But not if writing such an array field, so:
        if (!_tokenWriteContext.inObject()) {
            finishRow();
        }
    }

    @Override
    public final void writeStartObject() throws JacksonException
    {
        _verifyValueWrite("start an object");
        // No nesting for objects; can write Objects inside logical root-level arrays.
        // 14-Dec-2015, tatu: ... except, should be fine if we are ignoring the property
        if (_tokenWriteContext.inObject() ||
                // 07-Nov-2017, tatu: But we may actually be nested indirectly; so check
                (_tokenWriteContext.inArray() && !_tokenWriteContext.getParent().inRoot())) {
            if (_skipWithin == null) { // new in 2.7
                if (_skipValue && isEnabled(StreamWriteFeature.IGNORE_UNKNOWN)) {
                    _skipWithin = _tokenWriteContext;
                } else {
                    _reportMappingError("CSV generator does not support Object values for properties (nested Objects)");
                }
            }
        }
        _tokenWriteContext = _tokenWriteContext.createChildObjectContext(null);
    }

    @Override
    public final void writeStartObject(Object currValue) throws JacksonException {
        writeStartObject();
        setCurrentValue(currValue);
    }

    @Override
    public final void writeEndObject() throws JacksonException
    {
        if (!_tokenWriteContext.inObject()) {
            _reportError("Current context not Object but "+_tokenWriteContext.typeDesc());
        }
        _tokenWriteContext = _tokenWriteContext.getParent();
        // 14-Dec-2015, tatu: To complete skipping of ignored structured value, need this:
        if (_skipWithin != null) {
            if (_tokenWriteContext == _skipWithin) {
                _skipWithin = null;
            }
            return;
        }
        // not 100% fool-proof, but chances are row should be done now
        finishRow();
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
        if (!_skipValue) {
            if (!_arraySeparator.isEmpty()) {
                _addToArray(text);
            } else {
                _writer.write(_columnIndex(), text);
            }
        }
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws JacksonException
    {
        _verifyValueWrite("write String value");
        if (!_skipValue) {
            if (!_arraySeparator.isEmpty()) {
                _addToArray(new String(text, offset, len));
            } else {
                _writer.write(_columnIndex(), text, offset, len);
            }
        }
    }

    @Override
    public final void writeString(SerializableString sstr) throws JacksonException
    {
        _verifyValueWrite("write String value");
        if (!_skipValue) {
            if (!_arraySeparator.isEmpty()) {
                _addToArray(sstr.getValue());
            } else {
                _writer.write(_columnIndex(), sstr.getValue());
            }
        }
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int len) throws JacksonException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int len) throws JacksonException {
        writeString(new String(text, offset, len, StandardCharsets.UTF_8));
    }

    /*
    /**********************************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************************
     */

    @Override
    public void writeRaw(String text) throws JacksonException {
        _writer.writeRaw(text);
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws JacksonException {
        _writer.writeRaw(text, offset, len);
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws JacksonException {
        _writer.writeRaw(text, offset, len);
    }

    @Override
    public void writeRaw(char c) throws JacksonException {
        _writer.writeRaw(c);
    }

    @Override
    public void writeRawValue(String text) throws JacksonException {
        _verifyValueWrite("write Raw value");
        if (!_skipValue) {
            // NOTE: ignore array stuff
            _writer.writeNonEscaped(_columnIndex(), text);
        }
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws JacksonException {
        _verifyValueWrite("write Raw value");
        if (!_skipValue) {
            // NOTE: ignore array stuff
            _writer.writeNonEscaped(_columnIndex(), text.substring(offset, offset+len));
        }
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws JacksonException {
        _verifyValueWrite("write Raw value");
        if (!_skipValue) {
            // NOTE: ignore array stuff
            _writer.writeNonEscaped(_columnIndex(), new String(text, offset, len));
        }
    }

    /*
    /**********************************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************************
     */

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws JacksonException, JsonGenerationException
    {
        if (data == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write Binary value");
        if (!_skipValue) {
            // ok, better just Base64 encode as a String...
            if (offset > 0 || (offset+len) != data.length) {
                data = Arrays.copyOfRange(data, offset, offset+len);
            }
            String encoded = b64variant.encode(data);

            if (!_arraySeparator.isEmpty()) {
                _addToArray(encoded);
            } else {
                _writer.write(_columnIndex(), encoded);
            }
        }
    }

    /*
    /**********************************************************************
    /* Output method implementations, primitive
    /**********************************************************************
     */

    @Override
    public void writeBoolean(boolean state) throws JacksonException
    {
        _verifyValueWrite("write boolean value");
        if (!_skipValue) {
            if (!_arraySeparator.isEmpty()) {
                _addToArray(state ? "true" : "false");
            } else {
                _writer.write(_columnIndex(), state);
            }
        }
    }

    @Override
    public void writeNull() throws JacksonException
    {
        _verifyValueWrite("write null value");

        if (!_skipValue) {
            if (!_arraySeparator.isEmpty()) {
                _addToArray(_schema.getNullValueOrEmpty());
            } else if (_tokenWriteContext.inObject()) {
                _writer.writeNull(_columnIndex());
            } else if (_tokenWriteContext.inArray()) {
                // [dataformat-csv#106]: Need to make sure we don't swallow nulls in arrays either
                // 04-Jan-2016, tatu: but check for case of array-wrapping, in which case null stands for absence
                //   of Object. In this case, could either add an empty row, or skip -- for now, we'll
                //   just skip; can change, if so desired, to expose "root null" as empty rows, possibly
                //   based on either schema property, or CsvGenerator.Feature.
                //  Note: if nulls are to be written that way, would need to call `finishRow()` right after `writeNull()`
                if (!_tokenWriteContext.getParent().inRoot()) {
                    _writer.writeNull(_columnIndex());
                }

                // ... so, for "root-level nulls" (with or without array-wrapping), we would do:
                /*
                _writer.writeNull(_columnIndex());
                finishRow();
*/
            }
        }
    }

    @Override
    public void writeNumber(short v) throws JacksonException {
        writeNumber((int) v);
    }

    @Override
    public void writeNumber(int v) throws JacksonException
    {
        _verifyValueWrite("write number");
        if (!_skipValue) {
            if (!_arraySeparator.isEmpty()) {
                _addToArray(String.valueOf(v));
            } else {
                _writer.write(_columnIndex(), v);
            }
        }
    }

    @Override
    public void writeNumber(long v) throws JacksonException
    {
        // First: maybe 32 bits is enough?
        if (v <= MAX_INT_AS_LONG && v >= MIN_INT_AS_LONG) {
            writeNumber((int) v);
            return;
        }
        _verifyValueWrite("write number");
        if (!_skipValue) {
            if (!_arraySeparator.isEmpty()) {
                _addToArray(String.valueOf(v));
            } else {
                _writer.write(_columnIndex(), v);
            }
        }
    }

    @Override
    public void writeNumber(BigInteger v) throws JacksonException
    {
        if (v == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        if (!_skipValue) {
            if (!_arraySeparator.isEmpty()) {
                _addToArray(String.valueOf(v));
            } else {
                _writer.write(_columnIndex(), v.toString());

            }
        }
    }
    
    @Override
    public void writeNumber(double v) throws JacksonException
    {
        _verifyValueWrite("write number");
        if (!_skipValue) {
            if (!_arraySeparator.isEmpty()) {
                _addToArray(String.valueOf(v));
            } else {
                _writer.write(_columnIndex(), v);
            }
        }
    }    

    @Override
    public void writeNumber(float v) throws JacksonException
    {
        _verifyValueWrite("write number");
        if (!_skipValue) {
            if (!_arraySeparator.isEmpty()) {
                _addToArray(String.valueOf(v));
            } else {
                _writer.write(_columnIndex(), v);
            }
        }
    }

    @Override
    public void writeNumber(BigDecimal v) throws JacksonException
    {
        if (v == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        if (!_skipValue) {
            String str = isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                    ? v.toPlainString() : v.toString();
            if (!_arraySeparator.isEmpty()) {
                _addToArray(String.valueOf(v));
            } else {
                _writer.write(_columnIndex(), str);
            }
        }
    }

    @Override
    public void writeNumber(String encodedValue) throws JacksonException
    {
        if (encodedValue == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        if (!_skipValue) {
            if (!_arraySeparator.isEmpty()) {
                _addToArray(encodedValue);
            } else {
                _writer.write(_columnIndex(), encodedValue);
            }
        }
    }
    
    /*
    /**********************************************************************
    /* Overrides for field methods
    /**********************************************************************
     */

    @Override
    public void writeOmittedField(String fieldName) throws JacksonException
    {
        // Hmmh. Should we require a match? Actually, let's use logic: if field found,
        // assumption is we must add a placeholder; if not, we can merely ignore
        CsvSchema.Column col = _schema.column(fieldName);
        if (col == null) {
            // assumed to have been removed from schema too
        } else {
            // basically combination of "writeFieldName()" and "writeNull()"
            if (!_tokenWriteContext.writeFieldName(fieldName)) {
                _reportError("Can not skip a field, expecting a value");
            }
            // and all we do is just note index to use for following value write
            _nextColumnByName = col.getIndex();
            // We can basically copy what 'writeNull()' does...
            _verifyValueWrite("skip positional value due to filtering");
            _writer.write(_columnIndex(), "");
        }
    }

    /*
    /**********************************************************************
    /* Implementations for methods from base class
    /**********************************************************************
     */
    
    @Override
    protected final void _verifyValueWrite(String typeMsg) throws JacksonException
    {
        if (!_tokenWriteContext.writeValue()) {
            _reportError("Can not "+typeMsg+", expecting field name");
        }
        if (_handleFirstLine) {
            _handleFirstLine();
        }
    }

    @Override
    protected void _releaseBuffers() {
        _writer._releaseBuffers();
    }

    /*
    /**********************************************************************
    /* Internal methods, error reporting
    /**********************************************************************
     */

    /**
     * Method called when there is a problem related to mapping data
     * (compared to a low-level generation); if so, should be surfaced
     * as 
     */
    protected void _reportMappingError(String msg) throws JsonProcessingException {
        throw CsvMappingException.from(this, msg, _schema);
//        throw new JsonGenerationException(msg, this);
    }

    /*
    /**********************************************************************
    /* Internal methods, other
    /**********************************************************************
     */

    protected final int _columnIndex()
    {
        int ix = _nextColumnByName;
        if (ix < 0) { // if we had one, remove now
            ix = _writer.nextColumnIndex();
        }
        return ix;
    }

    /**
     * Method called when the current row is complete; typically
     * will flush possibly buffered column values, append linefeed
     * and reset state appropriately.
     */
    protected void finishRow() throws JacksonException
    {
        _writer.endRow();
        _nextColumnByName = -1;
    }

    protected void _handleFirstLine() throws JacksonException
    {
        _handleFirstLine = false;
        if (_schema.usesHeader()) {
            int count = _schema.size();
            if (count == 0) { 
                _reportMappingError("Schema specified that header line is to be written; but contains no column names");
            }
            for (CsvSchema.Column column : _schema) {
                _writer.writeColumnName(column.getName());
            }
            _writer.endRow();
        }
    }

    protected void _addToArray(String value) {
        if (_arrayElements > 0) {
            _arrayContents.append(_arraySeparator);
        }
        ++_arrayElements;
        _arrayContents.append(value);
    }
    
    protected void _addToArray(char[] value) {
        if (_arrayElements > 0) {
            _arrayContents.append(_arraySeparator);
        }
        ++_arrayElements;
        _arrayContents.append(value);
    }
}
