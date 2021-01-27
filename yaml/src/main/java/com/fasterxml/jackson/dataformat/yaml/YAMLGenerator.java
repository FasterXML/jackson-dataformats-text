package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.DumpSettingsBuilder;
import org.snakeyaml.engine.v2.common.Anchor;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.common.SpecVersion;
import org.snakeyaml.engine.v2.emitter.Emitter;
import org.snakeyaml.engine.v2.events.AliasEvent;
import org.snakeyaml.engine.v2.events.DocumentEndEvent;
import org.snakeyaml.engine.v2.events.DocumentStartEvent;
import org.snakeyaml.engine.v2.events.Event;
import org.snakeyaml.engine.v2.events.ImplicitTuple;
import org.snakeyaml.engine.v2.events.MappingEndEvent;
import org.snakeyaml.engine.v2.events.MappingStartEvent;
import org.snakeyaml.engine.v2.events.ScalarEvent;
import org.snakeyaml.engine.v2.events.SequenceEndEvent;
import org.snakeyaml.engine.v2.events.SequenceStartEvent;
import org.snakeyaml.engine.v2.events.StreamEndEvent;
import org.snakeyaml.engine.v2.events.StreamStartEvent;
import org.snakeyaml.engine.v2.nodes.Tag;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.json.DupDetector;
import com.fasterxml.jackson.core.util.JacksonFeatureSet;
import com.fasterxml.jackson.core.util.SimpleStreamWriteContext;
import com.fasterxml.jackson.core.io.IOContext;

import com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker;

public class YAMLGenerator extends GeneratorBase
{
    /**
     * Enumeration that defines all togglable features for YAML generators
     */
    public enum Feature implements FormatFeature
    {
        /**
         * Whether we are to write an explicit document start marker ("---")
         * or not.
         */
        WRITE_DOC_START_MARKER(true),

        /**
         * Whether to use YAML native Object Id construct for indicating type (true);
         * or "generic" Object Id mechanism (false). Former works better for systems that
         * are YAML-centric; latter may be better choice for interoperability, when
         * converting between formats or accepting other formats.
         */
        USE_NATIVE_OBJECT_ID(true),

        /**
         * Whether to use YAML native Type Id construct for indicating type (true);
         * or "generic" type property (false). Former works better for systems that
         * are YAML-centric; latter may be better choice for interoperability, when
         * converting between formats or accepting other formats.
         */
        USE_NATIVE_TYPE_ID(true),

        /**
         * Do we try to force so-called canonical output or not.
         */
        CANONICAL_OUTPUT(false),

        /**
         * Options passed to SnakeYAML that determines whether longer textual content
         * gets automatically split into multiple lines or not.
         *<p>
         * Feature is enabled by default to conform to SnakeYAML defaults as well as
         * backwards compatibility with 2.5 and earlier versions.
         */
        SPLIT_LINES(true),

        /**
         * Whether strings will be rendered without quotes (true) or
         * with quotes (false, default).
         *<p>
         * Minimized quote usage makes for more human readable output; however, content is
         * limited to printable characters according to the rules of
         * <a href="http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal block style</a>.
         */
        MINIMIZE_QUOTES(false),

        /**
         * Whether numbers stored as strings will be rendered with quotes (true) or
         * without quotes (false, default) when MINIMIZE_QUOTES is enabled.
         *<p>
         * Minimized quote usage makes for more human readable output; however, content is
         * limited to printable characters according to the rules of
         * <a href="http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal block style</a>.
         */
        ALWAYS_QUOTE_NUMBERS_AS_STRINGS(false),

        /**
         * Whether for string containing newlines a <a href="http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal block style</a>
         * should be used. This automatically enabled when {@link #MINIMIZE_QUOTES} is set.
         * <p>
         * The content of such strings is limited to printable characters according to the rules of
         * <a href="http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal block style</a>.
         */
        LITERAL_BLOCK_STYLE(false),

        /**
         * Feature enabling of which adds indentation for array entry generation
         * (default indentation being 2 spaces).
         *<p>
         * Default value is {@code false} for backwards compatibility
         */
        INDENT_ARRAYS(false),

