package tools.jackson.dataformat.yaml;

import java.io.*;
import java.math.BigInteger;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import tools.jackson.core.*;
import tools.jackson.core.base.ParserBase;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.NumberInput;
import tools.jackson.core.json.DupDetector;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.JacksonFeatureSet;
import tools.jackson.core.util.SimpleStreamReadContext;

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
import org.snakeyaml.engine.v2.resolver.ScalarResolver;
import org.snakeyaml.engine.v2.scanner.StreamReader;

/**
 * {@link JsonParser} implementation used to expose YAML documents
 * in form that allows other Jackson functionality to process YAML content,
 * such as binding POJOs to and from it, and building tree representations.
 */
public class YAMLParser extends ParserBase
{
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
    protected final ScalarResolver _yamlResolver;

    /*
    /**********************************************************************
    /* State
    /**********************************************************************
     */

    /**
     * Information about parser context, context in which
     * the next token is to be parsed (root, array, object).
     */
    protected SimpleStreamReadContext _streamReadContext;

    /**
     * Keep track of the last event read, to get access to Location info
     */
    protected Event _lastEvent;

    /**
     * To keep track of tags ("type ids"), need to either get tags for all
     * events, or, keep tag of relevant event that might have it: this is
     * different from {@code _lastEvent} in some cases.
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
     * Let's also have a local copy of the current property name
     */
    protected String _currentName;

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

    /**
     * Flag to track whether we have seen any actual content (not just
     * Document/Stream start/end events) to detect empty documents.
     */
    protected boolean _hasContent;

    /**
     * Flag to track whether we're currently emitting a synthetic empty object
     * for an empty document (when {@link YAMLReadFeature#EMPTY_DOCUMENT_AS_EMPTY_OBJECT}
     * is enabled).
     */
    protected boolean _emittingSyntheticEmptyObject;    

