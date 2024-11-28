package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.events.*;
import org.yaml.snakeyaml.nodes.Tag;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.core.util.JacksonFeatureSet;
import com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker;
import com.fasterxml.jackson.core.io.IOContext;

public class YAMLGenerator extends GeneratorBase
{
    /**
     * Enumeration that defines all togglable features for YAML generators
     */
    public enum Feature implements FormatFeature // since 2.9
    {
        /**
         * Whether we are to write an explicit document start marker ("---")
         * or not.
         *
         * @since 2.3
         */
        WRITE_DOC_START_MARKER(true),

        /**
         * Whether to use YAML native Object Id construct for indicating type (true);
         * or "generic" Object Id mechanism (false). Former works better for systems that
         * are YAML-centric; latter may be better choice for interoperability, when
         * converting between formats or accepting other formats.
         *
         * @since 2.5
         */
        USE_NATIVE_OBJECT_ID(true),

        /**
         * Whether to use YAML native Type Id construct for indicating type (true);
         * or "generic" type property (false). Former works better for systems that
         * are YAML-centric; latter may be better choice for interoperability, when
         * converting between formats or accepting other formats.
         *
         * @since 2.5
         */
        USE_NATIVE_TYPE_ID(true),

        /**
         * Do we try to force so-called canonical output or not.
         * <p>
         *     Ignored if you provide your own {@code DumperOptions}.
         * </p>
         */
        CANONICAL_OUTPUT(false),

        /**
         * Options passed to SnakeYAML that determines whether longer textual content
         * gets automatically split into multiple lines or not.
         * <p>
         *     Feature is enabled by default to conform to SnakeYAML defaults as well as
         *     backwards compatibility with 2.5 and earlier versions.
         * </p>
         * <p>
         *     Ignored if you provide your own {@code DumperOptions}.
         * </p>
         *
         * @since 2.6
         */
        SPLIT_LINES(true),

        /**
         * Whether strings will be rendered without quotes (true) or
         * with quotes (false, default).
         * <p>
         *     Minimized quote usage makes for more human readable output; however, content is
         *     limited to printable characters according to the rules of
         *     <a href="http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal block style</a>.
         * </p>
         *
         * @since 2.7
         */
        MINIMIZE_QUOTES(false),

        /**
         * Whether numbers stored as strings will be rendered with quotes (true) or
         * without quotes (false, default) when MINIMIZE_QUOTES is enabled.
         * <p>
         *     Minimized quote usage makes for more human readable output; however, content is
         *     limited to printable characters according to the rules of
         *     <a href="http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal block style</a>.
         * </p>
         *
         * @since 2.8.2
         */
        ALWAYS_QUOTE_NUMBERS_AS_STRINGS(false),

        /**
         * Whether for string containing newlines a
         * <a href="http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal block style</a>
         * should be used. This automatically enabled when {@link #MINIMIZE_QUOTES} is set.
         * <p>
         *     The content of such strings is limited to printable characters according to the rules of
         *     <a href="http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal block style</a>.
         * </p>
         *
         * @since 2.9
         */
        LITERAL_BLOCK_STYLE(false),

        /**
         * Feature enabling of which adds indentation for array entry generation
         * (default indentation being 2 spaces).
         * <p>
         *     Default value is {@code false} for backwards compatibility
         * </p>
         * <p>
         *     Ignored if you provide your own {@code DumperOptions}.
         * </p>
         *
         * @since 2.9
         */
        INDENT_ARRAYS(false),
        /**
         * Feature enabling of which adds indentation with indicator for array entry generation
         * (default indentation being 2 spaces).
         * <p>
         *     Default value is {@code false} for backwards compatibility
         * </p>
         * <p>
         *     Ignored if you provide your own {@code DumperOptions}.
         * </p>
         *
         * @since 2.12
         */
        INDENT_ARRAYS_WITH_INDICATOR(false),

        /**
         * Option passed to SnakeYAML that determines if the line breaks used for
         * serialization should be same as what the default is for current platform.
         * If disabled, Unix linefeed ({@code \n}) will be used.
         * <p>
         *     Default value is {@code false} for backwards compatibility
         * </p>
         * <p>
         *     Ignored if you provide your own {@code DumperOptions}.
         * </p>
         *
         * @since 2.9.6
         */
        USE_PLATFORM_LINE_BREAKS(false),