        /**
         * Feature enabling of which adds indentation with indicator for array entry generation
         * (default indentation being 2 spaces).
         *<p>
         * Default value is {@code false} for backwards compatibility
         */
        INDENT_ARRAYS_WITH_INDICATOR(false),
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
    protected final static Pattern PLAIN_NUMBER_P = Pattern.compile("-?[0-9]*(\\.[0-9]*)?");
    protected final static String TAG_BINARY = Tag.BINARY.toString();

    // for property names, leave out quotes
    private final static ScalarStyle STYLE_UNQUOTED_NAME = ScalarStyle.PLAIN;

    // numbers, booleans, should use implicit
    private final static ScalarStyle STYLE_SCALAR = ScalarStyle.PLAIN;
    // Strings quoted for fun
    private final static ScalarStyle STYLE_QUOTED = ScalarStyle.DOUBLE_QUOTED;
    // Strings in literal (block) style
    private final static ScalarStyle STYLE_LITERAL = ScalarStyle.LITERAL;

    // Which flow style to use for Base64? Maybe basic quoted?
    // 29-Nov-2017, tatu: Actually SnakeYAML uses block style so:
    private final static ScalarStyle STYLE_BASE64 = STYLE_LITERAL;

    private final static ScalarStyle STYLE_PLAIN = ScalarStyle.PLAIN;

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    final protected IOContext _ioContext;

    /**
     * Bit flag composed of bits that indicate which {@link YAMLGenerator.Feature}s
     * are enabled.
     */
    protected int _formatWriteFeatures;

    protected Writer _writer;

    protected DumpSettings _outputOptions;

    protected final boolean _cfgMinimizeQuotes;

    protected final SpecVersion _docVersion;

    /*
    /**********************************************************************
    /* Output state
    /**********************************************************************
     */

    /**
     * Object that keeps track of the current contextual state of the generator.
     */
    protected SimpleStreamWriteContext _streamWriteContext;

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
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */
    public YAMLGenerator(ObjectWriteContext writeContext, IOContext ioCtxt,
            int streamWriteFeatures, int yamlFeatures,
            StringQuotingChecker quotingChecker,
            Writer out,
            SpecVersion version)
    {
        super(writeContext, streamWriteFeatures);
        _ioContext = ioCtxt;
        final DupDetector dups = StreamWriteFeature.STRICT_DUPLICATE_DETECTION.enabledIn(streamWriteFeatures)
                ? DupDetector.rootDetector(this) : null;
        _streamWriteContext = SimpleStreamWriteContext.createRootContext(dups);

        _formatWriteFeatures = yamlFeatures;
        _cfgMinimizeQuotes = Feature.MINIMIZE_QUOTES.enabledIn(_formatWriteFeatures);
        _quotingChecker = quotingChecker;
        _writer = out;
        _docVersion = version;

        _outputOptions = buildDumperOptions(streamWriteFeatures, yamlFeatures, version);

        _emitter = new Emitter(_outputOptions, new WriterWrapper(_writer));
        // should we start output now, or try to defer?

        _emit(new StreamStartEvent());
        _emitStartDocument();
    }

