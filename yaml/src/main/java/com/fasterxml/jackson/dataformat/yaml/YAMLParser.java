package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.Anchor;
import org.snakeyaml.engine.v2.events.AliasEvent;
import org.snakeyaml.engine.v2.events.CollectionStartEvent;
import org.snakeyaml.engine.v2.events.Event;
import org.snakeyaml.engine.v2.events.MappingStartEvent;
import org.snakeyaml.engine.v2.events.NodeEvent;
import org.snakeyaml.engine.v2.events.ScalarEvent;
import org.snakeyaml.engine.v2.exceptions.Mark;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.parser.ParserImpl;
import org.snakeyaml.engine.v2.resolver.JsonScalarResolver;
import org.snakeyaml.engine.v2.resolver.ScalarResolver;
import org.snakeyaml.engine.v2.scanner.StreamReader;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.DupDetector;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.JacksonFeatureSet;
import com.fasterxml.jackson.core.util.SimpleTokenReadContext;

/**
 * {@link JsonParser} implementation used to expose YAML documents
 * in form that allows other Jackson functionality to process YAML content,
 * such as binding POJOs to and from it, and building tree representations.
 */
public class YAMLParser extends ParserBase
{
    /**
     * Enumeration that defines all togglable features for YAML parsers.
     */
    public enum Feature implements FormatFeature
    {
        /**
         * Feature that determines whether an empty {@link String} will be parsed
         * as {@code null}. Logic is part of YAML 1.1 
         * <a href="https://yaml.org/type/null.html">Null Language-Independent Type</a>.
         *<p>
         * Feature is enabled by default in Jackson 2.12 for backwards-compatibility
         * reasons.
         */
        EMPTY_STRING_AS_NULL(true)
        ;

        final boolean _defaultState;
        final int _mask;

        // Method that calculates bit set (flags) of all features that
        // are enabled by default.
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

    // note: does NOT include '0', handled separately
//    private final static Pattern PATTERN_INT = Pattern.compile("-?[1-9][0-9]*");

    // 22-Nov-2020, tatu: Not needed as of 2.12 since SnakeYAML tags
    //    doubles correctly
//    private final static Pattern PATTERN_FLOAT = Pattern.compile(
//            "[-+]?([0-9][0-9_]*)?\\.[0-9]*([eE][-+][0-9]+)?");
    
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected final int _formatFeatures;

    // @since 2.12
    protected final boolean _cfgEmptyStringsToNull;

    /*
    /**********************************************************************
    /* Input sources
    /**********************************************************************
     */

    /**
     * Need to keep track of underlying {@link Reader} to be able to
     * auto-close it (if required to)
     */
    protected final Reader _reader;

    protected final ParserImpl _yamlParser;
    protected final ScalarResolver _yamlResolver = new JsonScalarResolver();

    /*
    /**********************************************************************
    /* State
    /**********************************************************************
     */

    /**
     * Information about parser context, context in which
     * the next token is to be parsed (root, array, object).
     */
    protected SimpleTokenReadContext _parsingContext;

    /**
     * Keep track of the last event read, to get access to Location info
     */
    protected Event _lastEvent;

    /**
     * To keep track of tags ("type ids"), need to either get tags for all
     * events, or, keep tag of relevant event that might have it: this is
     * different from {@code _lastEvent} in some cases.
     *
     * @since 2.12
     */
    protected Event _lastTagEvent;

    /**
     * We need to keep track of text values.
     */
    protected String _textValue;

    /**
     * For some tokens (specifically, numbers), we'll have cleaned up version,
     * mostly free of underscores
     */
    protected String _cleanedTextValue;

    /**
     * Let's also have a local copy of the current field name
     */
    protected String _currentFieldName;

    /**
     * Flag that is set when current token was derived from an Alias
     * (reference to another value's anchor)
     */
    protected boolean _currentIsAlias;