    /**
     * Patterns for matching YAML's notation for Infinity and NaN values.
     */
    protected final static Pattern _patternInf = Pattern.compile("([-+]?)\\.(?:inf|Inf|INF)");
    protected final static Pattern _patternNaN = Pattern.compile("\\.(?:nan|NaN|NAN)");

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public YAMLParser(ObjectReadContext readCtxt, IOContext ioCtxt, BufferRecycler br,
            int streamReadFeatures, int formatFeatures,
            LoadSettings loadSettings, Reader reader)
    {
        super(readCtxt, ioCtxt, streamReadFeatures);
        if (loadSettings == null) {
            loadSettings = LoadSettings.builder().build();
        }
        _formatFeatures = formatFeatures;
        _reader = reader;
        _yamlParser = new ParserImpl(loadSettings, new StreamReader(loadSettings, reader));
        _yamlResolver = loadSettings.getSchema().getScalarResolver();

        _cfgEmptyStringsToNull = YAMLReadFeature.EMPTY_STRING_AS_NULL.enabledIn(formatFeatures);
        DupDetector dups = StreamReadFeature.STRICT_DUPLICATE_DETECTION.enabledIn(streamReadFeatures)
                ? DupDetector.rootDetector(this) : null;
        _streamReadContext = SimpleStreamReadContext.createRootContext(dups);
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

    // @since 3.2
    @Override
    public boolean willInternPropertyNames() { return false; }

    @Override
    public JacksonFeatureSet<StreamReadCapability> streamReadCapabilities() {
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
    public Reader streamReadInputSource() {
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
        if (_reader != null) {
            if (_ioContext.isResourceManaged() || isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE)) {
                _reader.close();
            }
        }
    }

    @Override public TokenStreamContext streamReadContext() { return _streamReadContext; }
    @Override public void assignCurrentValue(Object v) { _streamReadContext.assignCurrentValue(v); }
    @Override public Object currentValue() { return _streamReadContext.currentValue(); }

    /*
    /**********************************************************************
    /* Public API, configuration
    /**********************************************************************
     */

    /**
     * Method for checking whether specified {@link YAMLReadFeature}
     * is enabled.
     */
    public boolean isEnabled(YAMLReadFeature f) {
        return (_formatFeatures & f.getMask()) != 0;
    }

//    @Override public XxxSchema getSchema() 

    /*
    /**********************************************************************
    /* Location info
    /**********************************************************************
     */

    @Override
    public TokenStreamLocation currentTokenLocation()
    {
        if (_lastEvent == null) {
            return TokenStreamLocation.NA;
        }
        return _locationFor(_lastEvent.getStartMark());
    }

    @Override
    public TokenStreamLocation currentLocation() {
        // can assume we are at the end of token now...
        if (_lastEvent == null) {
            return TokenStreamLocation.NA;
        }
        return _locationFor(_lastEvent.getEndMark());
    }

    protected TokenStreamLocation _locationFor(Optional<Mark> option)
    {
        if (!option.isPresent()) {
            return new TokenStreamLocation(_ioContext.contentReference(),
                    -1, -1, -1);
        }
        Mark m = option.get();
        return new TokenStreamLocation(_ioContext.contentReference(),
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
        _numTypesValid = NR_UNKNOWN;
        if (_closed) {
            return null;
        }

        // [dataformats-text#154]: Handle synthetic empty object for empty documents
        if (_emittingSyntheticEmptyObject) {
            _emittingSyntheticEmptyObject = false;
            _streamReadContext = _streamReadContext.getParent();
            JsonToken token = _updateToken(JsonToken.END_OBJECT);
            // Now close since we've emitted the complete empty object
            close();
            return token;
        }

        while (true /*_yamlParser.hasNext()*/) {
            Event evt;
            try {
                evt = nextEvent();
            } catch (org.snakeyaml.engine.v2.exceptions.YamlEngineException e) {
                throw new JacksonYAMLParseException(this, e.getMessage(), e);
            } catch (NumberFormatException e) {
                // 12-Jan-2024, tatu: As per https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=63274
                //    we seem to have unhandled case by SnakeYAML
                throw _constructReadException(String.format(
                        "Malformed Number token: failed to tokenize due to (%s): %s",
                        e.getClass().getName(), e.getMessage()),
                        e);
            }
            // is null ok? Assume it is, for now, consider to be same as end-of-doc
            if (evt == null) {
                _currentAnchor = Optional.empty();
                _lastTagEvent = null;
                return _updateTokenToNull();
            }
            _lastEvent = evt;
            // One complication: property names are only inferred from the fact that we are
            // in Object context; they are just ScalarEvents (but separate and NOT just tagged
            // on values)
            if (_streamReadContext.inObject()) {
                if (_currToken != JsonToken.PROPERTY_NAME) {
                    if (evt.getEventId() != Event.ID.Scalar) {
                        _currentAnchor = Optional.empty();
                        _lastTagEvent = null;
                        // end is fine
                        if (evt.getEventId() == Event.ID.MappingEnd) {
                            if (!_streamReadContext.inObject()) { // sanity check is optional, but let's do it for now
                                _reportMismatchedEndMarker('}', ']');
                            }
                            _streamReadContext = _streamReadContext.getParent();
                            return _updateToken(JsonToken.END_OBJECT);
                        }
                        _reportError("Expected a property name (Scalar value in YAML), got this instead: "+evt);
                    }

                    // 20-Feb-2019, tatu: [dataformats-text#123] Looks like YAML exposes Anchor for Object at point
                    //   where we return START_OBJECT (which makes sense), but, alas, Jackson expects that at point
                    //   after first PROPERTY_NAME. So we will need to defer clearing of the anchor slightly,
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
                    // [dataformats-text#636]: validate name length
                    _streamReadConstraints.validateNameLength(name.length());
                    _currentName = name;
                    _streamReadContext.setCurrentName(name);
                    return _updateToken(JsonToken.PROPERTY_NAME);
                }
            } else if (_streamReadContext.inArray()) {
                _streamReadContext.valueRead();
            }

            _currentAnchor = Optional.empty();
            _lastTagEvent = evt;

            switch (evt.getEventId()) {
                case Scalar:
                    // scalar values are probably the commonest:
                    _hasContent = true;
                    JsonToken t = _decodeScalar((ScalarEvent) evt);
                    return _updateToken(t);
                case MappingStart:
                    // followed by maps, then arrays
                    _hasContent = true;
                    Optional<Mark> m = evt.getStartMark();
                    MappingStartEvent map = (MappingStartEvent) evt;
                    _currentAnchor = map.getAnchor();
                    _streamReadContext = _streamReadContext.createChildObjectContext(
                            m.map(mark -> mark.getLine()).orElse(0), m.map(mark -> mark.getColumn()).orElse(0));
                    _streamReadConstraints.validateNestingDepth(_streamReadContext.getNestingDepth());
                    return _updateToken(JsonToken.START_OBJECT);

                case MappingEnd:
                    // actually error; can not have map-end here
                    _reportError("Not expecting END_OBJECT but a value");

                case SequenceStart:
                    _hasContent = true;
                    Optional<Mark> mrk = evt.getStartMark();
                    _currentAnchor = ((NodeEvent) evt).getAnchor();
                    _streamReadContext = _streamReadContext.createChildArrayContext(
                            mrk.map(mark -> mark.getLine()).orElse(0), mrk.map(mark -> mark.getColumn()).orElse(0));
                    _streamReadConstraints.validateNestingDepth(_streamReadContext.getNestingDepth());
                    return _updateToken(JsonToken.START_ARRAY);

                case SequenceEnd:
                    if (!_streamReadContext.inArray()) { // sanity check is optional, but let's do it for now
                        _reportMismatchedEndMarker(']', '}');
                    }
                    _streamReadContext = _streamReadContext.getParent();
                    return _updateToken(JsonToken.END_ARRAY);

                // after this, less common tokens:
                case DocumentEnd:
                    // [dataformat-yaml#72]: logical end of doc; fine. Two choices; either skip,
                    // or return null as marker (but do NOT close). Earlier returned `null`, but
                    // to allow multi-document reading should actually just skip.
                    // return _updateTokenToNull();
                    continue;

                case DocumentStart:
                    // DocumentStartEvent dd = (DocumentStartEvent) evt;
                    // does this matter? Shouldn't, should it?
                    continue;

                case Alias:
                    AliasEvent alias = (AliasEvent) evt;
                    _currentIsAlias = true;
                    _textValue = alias.getAnchor().orElseThrow(() -> new RuntimeException("Alias must be provided.")).getValue();
                    // [dataformats-text#636]: validate string value length
                    _streamReadConstraints.validateStringLength(_textValue.length());
                    _cleanedTextValue = null;
                    // for now, nothing to do: in future, maybe try to expose as ObjectIds?
                    return _updateToken(JsonToken.VALUE_STRING);

                case StreamEnd:
                    // end-of-input; force closure
                    // [dataformats-text#154]: If we have no content and feature is enabled,
                    // emit a synthetic empty object instead of null
                    if (!_hasContent
                            && YAMLReadFeature.EMPTY_DOCUMENT_AS_EMPTY_OBJECT.enabledIn(_formatFeatures)) {
                        _emittingSyntheticEmptyObject = true;
                        // Don't close yet - we need to emit END_OBJECT first
                        _streamReadContext.createChildObjectContext(0, 0);
                        return _updateToken(JsonToken.START_OBJECT);
                    }
                    close();
                    return _updateTokenToNull();

                case StreamStart:
                    // useless, skip
                    continue;
                // 22-Jul-2022, tatu: Interesting... what to do?
                case Comment:
                    continue;

                // 26-Feb-2023, tatu: Should we report an error or... ?
                default:
            }
        }
    }

    protected Event nextEvent() {
        return _yamlParser.next();
    }

    protected JsonToken _decodeScalar(ScalarEvent scalar) throws JacksonException
    {
        String value = scalar.getValue();
        // [dataformats-text#636]: validate string value length
        _streamReadConstraints.validateStringLength(value.length());

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
                    final String[] tags = typeTag.split(",");
                    typeTag = (tags.length == 0) ? "" : tags[0];
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
                // 17-Sep-2022, tatu: empty String value is not valid number;
                //    so we could indicate exception or... for now, report as
                //    String value?
                if (len > 0) {
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
        throws JacksonException
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
            case 'o':
                // Explicit 0o prefix: always octal (YAML 1.2)
                return _decodeNumberIntOctal(value, i+1, len, _numberNegative);
            case 'x': case 'X': // hex
                return _decodeNumberIntHex(value, i+1, len, _numberNegative);
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
            case '_':
                // Implicit octal (leading 0 followed by digits): check feature flag
                if (YAMLReadFeature.PARSE_OCTAL_NUMBERS.enabledIn(_formatFeatures)) {
                    return _decodeNumberIntOctal(value, i, len, _numberNegative);
                }
                // When disabled, fall through to decimal parsing below
                break;
            default:
                // should never occur, but in abundance of caution, let's not
                // throw exception but just return as String
                return JsonToken.VALUE_STRING;
            }
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

    protected JsonToken _decodeNumberIntBinary(final String value, int i, final int origLen,
            boolean negative)
        throws JacksonException
    {
        final String cleansed = _cleanUnderscores(value, i, origLen);
        int digitLen = cleansed.length();

        if (digitLen <= 31) {
            int v = _decodeInt(cleansed, 2);
            if (negative) {
                v = -v;
            }
            _numberInt = v;
            _numTypesValid = NR_INT;
            return JsonToken.VALUE_NUMBER_INT;
        }
        if (digitLen <= 63) {
            return _decodeFromLong(_decodeLong(cleansed, 2), negative,
                    (digitLen == 32));
        }
        return _decodeFromBigInteger(_decodeBigInt(cleansed, 2), negative);
    }

    protected JsonToken _decodeNumberIntOctal(final String value, int i, final int origLen,
            boolean negative)
        throws JacksonException
    {
        final String cleansed = _cleanUnderscores(value, i, origLen);
        int digitLen = cleansed.length();

        if (digitLen <= 10) { // 30 bits
            int v = _decodeInt(cleansed, 8);
            if (negative) {
                v = -v;
            }
            _numberInt = v;
            _numTypesValid = NR_INT;
            return JsonToken.VALUE_NUMBER_INT;
        }
        if (digitLen <= 21) { // 63 bits
            return _decodeFromLong(_decodeLong(cleansed, 8), negative, false);
        }
        return _decodeFromBigInteger(_decodeBigInt(cleansed, 8), negative);
    }

    protected JsonToken _decodeNumberIntHex(final String value, int i, final int origLen,
            boolean negative)
        throws JacksonException
    {
        final String cleansed = _cleanUnderscores(value, i, origLen);
        int digitLen = cleansed.length();

        if (digitLen <= 7) { // 28 bits
            int v = _decodeInt(cleansed, 16);
            if (negative) {
                v = -v;
            }
            _numberInt = v;
            _numTypesValid = NR_INT;
            return JsonToken.VALUE_NUMBER_INT;
        }
        if (digitLen <= 15) { // 60 bits
            return _decodeFromLong(_decodeLong(cleansed, 16), negative,
                    (digitLen == 8));
        }
        return _decodeFromBigInteger(_decodeBigInt(cleansed, 16), negative);
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

    private int _decodeInt(String numStr, int base) throws JacksonException {
        try {
            return Integer.parseInt(numStr, base);
        } catch (NumberFormatException e) {
            return _reportInvalidNumber(numStr, base, e);
        }
    }

    private long _decodeLong(String numStr, int base) throws JacksonException {
        try {
            return Long.parseLong(numStr, base);
        } catch (NumberFormatException e) {
            return _reportInvalidNumber(numStr, base, e);
        }
    }

    private BigInteger _decodeBigInt(String numStr, int base) throws JacksonException {
        streamReadConstraints().validateIntegerLength(numStr.length());
        try {
            return base == 10 ?
                    NumberInput.parseBigInteger(numStr, isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)) :
                    NumberInput.parseBigIntegerWithRadix(
                            numStr, base, isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        } catch (NumberFormatException e) {
            return _reportInvalidNumber(numStr, base, e);
        }
    }

    private <T> T _reportInvalidNumber(String numStr, int base, Exception e) throws JacksonException {
        _reportError(String.format("Invalid base-%d number ('%s'), problem: %s",
                base, numStr, e.getMessage()));
        return null; // never gets here
    }

    /*
    /**********************************************************************
    /* String value handling
    /**********************************************************************
     */

    // For now we do not store char[] representation...
    @Override
    public boolean hasStringCharacters() {
        return false;
    }

    @Override
    public String getString() throws JacksonException
    {
        if (_currToken == JsonToken.VALUE_STRING) {
            return _textValue;
        }
        if (_currToken == JsonToken.PROPERTY_NAME) {
            return _currentName;
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
        if (_currToken == JsonToken.PROPERTY_NAME) {
            return _currentName;
        }
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            SimpleStreamReadContext parent = _streamReadContext.getParent();
            if (parent != null) {
                return parent.currentName();
            }
        }
        return _streamReadContext.currentName();
    }

    @Override
    public char[] getStringCharacters() throws JacksonException {
        String text = getString();
        return (text == null) ? null : text.toCharArray();
    }

    @Override
    public int getStringLength() throws JacksonException {
        String text = getString();
        return (text == null) ? 0 : text.length();
    }

    @Override
    public int getStringOffset() throws JacksonException {
        return 0;
    }

    @Override
    public int getString(Writer writer) throws JacksonException
    {
        String str = getString();
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
    public Object getNumberValueDeferred() throws JacksonException {
        // 01-Feb-2023, tatu: ParserBase implementation does not quite work
        //   due to refactoring. So let's try to cobble something together

        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            // We might already have suitable value?
            if ((_numTypesValid & NR_INT) != 0) {
                return _numberInt;
            }
            if ((_numTypesValid & NR_LONG) != 0) {
                return _numberLong;
            }
            if ((_numTypesValid & NR_BIGINT) != 0) {
                return _getBigInteger();
            }
            if (_cleanedTextValue == null) {
                _reportError("Internal number decoding error: `_cleanedTextValue` null when nothing decoded for `JsonToken.VALUE_NUMBER_INT`");
            }
            return _cleanedTextValue;
        }
        if (_currToken != JsonToken.VALUE_NUMBER_FLOAT) {
            _reportError("Current token ("+_currToken+") not numeric, can not use numeric value accessors");
        }

        // For FP, see if we might have decoded values already
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            return _getBigDecimal();
        }
        if ((_numTypesValid & NR_DOUBLE) != 0) {
            return _getNumberDouble();
        }
        if ((_numTypesValid & NR_FLOAT) != 0) {
            return _getNumberFloat();
        }

        // But if not, same as BigInteger, let lazy/deferred handling be done
        return _cleanedTextValue;
    }

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
                _numberInt = _decodeInt(_cleanedTextValue, 10);
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
            final String numStr = _cleanedTextValue;
            try {
                streamReadConstraints().validateIntegerLength(numStr.length());
                BigInteger n = NumberInput.parseBigInteger(
                        numStr, isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
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
            final String numStr = _cleanedTextValue;
            try {
                streamReadConstraints().validateFPLength(numStr.length());
                if (expType == NR_BIGDECIMAL) {
                    _numberBigDecimal = NumberInput.parseBigDecimal(
                            numStr, isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
                    _numTypesValid = NR_BIGDECIMAL;
                } else {
                    // Otherwise double has to do
                    streamReadConstraints().validateFPLength(numStr.length());
                    _numberDouble = NumberInput.parseDouble(numStr, isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
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
                _numberInt = _decodeInt(_cleanedTextValue, 10);
                _numTypesValid = NR_INT;
                return _numberInt;
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
        String tag = _currentTag().orElse(null);
        if (tag != null) {
            // 04-Aug-2013, tatu: Looks like YAML parser's expose these in... somewhat exotic
            //   ways sometimes. So let's prepare to peel off some wrappings:
            while (tag.startsWith("!")) {
                tag = tag.substring(1);
            }
        }
        return tag;
    }

    /**
     * Method that can be used to access the YAML tag of the current token,
     * in its raw form: that is, without stripping leading "!" prefix(es)
     * (unlike {@link #getTypeId()} which does strip them).
     *<p>
     * Note that "raw" here means raw with respect to Jackson's processing;
     * SnakeYAML Engine may have already resolved some tag syntax (for example,
     * verbatim tags like {@code !<tag>} are resolved to their URI content).
     *
     * @return Raw YAML tag of the current token, if any; {@code null} if none
     *
     * @since 3.2
     */
    public String getRawTag()
    {
        return _currentTag().orElse(null);
    }

    // @since 3.2
    protected Optional<String> _currentTag()
    {
        if (_lastTagEvent instanceof CollectionStartEvent cse) {
            return cse.getTag();
        } else if (_lastTagEvent instanceof ScalarEvent se) {
            return se.getTag();
        }
        return Optional.empty();
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
    private JsonToken _cleanYamlInt(String str) throws JacksonException
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
        if (_cleanedTextValue.isEmpty() || "-".equals(_cleanedTextValue)) {
            _reportError(String.format("Invalid number ('%s')", str));
        }
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
        Matcher matcher = _patternInf.matcher(str);
        if (matcher.matches()) {
            if (matcher.group(1).equals("-")) {
                return resetAsNaN(str, Double.NEGATIVE_INFINITY);
            }
            else {
                return resetAsNaN(str, Double.POSITIVE_INFINITY);
            }
        }

        if (_patternNaN.matcher(str).matches()) {
            return resetAsNaN(str, Double.NaN);
        }

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
        TokenStreamContext ctxt = streamReadContext();
        _reportError(String.format(
                "Unexpected close marker '%s': expected '%c' (for %s starting at %s)",
                (char) actCh, expCh, ctxt.typeDesc(), ctxt.startLocation(_contentReference())));
    }
}