    protected DumpSettings buildDumperOptions(int streamWriteFeatures, int yamlFeatures,
            SpecVersion version)
    {
        DumpSettingsBuilder opt = DumpSettings.builder();
        // would we want canonical?
        if (Feature.CANONICAL_OUTPUT.enabledIn(_formatWriteFeatures)) {
            opt.setCanonical(true);
        } else {
            opt.setCanonical(false);
            // if not, MUST specify flow styles
            opt.setDefaultFlowStyle(FlowStyle.BLOCK);
        }
        // split-lines for text blocks?
        opt.setSplitLines(Feature.SPLIT_LINES.enabledIn(_formatWriteFeatures));
        // array indentation?
        if (Feature.INDENT_ARRAYS.enabledIn(_formatWriteFeatures)) {
            // But, wrt [dataformats-text#34]: need to set both to diff values to work around bug
            // (otherwise indentation level is "invisible". Note that this should NOT be necessary
            // but is needed up to at least SnakeYAML 1.18.
            // Also looks like all kinds of values do work, except for both being 2... weird.
            opt.setIndicatorIndent(1);
            opt.setIndent(2);
        }
        // [dataformats-text#175]: further configurability that overrides prev setting
        if (Feature.INDENT_ARRAYS_WITH_INDICATOR.enabledIn(_formatWriteFeatures)) {
            opt.setIndicatorIndent(2);
            opt.setIndentWithIndicator(true);
        }

        // 03-Oct-2020, tatu: Specify spec version; however, does not seem to make
        //   any difference?
        opt.setYamlDirective(Optional.ofNullable(version));
        return opt.build();
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
    public final TokenStreamContext streamWriteContext() { return _streamWriteContext; }

    @Override
    public final Object currentValue() {
        return _streamWriteContext.currentValue();
    }

    @Override
    public final void assignCurrentValue(Object v) {
        _streamWriteContext.assignCurrentValue(v);
    }

    /*
    /**********************************************************************
    /* Overridden methods, configuration
    /**********************************************************************
     */

    @Override
    public Object streamWriteOutputTarget() {
        return _writer;
    }

    /**
     * SnakeYAML does not expose buffered content amount, so we can only return
     * <code>-1</code> from here
     */
    @Override
    public int streamWriteOutputBuffered() {
        return -1;
    }

    @Override
    public boolean canWriteFormattedNumbers() { return true; }

    @Override
    public JacksonFeatureSet<StreamWriteCapability> streamWriteCapabilities() {
        return DEFAULT_TEXTUAL_WRITE_CAPABILITIES;
    }

    /*
    /**********************************************************************
    /* Extended API, configuration
    /**********************************************************************
     */

    public YAMLGenerator enable(Feature f) {
        _formatWriteFeatures |= f.getMask();
        return this;
    }

    public YAMLGenerator disable(Feature f) {
        _formatWriteFeatures &= ~f.getMask();
        return this;
    }

    public final boolean isEnabled(Feature f) {
        return (_formatWriteFeatures & f.getMask()) != 0;
    }

    public YAMLGenerator configure(Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /*
    /**********************************************************************
    /* Overridden methods; writing property names
    /**********************************************************************
     */

    /* And then methods overridden to make final, streamline some
     * aspects...
     */

    @Override
    public final void writeName(String name) throws JacksonException
    {
        if (!_streamWriteContext.writeName(name)) {
            _reportError("Cannot write a property name, expecting a value");
        }
        _writeFieldName(name);
    }

    @Override
    public final void writeName(SerializableString name)
        throws JacksonException
    {
        // Object is a value, need to verify it's allowed
        if (!_streamWriteContext.writeName(name.getValue())) {
            _reportError("Cannotwrite a property name, expecting a value");
        }
        _writeFieldName(name.getValue());
    }

    @Override
    public void writePropertyId(long id) throws JacksonException {
        // 24-Jul-2019, tatu: Should not force construction of a String here...
        String idStr = Long.valueOf(id).toString(); // since instances for small values cached
        if (!_streamWriteContext.writeName(idStr)) {
            _reportError("Cannot write a property id, expecting a value");
        }
        // to avoid quoting
//        _writeFieldName(idStr);
        _writeScalar(idStr, "int", STYLE_SCALAR);
    }

    private final void _writeFieldName(String name) throws JacksonException
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
    public final void flush()
    {
        if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
            try {
                _writer.flush();
            } catch (IOException e) {
                throw _wrapIOFailure(e);
            }
        }
    }

    @Override
    public void close()
    {
        if (!isClosed()) {
            // 11-Dec-2019, tatu: Should perhaps check if content is to be auto-closed...
            //   but need END_DOCUMENT regardless
            _emitEndDocument();
            _emit(new StreamEndEvent());
            super.close();

            /* 25-Nov-2008, tatus: As per [JACKSON-16] we are not to call close()
             *   on the underlying Reader, unless we "own" it, or auto-closing
             *   feature is enabled.
             *   One downside: when using UTF8Writer, underlying buffer(s)
             *   may not be properly recycled if we don't close the writer.
             */
            if (_writer != null) {
                try {
                    if (_ioContext.isResourceManaged() || isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET)) {
                        _writer.close();
                    } else if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
                        // If we can't close it, we should at least flush
                        _writer.flush();
                    }
                } catch (IOException e) {
                    throw _wrapIOFailure(e);
                }
            }
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
        _streamWriteContext = _streamWriteContext.createChildArrayContext(null);
        FlowStyle style = _outputOptions.getDefaultFlowStyle();
        String yamlTag = _typeId;
        boolean implicit = (yamlTag == null);
        Optional<Anchor> anchor = Optional.ofNullable(_objectId).map(s -> new Anchor(s));
        if (anchor.isPresent()) {
            _objectId = null;
        }
        _emit(new SequenceStartEvent(anchor, Optional.ofNullable(yamlTag),
                implicit,  style));
    }