    /**
     * Anchor for the value that parser currently points to: in case of
     * structured types, value whose first token current token is.
     */
    protected Optional<Anchor> _currentAnchor;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public YAMLParser(ObjectReadContext readCtxt, IOContext ioCtxt, BufferRecycler br,
            int streamReadFeatures, int formatFeatures, Reader reader)
    {
        super(readCtxt, ioCtxt, streamReadFeatures);
        _formatFeatures = formatFeatures;
        _reader = reader;
        LoadSettings settings = LoadSettings.builder().build();//TODO use parserFeatures
        _yamlParser = new ParserImpl(new StreamReader(reader, settings), settings);
        _cfgEmptyStringsToNull = Feature.EMPTY_STRING_AS_NULL.enabledIn(formatFeatures);
        DupDetector dups = StreamReadFeature.STRICT_DUPLICATE_DETECTION.enabledIn(streamReadFeatures)
                ? DupDetector.rootDetector(this) : null;
        _parsingContext = SimpleTokenReadContext.createRootContext(dups);
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
    /* Extended YAML-specific API
    /**********************************************************************
     */

    /**
     * Method that can be used to check whether current token was
     * created from YAML Alias token (reference to an anchor).
     */
    public boolean isCurrentAlias() {
        return _currentIsAlias;
    }

    /*
    /**********************************************************************
    /* Overrides: capability introspection methods
    /**********************************************************************
     */

    @Override
    public boolean canReadObjectId() { // yup
        return true;
    }

    @Override
    public boolean canReadTypeId() {
        return true; // yes, YAML got 'em
    }

    @Override
    public JacksonFeatureSet<StreamReadCapability> getReadCapabilities() {
        // Defaults are fine; YAML actually has typed scalars (to a degree)
        // unlike CSV, Properties and XML
        return DEFAULT_READ_CAPABILITIES;
    }

    /*
    /**********************************************************************
    /* ParserBase method impls
    /**********************************************************************
     */

    @Override
    public Reader getInputSource() {
        return _reader;
    }

    @Override
    protected void _closeInput() throws IOException {
        /* 25-Nov-2008, tatus: As per [JACKSON-16] we are not to call close()
         *   on the underlying Reader, unless we "own" it, or auto-closing
         *   feature is enabled.
         *   One downside is that when using our optimized
         *   Reader (granted, we only do that for UTF-32...) this
         *   means that buffer recycling won't work correctly.
         */
        if (_ioContext.isResourceManaged() || isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE)) {
            _reader.close();
        }
    }

    @Override public TokenStreamContext getParsingContext() { return _parsingContext; }
    @Override public void setCurrentValue(Object v) { _parsingContext.setCurrentValue(v); }
    @Override public Object getCurrentValue() { return _parsingContext.getCurrentValue(); }

    /*
    /**********************************************************************
    /* FormatFeature support (none yet)
    /**********************************************************************

    /*
    /**********************************************************************
    /* Public API, configuration
    /**********************************************************************
     */

    /**
     * Method for checking whether specified CSV {@link Feature}
     * is enabled.
     */
    public boolean isEnabled(YAMLParser.Feature f) {
        return (_formatFeatures & f.getMask()) != 0;
    }

//    @Override public XxxSchema getSchema() 

    /*
    /**********************************************************************
    /* Location info
    /**********************************************************************
     */

    @Override
    public JsonLocation getTokenLocation()
    {
        if (_lastEvent == null) {
            return JsonLocation.NA;
        }
        return _locationFor(_lastEvent.getStartMark());
    }

    @Override
    public JsonLocation getCurrentLocation() {
        // can assume we are at the end of token now...
        if (_lastEvent == null) {
            return JsonLocation.NA;
        }
        return _locationFor(_lastEvent.getEndMark());
    }

    protected JsonLocation _locationFor(Optional<Mark> option)
    {
        if (!option.isPresent()) {
            return new JsonLocation(_ioContext.getSourceReference(),
                    -1, -1, -1);
        }
        Mark m = option.get();
        return new JsonLocation(_ioContext.getSourceReference(),
                m.getIndex(),
                m.getLine() + 1, // from 0- to 1-based
                m.getColumn() + 1); // ditto
    }