        /**
         * Option passed to SnakeYAML to allows writing key longer that 128 characters
         * (up to 1024 characters).
         * If disabled, the max key length is left as 128 characters: longer names
         * are truncated. If enabled, limit is raised to 1024 characters.
         * <p>
         *     Default value is {@code false} for backwards-compatibility (same as behavior
         *     before this feature was added).
         * </p>
         * <p>
         *     Ignored if you provide your own {@code DumperOptions}.
         * </p>
         * 
         * @since 2.14
         */
        ALLOW_LONG_KEYS(false),
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
        public boolean enabledByDefault() { return _defaultState; }
        @Override
        public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
        @Override
        public int getMask() { return _mask; }
    }

    /*
    /**********************************************************************
    /* Internal constants
    /**********************************************************************
     */

    protected final static long MIN_INT_AS_LONG = (long) Integer.MIN_VALUE;
    protected final static long MAX_INT_AS_LONG = (long) Integer.MAX_VALUE;
    protected final static Pattern PLAIN_NUMBER_P = Pattern.compile("[+-]?[0-9]*(\\.[0-9]*)?");
    protected final static String TAG_BINARY = Tag.BINARY.toString();

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * @since 2.16
     */
    protected final StreamWriteConstraints _streamWriteConstraints;

    /**
     * Bit flag composed of bits that indicate which
     * {@link YAMLGenerator.Feature}s
     * are enabled.
     */
    protected int _formatFeatures;

    protected Writer _writer;

    protected DumperOptions _outputOptions;

    protected final org.yaml.snakeyaml.DumperOptions.Version _docVersion;

    // for field names, leave out quotes
    private final static DumperOptions.ScalarStyle STYLE_UNQUOTED_NAME = DumperOptions.ScalarStyle.PLAIN;

    // numbers, booleans, should use implicit
    private final static DumperOptions.ScalarStyle STYLE_SCALAR = DumperOptions.ScalarStyle.PLAIN;
    // Strings quoted for fun
    private final static DumperOptions.ScalarStyle STYLE_QUOTED = DumperOptions.ScalarStyle.DOUBLE_QUOTED;
    // Strings in literal (block) style
    private final static DumperOptions.ScalarStyle STYLE_LITERAL = DumperOptions.ScalarStyle.LITERAL;

    // Which flow style to use for Base64? Maybe basic quoted?
    // 29-Nov-2017, tatu: Actually SnakeYAML uses block style so:
    private final static DumperOptions.ScalarStyle STYLE_BASE64 = STYLE_LITERAL;

    private final static DumperOptions.ScalarStyle STYLE_PLAIN = DumperOptions.ScalarStyle.PLAIN;

    /*
    /**********************************************************************
    /* Output state
    /**********************************************************************
     */

    protected Emitter _emitter;

    /**
     * YAML supports native Object identifiers, so databinder may indicate
     * need to output one.
     */
    protected String _objectId;

    /**
     * YAML supports native Type identifiers, so databinder may indicate
     * need to output one.
     */
    protected String _typeId;

    protected int _rootValueCount;

    protected final StringQuotingChecker _quotingChecker;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public YAMLGenerator(IOContext ctxt, int jsonFeatures, int yamlFeatures,
            StringQuotingChecker quotingChecker,
            ObjectCodec codec, Writer out,
            org.yaml.snakeyaml.DumperOptions.Version version)
        throws IOException
    {
        super(jsonFeatures, codec, ctxt);
        _streamWriteConstraints = ctxt.streamWriteConstraints();
        _formatFeatures = yamlFeatures;
        _quotingChecker = (quotingChecker == null)
                ? StringQuotingChecker.Default.instance() : quotingChecker;
        _writer = out;
        _docVersion = version;

        _outputOptions = buildDumperOptions(jsonFeatures, yamlFeatures, version);

        _emitter = new Emitter(_writer, _outputOptions);
        // should we start output now, or try to defer?
        _emit(new StreamStartEvent(null, null));
        _emitStartDocument();
    }