    @Override
    public final void writeStartArray(Object currValue) throws JacksonException {
        writeStartArray();
        assignCurrentValue(currValue);
    }

    @Override
    public final void writeEndArray() throws JacksonException
    {
        if (!_streamWriteContext.inArray()) {
            _reportError("Current context not Array but "+_streamWriteContext.typeDesc());
        }
        // just to make sure we don't "leak" type ids
        _typeId = null;
        _streamWriteContext = _streamWriteContext.getParent();
        _emit(new SequenceEndEvent());
    }

    @Override
    public final void writeStartObject() throws JacksonException
    {
        _verifyValueWrite("start an object");
        _streamWriteContext = _streamWriteContext.createChildObjectContext(null);
        FlowStyle style = _outputOptions.getDefaultFlowStyle();
        String yamlTag = _typeId;
        boolean implicit = (yamlTag == null);
        Optional<Anchor> anchor = Optional.ofNullable(_objectId).map(s -> new Anchor(s));
        if (anchor.isPresent()) {
            _objectId = null;
        }
        _emit(new MappingStartEvent(anchor, Optional.ofNullable(yamlTag), implicit,  style));
    }

    @Override
    public final void writeStartObject(Object currValue) throws JacksonException {
        writeStartObject();
        assignCurrentValue(currValue);
    }

    @Override
    public final void writeEndObject() throws JacksonException
    {
        if (!_streamWriteContext.inObject()) {
            _reportError("Current context not Object but "+_streamWriteContext.typeDesc());
        }
        // just to make sure we don't "leak" type ids
        _typeId = null;
        _streamWriteContext = _streamWriteContext.getParent();
        _emit(new MappingEndEvent());
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

        // [dataformats-text#50]: Empty String always quoted
        if (text.isEmpty()) {
            _writeScalar(text, "string", STYLE_QUOTED);
            return;
        }
        ScalarStyle style;
        if (_cfgMinimizeQuotes) {
            // If one of reserved values ("true", "null"), or, number, preserve quoting:
            if (_quotingChecker.needToQuoteValue(text)
                || (Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS.enabledIn(_formatWriteFeatures)
                        && PLAIN_NUMBER_P.matcher(text).matches())
                ) {
                style = STYLE_QUOTED;
            } else if (text.indexOf('\n') >= 0) {
                style = STYLE_LITERAL;
            } else {
                style = STYLE_PLAIN;
            }
        } else {
            if (Feature.LITERAL_BLOCK_STYLE.enabledIn(_formatWriteFeatures) && text.indexOf('\n') >= 0) {
                style = STYLE_LITERAL;
            } else {
                style = STYLE_QUOTED;
            }
        }
        _writeScalar(text, "string", style);
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws JacksonException
    {
        writeString(new String(text, offset, len));
    }

    @Override
    public final void writeString(SerializableString sstr)
        throws JacksonException
    {
        writeString(sstr.toString());
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int len)
        throws JacksonException
    {
        _reportUnsupportedOperation();
    }

    @Override
    public final void writeUTF8String(byte[] text, int offset, int len)
        throws JacksonException
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
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws JacksonException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws JacksonException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char c) throws JacksonException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text) throws JacksonException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws JacksonException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws JacksonException {
        _reportUnsupportedOperation();
    }