    // Note: SHOULD override 'getTokenLineNr', 'getTokenColumnNr', but those are final in 2.0

    /*
    /**********************************************************************
    /* Parsing
    /**********************************************************************
     */

    @Override
    public JsonToken nextToken() throws JacksonException
    {
        _currentIsAlias = false;
        _binaryValue = null;
        if (_closed) {
            return null;
        }

        while (true /*_yamlParser.hasNext()*/) {
            Event evt;
            try {
                evt = _yamlParser.next();
            } catch (org.snakeyaml.engine.v2.exceptions.YamlEngineException e) {
                throw new JacksonYAMLParseException(this, e.getMessage(), e);
            }
            // is null ok? Assume it is, for now, consider to be same as end-of-doc
            if (evt == null) {
                _currentAnchor = Optional.empty();
                _lastTagEvent = null;
                return (_currToken = null);
            }
            _lastEvent = evt;
            // One complication: field names are only inferred from the fact that we are
            // in Object context; they are just ScalarEvents (but separate and NOT just tagged
            // on values)
            if (_parsingContext.inObject()) {
                if (_currToken != JsonToken.FIELD_NAME) {
                    if (evt.getEventId() != Event.ID.Scalar) {
                        _currentAnchor = Optional.empty();
                        _lastTagEvent = null;
                        // end is fine
                        if (evt.getEventId() == Event.ID.MappingEnd) {
                            if (!_parsingContext.inObject()) { // sanity check is optional, but let's do it for now
                                _reportMismatchedEndMarker('}', ']');
                            }
                            _parsingContext = _parsingContext.getParent();
                            return (_currToken = JsonToken.END_OBJECT);
                        }
                        _reportError("Expected a field name (Scalar value in YAML), got this instead: "+evt);
                    }

                    // 20-Feb-2019, tatu: [dataformats-text#123] Looks like YAML exposes Anchor for Object at point
                    //   where we return START_OBJECT (which makes sense), but, alas, Jackson expects that at point
                    //   after first FIELD_NAME. So we will need to defer clearing of the anchor slightly,
                    //   just for the very first entry; and only if no anchor for name found.
                    //  ... not even 100% sure this is correct, or robust, but does appear to work for specific
                    //  test case given.
                    final ScalarEvent scalar = (ScalarEvent) evt;
                    final boolean firstEntry = (_currToken == JsonToken.START_OBJECT);
                    final Optional<Anchor> newAnchor = scalar.getAnchor();
                    if (newAnchor.isPresent() || !firstEntry) {
                        _currentAnchor = scalar.getAnchor();
                    }
                    // 23-Nov-2020, tatu: [dataformats-text#232] shows case where ref to type id
                    //   needs to be similarly deferred...
                    if (!firstEntry) {
                        _lastTagEvent = evt;
                    }
                    final String name = scalar.getValue();
                    _currentFieldName = name;
                    _parsingContext.setCurrentName(name);
                    return (_currToken = JsonToken.FIELD_NAME);
                }
            } else if (_parsingContext.inArray()) {
                _parsingContext.valueRead();
            }

            _currentAnchor = Optional.empty();
            _lastTagEvent = evt;

            switch (evt.getEventId()) {
                case Scalar:
                    // scalar values are probably the commonest:
                    JsonToken t = _decodeScalar((ScalarEvent) evt);
                    _currToken = t;
                    return t;
                case MappingStart:
                    // followed by maps, then arrays
                    Optional<Mark> m = evt.getStartMark();
                    MappingStartEvent map = (MappingStartEvent) evt;
                    _currentAnchor = map.getAnchor();
                    _parsingContext = _parsingContext.createChildObjectContext(
                            m.map(mark -> mark.getLine()).orElse(0), m.map(mark -> mark.getColumn()).orElse(0));
                    return (_currToken = JsonToken.START_OBJECT);

                case MappingEnd:
                    // actually error; can not have map-end here
                    _reportError("Not expecting END_OBJECT but a value");

                case SequenceStart:
                    Optional<Mark> mrk = evt.getStartMark();
                    _currentAnchor = ((NodeEvent) evt).getAnchor();
                    _parsingContext = _parsingContext.createChildArrayContext(
                            mrk.map(mark -> mark.getLine()).orElse(0), mrk.map(mark -> mark.getColumn()).orElse(0));
                    return (_currToken = JsonToken.START_ARRAY);

                case SequenceEnd:
                    if (!_parsingContext.inArray()) { // sanity check is optional, but let's do it for now
                        _reportMismatchedEndMarker(']', '}');
                    }
                    _parsingContext = _parsingContext.getParent();
                    return (_currToken = JsonToken.END_ARRAY);

                // after this, less common tokens:
                case DocumentEnd:
                    // [dataformat-yaml#72]: logical end of doc; fine. Two choices; either skip,
                    // or return null as marker (but do NOT close). Earlier returned `null`, but
                    // to allow multi-document reading should actually just skip.
                    // return (_currToken = null);
                    continue;

                case DocumentStart:
                    // DocumentStartEvent dd = (DocumentStartEvent) evt;
                    // does this matter? Shouldn't, should it?
                    continue;

                case Alias:
                    AliasEvent alias = (AliasEvent) evt;
                    _currentIsAlias = true;
                    _textValue = alias.getAnchor().orElseThrow(() -> new RuntimeException("Alias must be provided.")).getValue();
                    _cleanedTextValue = null;
                    // for now, nothing to do: in future, maybe try to expose as ObjectIds?
                    return (_currToken = JsonToken.VALUE_STRING);

                case StreamEnd:
                    // end-of-input; force closure
                    close();
                    return (_currToken = null);

                case StreamStart:
                    // useless, skip
                    continue;
            }
        }
    }