    /**
     * @since 2.14
     */
    public YAMLGenerator(IOContext ctxt, int jsonFeatures, int yamlFeatures,
                         StringQuotingChecker quotingChecker,
                         ObjectCodec codec, Writer out,
                         org.yaml.snakeyaml.DumperOptions dumperOptions)
            throws IOException
    {
        super(jsonFeatures, codec, ctxt);
        _streamWriteConstraints = ctxt.streamWriteConstraints();
        _formatFeatures = yamlFeatures;
        _quotingChecker = (quotingChecker == null)
                ? StringQuotingChecker.Default.instance() : quotingChecker;
        _writer = out;
        _docVersion = dumperOptions.getVersion();
        _outputOptions = dumperOptions;

        _emitter = new Emitter(_writer, _outputOptions);
        // should we start output now, or try to defer?
        _emit(new StreamStartEvent(null, null));
        _emitStartDocument();
    }

    @Deprecated // since 2.12
    public YAMLGenerator(IOContext ctxt, int jsonFeatures, int yamlFeatures,
            ObjectCodec codec, Writer out,
            org.yaml.snakeyaml.DumperOptions.Version version) throws IOException {
        this(ctxt, jsonFeatures, yamlFeatures, null,
                codec, out, version);
    }

    protected DumperOptions buildDumperOptions(int jsonFeatures, int yamlFeatures,
            org.yaml.snakeyaml.DumperOptions.Version version)
    {
        DumperOptions opt = new DumperOptions();
        // would we want canonical?
        if (Feature.CANONICAL_OUTPUT.enabledIn(_formatFeatures)) {
            opt.setCanonical(true);
        } else {
            opt.setCanonical(false);
            // if not, MUST specify flow styles
            opt.setDefaultFlowStyle(FlowStyle.BLOCK);
        }
        // split-lines for text blocks?
        opt.setSplitLines(Feature.SPLIT_LINES.enabledIn(_formatFeatures));
        // array indentation?
        if (Feature.INDENT_ARRAYS.enabledIn(_formatFeatures)) {
            // But, wrt [dataformats-text#34]: need to set both to diff values to work around bug
            // (otherwise indentation level is "invisible". Note that this should NOT be necessary
            // but is needed up to at least SnakeYAML 1.18.
            // Also looks like all kinds of values do work, except for both being 2... weird.
            opt.setIndicatorIndent(1);
            opt.setIndent(2);
        }
        // [dataformats-text#175]: further configurability that overrides prev setting
        if (Feature.INDENT_ARRAYS_WITH_INDICATOR.enabledIn(_formatFeatures)) {
            opt.setIndicatorIndent(2);
            opt.setIndentWithIndicator(true);
        }
        // 14-May-2018: [dataformats-text#84] allow use of platform linefeed
        if (Feature.USE_PLATFORM_LINE_BREAKS.enabledIn(_formatFeatures)) {
            opt.setLineBreak(DumperOptions.LineBreak.getPlatformLineBreak());
        }

        if (Feature.ALLOW_LONG_KEYS.enabledIn(_formatFeatures)) {
            opt.setMaxSimpleKeyLength(1024);
        }
        return opt;
    }