    /*
    /**********************************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************************
     */

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws JacksonException
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
    public void writeBoolean(boolean state) throws JacksonException
    {
        _verifyValueWrite("write boolean value");
        _writeScalar(state ? "true" : "false", "bool", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(short v) throws JacksonException {
        writeNumber((int) v);
    }

    @Override
    public void writeNumber(int v) throws JacksonException
    {
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(v), "int", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(long l) throws JacksonException
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
    public void writeNumber(BigInteger v) throws JacksonException
    {
        if (v == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(v.toString()), "java.math.BigInteger", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(double d) throws JacksonException
    {
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(d), "double", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(float f) throws JacksonException
    {
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(f), "float", STYLE_SCALAR);
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
        _writeScalar(str, "java.math.BigDecimal", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(String encodedValue) throws JacksonException
    {
        if (encodedValue == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writeScalar(encodedValue, "number", STYLE_SCALAR);
    }

    @Override
    public void writeNull() throws JacksonException
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
        return Feature.USE_NATIVE_OBJECT_ID.enabledIn(_formatWriteFeatures);
    }

    @Override
    public boolean canWriteTypeId() {
        // yes, YAML does support Native Type Ids!
        // 10-Sep-2014, tatu: Except as per [#22] might not want to...
        return Feature.USE_NATIVE_TYPE_ID.enabledIn(_formatWriteFeatures);
    }

    @Override
    public void writeTypeId(Object id)
        throws JacksonException
    {
        // should we verify there's no preceding type id?
        _typeId = String.valueOf(id);
    }

    @Override
    public void writeObjectRef(Object id)
        throws JacksonException
    {
        _verifyValueWrite("write Object reference");
        AliasEvent evt = new AliasEvent(Optional.of(String.valueOf(id)).map(s -> new Anchor(s)));
        _emit(evt);
    }

    @Override
    public void writeObjectId(Object id)
        throws JacksonException
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
    protected final void _verifyValueWrite(String typeMsg) throws JacksonException
    {
        if (!_streamWriteContext.writeValue()) {
            _reportError("Cannot "+typeMsg+", expecting a property name");
        }
        if (_streamWriteContext.inRoot()) {
            // Start-doc emitted when creating generator, but otherwise need it; similarly,
            // need matching end-document to close earlier open one
            if (_streamWriteContext.getCurrentIndex() > 0) {
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

    protected void _writeScalar(String value, String type, ScalarStyle style) throws JacksonException
    {
        _emit(_scalarEvent(value, style));
    }

    private void _writeScalarBinary(Base64Variant b64variant,
            byte[] data) throws JacksonException
    {
        // 15-Dec-2017, tatu: as per [dataformats-text#62], can not use SnakeYAML's internal
        //    codec. Also: force use of linefeed variant if using default
        if (b64variant == Base64Variants.getDefaultVariant()) {
            b64variant = Base64Variants.MIME;
        }
        final String lf = _lf();
        String encoded = b64variant.encode(data, false, lf);
        _emit(new ScalarEvent(Optional.empty(), Optional.ofNullable(TAG_BINARY), EXPLICIT_TAGS, encoded, STYLE_BASE64));
    }

    protected ScalarEvent _scalarEvent(String value, ScalarStyle style)
    {
        String yamlTag = _typeId;
        if (yamlTag != null) {
            _typeId = null;
        }
        Optional<Anchor> anchor = Optional.ofNullable(_objectId).map(s -> new Anchor(s));
        if (anchor.isPresent()) {
            _objectId = null;
        }
        // 29-Nov-2017, tatu: Not 100% sure why we don't force explicit tags for
        //    type id, but trying to do so seems to double up tag output...
        return new ScalarEvent(anchor, Optional.ofNullable(yamlTag), NO_TAGS, value, style);
    }

    protected String _lf() {
        return _outputOptions.getBestLineBreak();
    }

    protected void _emitStartDocument() throws JacksonException
    {
        Map<String,String> noTags = Collections.emptyMap();
        boolean startMarker = Feature.WRITE_DOC_START_MARKER.enabledIn(_formatWriteFeatures);
        _emit(new DocumentStartEvent(startMarker, _outputOptions.getYamlDirective(),
                 // for 1.10 was: ((version == null) ? null : version.getArray()),
                noTags));
    }

    protected void _emitEndDocument() throws JacksonException {
        _emit(new DocumentEndEvent(false));
    }

    protected final void _emit(Event e) {
        _emitter.emit(e);
    }
}