    protected JsonToken _decodeScalar(ScalarEvent scalar) throws JacksonException
    {
        String value = scalar.getValue();

        _textValue = value;
        _cleanedTextValue = null;

        // [dataformats-text#130]: Allow determining whether empty String is
        // coerced into null or not
        if (!_cfgEmptyStringsToNull && value.isEmpty()) {
            return JsonToken.VALUE_STRING;
        }

        // we may get an explicit tag, if so, use for corroborating...
        Optional<String> typeTagOptional = scalar.getTag();
        final int len = value.length();
        if (!typeTagOptional.isPresent() || typeTagOptional.get().equals("!")) { // no, implicit
            Tag nodeTag = _yamlResolver.resolve(value, scalar.getImplicit().canOmitTagInPlainScalar());
            if (nodeTag == Tag.STR) {
                return JsonToken.VALUE_STRING;
            }
            if (nodeTag == Tag.INT) {
                return _decodeNumberScalar(value, len);
            }
            if (nodeTag == Tag.FLOAT) {
                _numTypesValid = 0;
                return _cleanYamlFloat(value);
            }
            if (nodeTag == Tag.BOOL) {
                Boolean B = _matchYAMLBoolean(value, len);
                if (B != null) {
                    return B ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE;
                }
            } else if (nodeTag == Tag.NULL) {
                return JsonToken.VALUE_NULL;
            } else {
                // what to do with timestamp and binary and merge etc.
                return JsonToken.VALUE_STRING;
            }
        } else { // yes, got type tag
            String typeTag = typeTagOptional.get();
            if (typeTag.startsWith("tag:yaml.org,2002:")) {
                typeTag = typeTag.substring("tag:yaml.org,2002:".length());
                if (typeTag.contains(",")) {
                    typeTag = typeTag.split(",")[0];
                }
            }
            // [dataformats-text#39]: support binary type
            if ("binary".equals(typeTag)) {
                // 15-Dec-2017, tatu: 2.9.4 uses Jackson's codec because SnakeYAML does
                //    not export its codec via OSGi (breaking 2.9.3). Note that trailing
                //    whitespace is ok with core 2.9.4, but not earlier, so we'll trim
                //    on purpose here
                value = value.trim();
                try {
                    _binaryValue = Base64Variants.MIME.decode(value);
                } catch (IllegalArgumentException e) {
                    _reportError(e.getMessage());
                }
                return JsonToken.VALUE_EMBEDDED_OBJECT;
            }
            // canonical values by YAML are actually 'y' and 'n'; but plenty more unofficial:
            if ("bool".equals(typeTag)) { // must be "true" or "false"
                Boolean B = _matchYAMLBoolean(value, len);
                if (B != null) {
                    return B ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE;
                }
            } else {
                if ("int".equals(typeTag)) {
                    return _decodeNumberScalar(value, len);
                }
                if ("float".equals(typeTag)) {
                    _numTypesValid = 0;
                    return _cleanYamlFloat(value);
                }
                if ("null".equals(typeTag)) {
                    return JsonToken.VALUE_NULL;
                }
            }
        }

        // any way to figure out actual type? No?
        return JsonToken.VALUE_STRING;
    }