    @Override
    public StreamWriteConstraints streamWriteConstraints() {
        return _streamWriteConstraints;
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

    /**
     * Not sure what to do here; could reset indentation to some value maybe?
     */
    @Override
    public YAMLGenerator useDefaultPrettyPrinter()
    {
        return this;
    }

    /**
     * Not sure what to do here; will always indent, but uses
     * YAML-specific settings etc.
     */
    @Override
    public YAMLGenerator setPrettyPrinter(PrettyPrinter pp) {
        return this;
    }

    @Override
    public Object getOutputTarget() {
        return _writer;
    }

    /**
     * SnakeYAML does not expose buffered content amount, so we can only return
     * <code>-1</code> from here
     */
    @Override
    public int getOutputBuffered() {
        return -1;
    }

    @Override
    public int getFormatFeatures() {
        return _formatFeatures;
    }

    @Override
    public JsonGenerator overrideFormatFeatures(int values, int mask) {
        // 14-Mar-2016, tatu: Should re-configure, but unfortunately most
        //    settings passed via options passed to constructor of Emitter
        _formatFeatures = (_formatFeatures & ~mask) | (values & mask);
        return this;
    }

    @Override
    public boolean canUseSchema(FormatSchema schema) {
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
    /* Extended API, configuration
    /**********************************************************************
     */

    public YAMLGenerator enable(YAMLGenerator.Feature f) {
        _formatFeatures |= f.getMask();
        return this;
    }

    public YAMLGenerator disable(YAMLGenerator.Feature f) {
        _formatFeatures &= ~f.getMask();
        return this;
    }

    public final boolean isEnabled(YAMLGenerator.Feature f) {
        return (_formatFeatures & f.getMask()) != 0;
    }

    public YAMLGenerator configure(YAMLGenerator.Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
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
    public final void writeFieldName(String name) throws IOException
    {
        if (_writeContext.writeFieldName(name) == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(name);
    }

    @Override
    public final void writeFieldName(SerializableString name)
        throws IOException
    {
        // Object is a value, need to verify it's allowed
        if (_writeContext.writeFieldName(name.getValue()) == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(name.getValue());
    }

    @Override // override since 2.10 (method added in 2.8)
    public void writeFieldId(long id) throws IOException {
        // 24-Jul-2019, tatu: Should not force construction of a String here...
        String idStr = Long.valueOf(id).toString(); // since instances for small values cached
        if (_writeContext.writeFieldName(idStr) == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field id, expecting a value");
        }
        // to avoid quoting
//        _writeFieldName(idStr);
        _writeScalar(idStr, "int", STYLE_SCALAR);
    }

    private final void _writeFieldName(String name) throws IOException
    {
        _writeScalar(name, "string",
                _quotingChecker.needToQuoteName(name) ? STYLE_QUOTED : STYLE_UNQUOTED_NAME);
    }

    /*
    /**********************************************************************
    /* Public API: low-level I/O
    /**********************************************************************
     */

    @Override
    public final void flush() throws IOException
    {
        if (isEnabled(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)) {
            _writer.flush();
        }
    }

    @Override
    public void close() throws IOException
    {
        if (!isClosed()) {
            // 11-Dec-2019, tatu: Should perhaps check if content is to be auto-closed...
            //   but need END_DOCUMENT regardless

            _emitEndDocument();
            _emit(new StreamEndEvent(null, null));

            /* 25-Nov-2008, tatus: As per [JACKSON-16] we are not to call close()
             *   on the underlying Reader, unless we "own" it, or auto-closing
             *   feature is enabled.
             *   One downside: when using UTF8Writer, underlying buffer(s)
             *   may not be properly recycled if we don't close the writer.
             */
            if (_writer != null) {
                if (_ioContext.isResourceManaged() || isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET)) {
                    _writer.close();
                } else if (isEnabled(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)) {
                    // If we can't close it, we should at least flush
                    _writer.flush();
                }
            }
            super.close();
        }
    }

    /*
    /**********************************************************************
    /* Public API: structural output
    /**********************************************************************
     */

    @Override
    public final void writeStartArray() throws IOException
    {
        _verifyValueWrite("start an array");
        _writeContext = _writeContext.createChildArrayContext();
        _streamWriteConstraints.validateNestingDepth(_writeContext.getNestingDepth());
        FlowStyle style = _outputOptions.getDefaultFlowStyle();
        String yamlTag = _typeId;
        boolean implicit = (yamlTag == null);
        String anchor = _objectId;
        if (anchor != null) {
            _objectId = null;
        }
        _emit(new SequenceStartEvent(anchor, yamlTag,
                implicit,  null, null, style));
    }

    @Override
    public final void writeEndArray() throws IOException
    {
        if (!_writeContext.inArray()) {
            _reportError("Current context not Array but "+_writeContext.typeDesc());
        }
        // just to make sure we don't "leak" type ids
        _typeId = null;
        _writeContext = _writeContext.getParent();
        _emit(new SequenceEndEvent(null, null));
    }

    @Override
    public final void writeStartObject() throws IOException
    {
        _verifyValueWrite("start an object");
        _writeContext = _writeContext.createChildObjectContext();
        _streamWriteConstraints.validateNestingDepth(_writeContext.getNestingDepth());
        FlowStyle style = _outputOptions.getDefaultFlowStyle();
        String yamlTag = _typeId;
        boolean implicit = (yamlTag == null);
        String anchor = _objectId;
        if (anchor != null) {
            _objectId = null;
        }
        _emit(new MappingStartEvent(anchor, yamlTag,
                implicit, null, null, style));
    }

    @Override
    public final void writeEndObject() throws IOException
    {
        if (!_writeContext.inObject()) {
            _reportError("Current context not Object but "+_writeContext.typeDesc());
        }
        // just to make sure we don't "leak" type ids
        _typeId = null;
        _writeContext = _writeContext.getParent();
        _emit(new MappingEndEvent(null, null));
    }

    /*
    /**********************************************************************
    /* Output method implementations, textual
    /**********************************************************************
     */

    @Override
    public void writeString(String text) throws IOException,JsonGenerationException
    {
        if (text == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write String value");

        // [dataformats-text#50]: Empty String always quoted
        if (text.isEmpty()) {
            _writeScalar(text, "string", STYLE_QUOTED);
            return;
        }
        DumperOptions.ScalarStyle style;
        if (Feature.MINIMIZE_QUOTES.enabledIn(_formatFeatures)) {
            if (text.indexOf('\n') >= 0) {
                style = STYLE_LITERAL;
            // If one of reserved values ("true", "null"), or, number, preserve quoting:
            } else if (_quotingChecker.needToQuoteValue(text)
                || (Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS.enabledIn(_formatFeatures)
                        && PLAIN_NUMBER_P.matcher(text).matches())
                ) {
                style = STYLE_QUOTED;
            } else {
                style = STYLE_PLAIN;
            }
        } else {
            if (Feature.LITERAL_BLOCK_STYLE.enabledIn(_formatFeatures)
                    && text.indexOf('\n') >= 0) {
                style = STYLE_LITERAL;
            } else {
                style = STYLE_QUOTED;
            }
        }
        _writeScalar(text, "string", style);
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException
    {
        writeString(new String(text, offset, len));
    }

    @Override
    public final void writeString(SerializableString sstr)
        throws IOException
    {
        writeString(sstr.toString());
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int len)
        throws IOException
    {
        _reportUnsupportedOperation();
    }

    @Override
    public final void writeUTF8String(byte[] text, int offset, int len)
        throws IOException
    {
        writeString(new String(text, offset, len, "UTF-8"));
    }

    /*
    /**********************************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************************
     */

    @Override
    public void writeRaw(String text) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char c) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    /*
    /**********************************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************************
     */

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException
    {
        if (data == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write Binary value");
        if (offset > 0 || (offset+len) != data.length) {
            data = Arrays.copyOfRange(data, offset, offset+len);
        }
        _writeScalarBinary(b64variant, data);
    }

    /*
    /**********************************************************************
    /* Output method implementations, scalars
    /**********************************************************************
     */

    @Override
    public void writeBoolean(boolean state) throws IOException
    {
        _verifyValueWrite("write boolean value");
        _writeScalar(state ? "true" : "false", "bool", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(int i) throws IOException
    {
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(i), "int", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(long l) throws IOException
    {
        // First: maybe 32 bits is enough?
        if (l <= MAX_INT_AS_LONG && l >= MIN_INT_AS_LONG) {
            writeNumber((int) l);
            return;
        }
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(l), "long", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException
    {
        if (v == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(v.toString()), "java.math.BigInteger", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(double d) throws IOException
    {
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(d), "double", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(float f) throws IOException
    {
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(f), "float", STYLE_SCALAR);
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
        _writeScalar(str, "java.math.BigDecimal", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException,JsonGenerationException, UnsupportedOperationException
    {
        if (encodedValue == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writeScalar(encodedValue, "number", STYLE_SCALAR);
    }

    @Override
    public void writeNull() throws IOException
    {
        _verifyValueWrite("write null value");
        // no real type for this, is there?
        _writeScalar("null", "object", STYLE_SCALAR);
    }

    /*
    /**********************************************************************
    /* Public API, write methods, Native Ids
    /**********************************************************************
     */

    @Override
    public boolean canWriteObjectId() {
        // yes, YAML does support Native Type Ids!
        // 10-Sep-2014, tatu: Except as per [#23] might not want to...
        return Feature.USE_NATIVE_OBJECT_ID.enabledIn(_formatFeatures);
    }

    @Override
    public boolean canWriteTypeId() {
        // yes, YAML does support Native Type Ids!
        // 10-Sep-2014, tatu: Except as per [#22] might not want to...
        return Feature.USE_NATIVE_TYPE_ID.enabledIn(_formatFeatures);
    }

    @Override
    public void writeTypeId(Object id)
        throws IOException
    {
        // should we verify there's no preceding type id?
        _typeId = String.valueOf(id);
    }

    @Override
    public void writeObjectRef(Object id)
        throws IOException
    {
        _verifyValueWrite("write Object reference");
        AliasEvent evt = new AliasEvent(String.valueOf(id), null, null);
        _emit(evt);
    }

    @Override
    public void writeObjectId(Object id)
        throws IOException
    {
        // should we verify there's no preceding id?
        _objectId = (id == null) ? null : String.valueOf(id);
    }

    /*
    /**********************************************************************
    /* Implementations for methods from base class
    /**********************************************************************
     */

    @Override
    protected final void _verifyValueWrite(String typeMsg)
        throws IOException
    {
        int status = _writeContext.writeValue();
        if (status == JsonWriteContext.STATUS_EXPECT_NAME) {
            _reportError("Can not "+typeMsg+", expecting field name");
        }
        if (_writeContext.inRoot()) {
            // Start-doc emitted when creating generator, but otherwise need it; similarly,
            // need matching end-document to close earlier open one
            if (_writeContext.getCurrentIndex() > 0) {
                _emitEndDocument();
                _emitStartDocument();
            }
        }

    }

    @Override
    protected void _releaseBuffers() {
        // nothing special to do...
    }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    // Implicit means that (type) tags won't be shown, right?
    private final static ImplicitTuple NO_TAGS = new ImplicitTuple(true, true);

    // ... and sometimes we specifically DO want explicit tag:
    private final static ImplicitTuple EXPLICIT_TAGS = new ImplicitTuple(false, false);

    protected void _writeScalar(String value, String type, DumperOptions.ScalarStyle style) throws IOException
    {
        _emit(_scalarEvent(value, style));
    }

    private void _writeScalarBinary(Base64Variant b64variant,
            byte[] data) throws IOException
    {
        // 15-Dec-2017, tatu: as per [dataformats-text#62], can not use SnakeYAML's internal
        //    codec. Also: force use of linefeed variant if using default
        if (b64variant == Base64Variants.getDefaultVariant()) {
            b64variant = Base64Variants.MIME;
        }
        String encoded = b64variant.encode(data, false, _lf());
        _emit(new ScalarEvent(null, TAG_BINARY, EXPLICIT_TAGS, encoded,
                null, null, STYLE_BASE64));
    }

    protected ScalarEvent _scalarEvent(String value, DumperOptions.ScalarStyle style)
    {
        String yamlTag = _typeId;
        if (yamlTag != null) {
            _typeId = null;
        }
        String anchor = _objectId;
        if (anchor != null) {
            _objectId = null;
        }
        // 29-Nov-2017, tatu: Not 100% sure why we don't force explicit tags for
        //    type id, but trying to do so seems to double up tag output...
        return new ScalarEvent(anchor, yamlTag, NO_TAGS, value,
                null, null, style);
    }

    protected String _lf() {
        return _outputOptions.getLineBreak().getString();
    }

    // @since 2.10.2
    protected void _emitStartDocument() throws IOException
    {
        Map<String,String> noTags = Collections.emptyMap();
        boolean startMarker = Feature.WRITE_DOC_START_MARKER.enabledIn(_formatFeatures);
        _emit(new DocumentStartEvent(null, null, startMarker,
                _docVersion, // for 1.10 was: ((version == null) ? null : version.getArray()),
                noTags));
    }

    // @since 2.10.2
    protected void _emitEndDocument() throws IOException {
        _emit(new DocumentEndEvent(null, null, false));
    }

    // @since 2.10.2
    protected final void _emit(Event e) throws IOException {
        _emitter.emit(e);
    }
}