    protected Boolean _matchYAMLBoolean(String value, int len)
    {
        switch (len) {
        case 4:
            //TODO it should be only lower case
            if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
            break;
        case 5:
            if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
            break;
        }
        return null;
    }

    protected JsonToken _decodeNumberScalar(String value, final int len)
    {
        // 05-May-2012, tatu: Turns out this is a hot spot; so let's write it
        //  out and avoid regexp overhead...

        //if (PATTERN_INT.matcher(value).matches()) {
        int i;
        char ch = value.charAt(0);
        if (ch == '-') {
            _numberNegative = true;
            i = 1;
        } else if (ch == '+') {
            _numberNegative = false;
            if (len == 1) {
                return null;
            }
            i = 1;
        } else {
            _numberNegative = false;
            i = 0;
        }
        if (len == i) { // should not occur but play it safe
            return null;
        }
        // Next: either "0" ("-0" and "+0" also accepted), or non-decimal. So:
        if (value.charAt(i) == '0') {
            if (++i == len) {
                // can leave "_numberNegative" as is, does not matter
                _numberInt = 0;
                _numTypesValid = NR_INT;
                return JsonToken.VALUE_NUMBER_INT;
            }
            ch = value.charAt(i);

            switch (ch) {
            case 'b': case 'B': // binary
                return _decodeNumberIntBinary(value, i+1, len, _numberNegative);
            case 'x': case 'X': // hex
                return _decodeNumberIntHex(value, i+1, len, _numberNegative);
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
            case '_':
                return _decodeNumberIntOctal(value, i, len, _numberNegative);
            default:
            }
            // should never occur, but in abundance of caution, let's not
            // throw exception but just return as String
            return JsonToken.VALUE_STRING;
        }
        
        // 23-Nov-2020, tatu: will now check and support all formats EXCEPT
        //    for 60-base; 60-base is trickier not just because decoding gets
        //    more involved but also because it can accidentally "detect" values
        //    that we most likely expressing either Times or IP numbers.
        
        boolean underscores = false;

        while (true) {
            int c = value.charAt(i);
            if (c > '9' || c < '0') {
                if (c == '_') {
                    underscores = true;
                } else {
                    break;
                }
            }
            if (++i == len) {
                _numTypesValid = 0;
                if (underscores) {
                    return _cleanYamlInt(value);
                }
                _cleanedTextValue = _textValue;
                return JsonToken.VALUE_NUMBER_INT;
            }
        }
        // 22-Nov-2020, tatu: Should not be needed; SnakeYAML does not
        //   tag things this way...
//        if (PATTERN_FLOAT.matcher(value).matches()) {
//            _numTypesValid = 0;
//            return _cleanYamlFloat(_textValue);
//        }

        // 25-Aug-2016, tatu: If we can't actually match it to valid number,
        //    consider String; better than claiming there's not token
        return JsonToken.VALUE_STRING;
    }

    // @since 2.12
    protected JsonToken _decodeNumberIntBinary(final String value, int i, final int origLen,
            boolean negative)
    {
        final String cleansed = _cleanUnderscores(value, i, origLen);
        int digitLen = cleansed.length();

        if (digitLen <= 31) {
            int v = Integer.parseInt(cleansed, 2);
            if (negative) {
                v = -v;
            }
            _numberInt = v;
            _numTypesValid = NR_INT;
            return JsonToken.VALUE_NUMBER_INT;
        }
        if (digitLen <= 63) {
            return _decodeFromLong(Long.parseLong(cleansed, 2), negative,
                    (digitLen == 32));
        }
        return _decodeFromBigInteger(new BigInteger(cleansed, 2), negative);
    }

    // @since 2.12
    protected JsonToken _decodeNumberIntOctal(final String value, int i, final int origLen,
            boolean negative)
    {
        final String cleansed = _cleanUnderscores(value, i, origLen);
        int digitLen = cleansed.length();

        if (digitLen <= 10) { // 30 bits
            int v = Integer.parseInt(cleansed, 8);
            if (negative) {
                v = -v;
            }
            _numberInt = v;
            _numTypesValid = NR_INT;
            return JsonToken.VALUE_NUMBER_INT;
        }
        if (digitLen <= 21) { // 63 bits
            return _decodeFromLong(Long.parseLong(cleansed, 8), negative, false);
        }
        return _decodeFromBigInteger(new BigInteger(cleansed, 8), negative);
    }

    // @since 2.12
    protected JsonToken _decodeNumberIntHex(final String value, int i, final int origLen,
            boolean negative)
    {
        final String cleansed = _cleanUnderscores(value, i, origLen);
        int digitLen = cleansed.length();

        if (digitLen <= 7) { // 28 bits
            int v = Integer.parseInt(cleansed, 16);
            if (negative) {
                v = -v;
            }
            _numberInt = v;
            _numTypesValid = NR_INT;
            return JsonToken.VALUE_NUMBER_INT;
        }
        if (digitLen <= 15) { // 60 bits
            return _decodeFromLong(Long.parseLong(cleansed, 16), negative,
                    (digitLen == 8));
        }
        return _decodeFromBigInteger(new BigInteger(cleansed, 16), negative);
    }

    private JsonToken _decodeFromLong(long unsignedValue, boolean negative,
            boolean checkIfInt)
    {
        long actualValue;

        if (negative) {
            actualValue = -unsignedValue;
            if (checkIfInt && (actualValue >= MIN_INT_L)) {
                _numberInt = (int) actualValue;
                _numTypesValid = NR_INT;
                return JsonToken.VALUE_NUMBER_INT;
            }
        } else {
            if (checkIfInt && (unsignedValue < MAX_INT_L)) {
                _numberInt = (int) unsignedValue;
                _numTypesValid = NR_INT;
                return JsonToken.VALUE_NUMBER_INT;
            }
            actualValue = unsignedValue;
        }
        _numberLong = actualValue;
        _numTypesValid = NR_LONG;
        return JsonToken.VALUE_NUMBER_INT;
    }

    private JsonToken _decodeFromBigInteger(BigInteger unsignedValue, boolean negative)
    {
        // Should we check for bounds here too? Let's not bother yet
        if (negative) {
            _numberBigInt = unsignedValue.negate();
        } else {
            _numberBigInt = unsignedValue;
        }
        _numTypesValid = NR_BIGINT;
        return JsonToken.VALUE_NUMBER_INT;
    }

    /*
    /**********************************************************
    /* String value handling
    /**********************************************************
     */

    // For now we do not store char[] representation...
    @Override
    public boolean hasTextCharacters() {
        return false;
    }

    @Override
    public String getText() throws JacksonException
    {
        if (_currToken == JsonToken.VALUE_STRING) {
            return _textValue;
        }
        if (_currToken == JsonToken.FIELD_NAME) {
            return _currentFieldName;
        }
        if (_currToken != null) {
            if (_currToken.isScalarValue()) {
                return _textValue;
            }
            return _currToken.asString();
        }
        return null;
    }

    @Override
    public String currentName() throws JacksonException
    {
        if (_currToken == JsonToken.FIELD_NAME) {
            return _currentFieldName;
        }
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            SimpleTokenReadContext parent = _parsingContext.getParent();
            if (parent != null) {
                return parent.currentName();
            }
        }
        return _parsingContext.currentName();
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

    /*
    /**********************************************************************
    /* Binary (base64)
    /**********************************************************************
     */

    @Override
    public Object getEmbeddedObject() throws JacksonException {
        if (_currToken == JsonToken.VALUE_EMBEDDED_OBJECT) {
            return _binaryValue;
        }
        return null;
    }

    // Base impl from `ParserBase` works fine here:
//    public byte[] getBinaryValue(Base64Variant variant) throws JacksonException

    @Override
    public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws JacksonException
    {
        byte[] b = getBinaryValue(b64variant);
        try {
            out.write(b);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
        return b.length;
    }

    /*
    /**********************************************************************
    /* Number accessor overrides
    /**********************************************************************
     */

    @Override
    protected void _parseNumericValue(int expType) throws JacksonException
    {
        // Int or float?
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            int len = _cleanedTextValue.length();
            if (_numberNegative) {
                len--;
            }
            if (len <= 9) { // definitely fits in int
                _numberInt = Integer.parseInt(_cleanedTextValue);
                _numTypesValid = NR_INT;
                return;
            }
            if (len <= 18) { // definitely fits AND is easy to parse using 2 int parse calls
                long l = Long.parseLong(_cleanedTextValue);
                // [JACKSON-230] Could still fit in int, need to check
                if (len == 10) {
                    if (_numberNegative) {
                        if (l >= Integer.MIN_VALUE) {
                            _numberInt = (int) l;
                            _numTypesValid = NR_INT;
                            return;
                        }
                    } else {
                        if (l <= Integer.MAX_VALUE) {
                            _numberInt = (int) l;
                            _numTypesValid = NR_INT;
                            return;
                        }
                    }
                }
                _numberLong = l;
                _numTypesValid = NR_LONG;
                return;
            }
            // !!! TODO: implement proper bounds checks; now we'll just use BigInteger for convenience
            try {
                BigInteger n = new BigInteger(_cleanedTextValue);
                // Could still fit in a long, need to check
                if (len == 19 && n.bitLength() <= 63) {
                    _numberLong = n.longValue();
                    _numTypesValid = NR_LONG;
                    return;
                }
                _numberBigInt = n;
                _numTypesValid = NR_BIGINT;
                return;
            } catch (NumberFormatException nex) {
                // NOTE: pass non-cleaned variant for error message
                // Can this ever occur? Due to overflow, maybe?
                throw _constructReadException(String.format(
                        "Malformed numeric value '%s: (%s) %s",
                        _textValue, nex.getClass().getName(), nex.getMessage()));
            }
        }
        if (_currToken == JsonToken.VALUE_NUMBER_FLOAT) {
            // strip out optional underscores, if any:
            final String str = _cleanedTextValue;
            try {
                if (expType == NR_BIGDECIMAL) {
                    _numberBigDecimal = new BigDecimal(str);
                    _numTypesValid = NR_BIGDECIMAL;
                } else {
                    // Otherwise double has to do
                    _numberDouble = Double.parseDouble(str);
                    _numTypesValid = NR_DOUBLE;
                }
            } catch (NumberFormatException nex) {
                // Can this ever occur? Due to overflow, maybe?
                // NOTE: pass non-cleaned variant for error message
                throw _constructReadException(String.format(
                        "Malformed numeric value '%s: (%s) %s",
                        _textValue, nex.getClass().getName(), nex.getMessage()));
            }
            return;
        }
        _reportError("Current token (" + _currToken + ") not numeric, can not use numeric value accessors");
    }

    @Override
    protected int _parseIntValue() throws JacksonException
    {
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            int len = _cleanedTextValue.length();
            if (_numberNegative) {
                len--;
            }
            if (len <= 9) { // definitely fits in int
                _numTypesValid = NR_INT;
                return (_numberInt = Integer.parseInt(_cleanedTextValue));
            }
        }
        _parseNumericValue(NR_INT);
        if ((_numTypesValid & NR_INT) == 0) {
            convertNumberToInt();
        }
        return _numberInt;
    }

    /*
    /**********************************************************************
    /* Native id (type id) access
    /**********************************************************************
     */

    @Override
    public String getObjectId() throws JacksonException
    {
        return _currentAnchor.map(a -> a.getValue()).orElse(null);
    }

    @Override
    public String getTypeId() throws JacksonException
    {
        Optional<String> tagOpt;
        if (_lastTagEvent instanceof CollectionStartEvent) {
            tagOpt = ((CollectionStartEvent) _lastTagEvent).getTag();
  //System.err.println("getTypeId() at "+currentToken()+", last was collection ("+_lastTagEvent.getClass().getSimpleName()+") -> "+tag);
        } else if (_lastTagEvent instanceof ScalarEvent) {
            tagOpt = ((ScalarEvent) _lastTagEvent).getTag();
          //System.err.println("getTypeId() at "+currentToken()+", last was scalar -> "+tag+", scalar == "+_lastEvent);
        } else {
//System.err.println("getTypeId(), something else, curr token: "+currentToken());
            return null;
        }
        if (tagOpt.isPresent()) {
            String tag = tagOpt.get();
            // 04-Aug-2013, tatu: Looks like YAML parser's expose these in... somewhat exotic
            //   ways sometimes. So let's prepare to peel off some wrappings:
            while (tag.startsWith("!")) {
                tag = tag.substring(1);
            }
            return tag;
        }
        return null;
    }
    
    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    /**
     * Helper method used to clean up YAML integer value so it can be parsed
     * using standard JDK classes.
     * Currently this just means stripping out optional underscores.
     */
    private JsonToken _cleanYamlInt(String str)
    {
        // Here we already know there is either plus sign, or underscore (or both) so
        final int len = str.length();
        StringBuilder sb = new StringBuilder(len);
        // first: do we have a leading plus sign to skip?
        int i = (str.charAt(0) == '+') ? 1 : 0;
        for (; i < len; ++i) {
            char c = str.charAt(i);
            if (c != '_') {
                sb.append(c);
            }
        }
        _cleanedTextValue = sb.toString();
        return JsonToken.VALUE_NUMBER_INT;
    }

    private String _cleanUnderscores(String str, int i, final int len)
    {
        final StringBuilder sb = new StringBuilder(len);
        for (; i < len; ++i) {
            char ch = str.charAt(i);
            if (ch != '_') {
                sb.append(ch);
            }
        }
        // tiny optimization: if nothing was trimmed, return String
        if (sb.length() == len) {
            return str;
        }
        return sb.toString();
    }

    private JsonToken _cleanYamlFloat(String str)
    {
        // Here we do NOT yet know whether we might have underscores so check
        final int len = str.length();
        int ix = str.indexOf('_');
        if (ix < 0 || len == 0) {
            _cleanedTextValue = str;
            return JsonToken.VALUE_NUMBER_FLOAT;
        }
        StringBuilder sb = new StringBuilder(len);
        // first: do we have a leading plus sign to skip?
        int i = (str.charAt(0) == '+') ? 1 : 0;
        for (; i < len; ++i) {
            char c = str.charAt(i);
            if (c != '_') {
                sb.append(c);
            }
        }
        _cleanedTextValue = sb.toString();
        return JsonToken.VALUE_NUMBER_FLOAT;
    }

    // Promoted from `ParserBase` in 3.0
    protected void _reportMismatchedEndMarker(int actCh, char expCh) throws JacksonException {
        TokenStreamContext ctxt = getParsingContext();
        _reportError(String.format(
                "Unexpected close marker '%s': expected '%c' (for %s starting at %s)",
                (char) actCh, expCh, ctxt.typeDesc(), ctxt.getStartLocation(_getSourceReference())));
    }
}
