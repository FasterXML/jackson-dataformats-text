package com.fasterxml.jackson.dataformat.csv.impl;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.io.NumberInput;
import com.fasterxml.jackson.core.json.JsonReadContext;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.TextBuffer;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * Low-level helper class that handles actual reading of CSV,
 * purely based on indexes given without worrying about reordering etc.
 */
public class CsvDecoder
{
    private final static int INT_SPACE = 0x0020;

    private final static int INT_CR = '\r';
    private final static int INT_LF = '\n';

    /*
    /**********************************************************************
    /* Input handling, configuration
    /**********************************************************************
     */

    /**
     * Unfortunate back reference, needed for error reporting
     */
    protected final CsvParser _owner;
    
    /**
     * I/O context for this reader. It handles buffer allocation
     * for the reader.
     */
    protected final IOContext _ioContext;
    
    /**
     * Input stream that can be used for reading more content, if one
     * in use. May be null, if input comes just as a full buffer,
     * or if the stream has been closed.
     *<p>
     * NOTE: renamed in 2.13 from {@code _inputSource}.
     */
    protected Reader _inputReader;

    /**
     * Flag that indicates whether the input buffer is recycable (and
     * needs to be returned to recycler once we are done) or not.
     *<p>
     * If it is not, it also means that parser can NOT modify underlying
     * buffer.
     */
    protected boolean _bufferRecyclable;

    protected boolean _autoCloseInput;

    /**
     * Configuration flag that determines whether spaces surrounding
     * separator characters are to be automatically trimmed or not.
     */
    protected boolean _trimSpaces;

    protected boolean _allowComments;

    /**
     * @since 2.10.1
     */
    protected boolean _skipBlankLines; // NOTE: can be final in 3.0, not before

    /**
     * Maximum of quote character, linefeeds (\r and \n), escape character.
     */
    protected int _maxSpecialChar;
    
    protected int _separatorChar;

    protected int _quoteChar;

    protected int _escapeChar;

    protected int _maxTokenLength = -1;

    /*
    /**********************************************************************
    /* Input handling, state
    /**********************************************************************
     */

    /**
     * Buffer that contains contents of all values after processing
     * of doubled-quotes, escaped characters.
     */
    protected final TextBuffer _textBuffer;

    /**
     * Current buffer from which data is read; generally data is read into
     * buffer from input source, but in some cases pre-loaded buffer
     * is handed to the parser.
     */
    protected char[] _inputBuffer;

    /**
     * Pointer to next available character in buffer
     */
    protected int _inputPtr = 0;

    /**
     * Index of character after last available one in the buffer.
     */
    protected int _inputEnd = 0;

    /**
     * Marker to indicate that a linefeed was encountered and now
     * needs to be handled (indicates end-of-record).
     */
    protected int _pendingLF = 0;

    /**
     * Flag that indicates whether parser is closed or not. Gets
     * set when parser is either closed by explicit call
     * ({@link #close}) or when end-of-input is reached.
     */
    protected boolean _closed;

    /*
    /**********************************************************************
    /* Current input location information
    /**********************************************************************
     */

    /**
     * Number of characters/bytes that were contained in previous blocks
     * (blocks that were already processed prior to the current buffer).
     */
    protected long _currInputProcessed = 0L;

    /**
     * Current row location of current point in input buffer, starting
     * from 1, if available.
     */
    protected int _currInputRow = 1;

    /**
     * Current index of the first character of the current row in input
     * buffer. Needed to calculate column position, if necessary; benefit
     * of not having column itself is that this only has to be updated
     * once per line.
     */
    protected int _currInputRowStart = 0;

    // // // Location info at point when current token was started

    /**
     * Total number of bytes/characters read before start of current token.
     * For big (gigabyte-sized) sizes are possible, needs to be long,
     * unlike pointers and sizes related to in-memory buffers.
     */
    protected long _tokenInputTotal = 0;

    /**
     * Input row on which current token starts, 1-based
     */
    protected int _tokenInputRow = 1;

    /**
     * Column on input row that current token starts; 0-based (although
     * in the end it'll be converted to 1-based)
     */
    protected int _tokenInputCol = 0;

    protected int _tokenLength = 0;

    /*
    /**********************************************************************
    /* Constants and fields of former 'JsonNumericParserBase'
    /**********************************************************************
     */

    final protected static int NR_UNKNOWN = 0;

    // First, integer types

    final protected static int NR_INT = 0x0001;
    final protected static int NR_LONG = 0x0002;
    final protected static int NR_BIGINT = 0x0004;

    // And then floating point types

    final protected static int NR_DOUBLE = 0x008;
    final protected static int NR_BIGDECIMAL = 0x0010;

    // Also, we need some numeric constants

    final static BigDecimal BD_MIN_LONG = new BigDecimal(Long.MIN_VALUE);
    final static BigDecimal BD_MAX_LONG = new BigDecimal(Long.MAX_VALUE);

    final static BigDecimal BD_MIN_INT = new BigDecimal(Long.MIN_VALUE);
    final static BigDecimal BD_MAX_INT = new BigDecimal(Long.MAX_VALUE);

    final static long MIN_INT_L = Integer.MIN_VALUE;
    final static long MAX_INT_L = Integer.MAX_VALUE;

    // These are not very accurate, but have to do... (for bounds checks)

    final static double MIN_LONG_D = Long.MIN_VALUE;
    final static double MAX_LONG_D = Long.MAX_VALUE;

    final static double MIN_INT_D = Integer.MIN_VALUE;
    final static double MAX_INT_D = Integer.MAX_VALUE;

    // Digits, numeric
    final protected static int INT_0 = '0';
    final protected static int INT_1 = '1';
    final protected static int INT_2 = '2';
    final protected static int INT_3 = '3';
    final protected static int INT_4 = '4';
    final protected static int INT_5 = '5';
    final protected static int INT_6 = '6';
    final protected static int INT_7 = '7';
    final protected static int INT_8 = '8';
    final protected static int INT_9 = '9';

    final protected static int INT_MINUS = '-';
    final protected static int INT_PLUS = '+';
    final protected static int INT_DECIMAL_POINT = '.';

    final protected static int INT_e = 'e';
    final protected static int INT_E = 'E';

    final protected static char CHAR_NULL = '\0';
    
    // Numeric value holders: multiple fields used for
    // for efficiency

    /**
     * Bitfield that indicates which numeric representations
     * have been calculated for the current type
     */
    protected int _numTypesValid = NR_UNKNOWN;

    // First primitives

    protected int _numberInt;

    protected long _numberLong;

    protected double _numberDouble;

    // And then object types

    protected BigInteger _numberBigInt;

    protected BigDecimal _numberBigDecimal;

    /**
     * Textual number representation captured from input in cases lazy-parsing
     * is desired.
     *<p>
     * As of 2.14, this only applies to {@link BigInteger} and {@link BigDecimal}.
     *
     * @since 2.14
     */
    protected String _numberString;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public CsvDecoder(CsvParser owner, IOContext ctxt, Reader r, CsvSchema schema,
            TextBuffer textBuffer,
            int stdFeatures, int csvFeatures)
    {
        _owner = owner;
        _ioContext = ctxt;
        _inputReader = r;
        _textBuffer = textBuffer;
        _autoCloseInput =  JsonParser.Feature.AUTO_CLOSE_SOURCE.enabledIn(stdFeatures);
        final boolean oldComments = JsonParser.Feature.ALLOW_YAML_COMMENTS.enabledIn(stdFeatures);
        _allowComments = oldComments | CsvParser.Feature.ALLOW_COMMENTS.enabledIn(csvFeatures);
        _trimSpaces = CsvParser.Feature.TRIM_SPACES.enabledIn(csvFeatures);
        _skipBlankLines = CsvParser.Feature.SKIP_EMPTY_LINES.enabledIn(csvFeatures);
        _inputBuffer = ctxt.allocTokenBuffer();
        _bufferRecyclable = true; // since we allocated it
        _inputReader = r;
        _tokenInputRow = -1;
        _tokenInputCol = -1;
        setSchema(schema);
    }

    public void setSchema(CsvSchema schema)
    {
        _separatorChar = schema.getColumnSeparator();
        _quoteChar = schema.getQuoteChar();
        _escapeChar = schema.getEscapeChar();
        _maxTokenLength = schema.getMaxTokenLength();
        if (!_allowComments) {
            _allowComments = schema.allowsComments();
        }
        int max = Math.max(_separatorChar, _quoteChar);
        max = Math.max(max, _escapeChar);
        max = Math.max(max, '\r');
        max = Math.max(max, '\n');
        _maxSpecialChar = max;
    }

    /**
     * @since 2.7
     */
    public void overrideFormatFeatures(int csvFeatures) {
        _trimSpaces = CsvParser.Feature.TRIM_SPACES.enabledIn(csvFeatures);
        _skipBlankLines = CsvParser.Feature.SKIP_EMPTY_LINES.enabledIn(csvFeatures);

        // 07-Oct-2019, tatu: not 100% accurate, as we have no access to legacy
        //     setting. But close enough, fixed in 3.0
        if (CsvParser.Feature.ALLOW_COMMENTS.enabledIn(csvFeatures)) {
            _allowComments = true;
        }
    }

    /*
    /**********************************************************************
    /* JsonParser implementations passed-through by CsvParser
    /**********************************************************************
     */
    
    public Object getInputSource() {
        return _inputReader;
    }

    public boolean isClosed() { return _closed; }
    
    public void close() throws IOException
    {
        _pendingLF = 1; // just to ensure we'll also check _closed flag later on
        if (!_closed) {
            _closed = true;
            try {
                _closeInput();
            } finally {
                // Also, internal buffer(s) can now be released as well
                releaseBuffers();
            }
        }
    }

    public int releaseBuffered(Writer out) throws IOException
    {
        int count = _inputEnd - _inputPtr;
        if (count < 1) {
            return 0;
        }
        // let's just advance ptr to end
        int origPtr = _inputPtr;
        out.write(_inputBuffer, origPtr, count);
        return count;
    }

    public JsonReadContext childArrayContext(JsonReadContext context)
    {
        int col = _inputPtr - _currInputRowStart + 1; // 1-based
        return context.createChildArrayContext(_currInputRow, col);
    }

    public JsonReadContext childObjectContext(JsonReadContext context)
    {
        int col = _inputPtr - _currInputRowStart + 1; // 1-based
        return context.createChildObjectContext(_currInputRow, col);
    }
    
    public JsonLocation getTokenLocation()
    {
        return new JsonLocation(_ioContext.contentReference(),
                getTokenCharacterOffset(),
                getTokenLineNr(), getTokenColumnNr());
    }

    public JsonLocation getCurrentLocation()
    {
        int ptr = _inputPtr;
        /* One twist: when dealing with a "pending LF", need to
         * go back one position when calculating location
         */
        if (_pendingLF > 1) { // 1 is used as marker for end-of-input
            --ptr;
        }
        int col = ptr - _currInputRowStart + 1; // 1-based
        return new JsonLocation(_ioContext.contentReference(),
                _currInputProcessed + ptr - 1, _currInputRow, col);
    }

    public final int getCurrentRow() {
        return _currInputRow;
    }

    public final int getCurrentColumn() {
        int ptr = _inputPtr;
        // One twist: when dealing with a "pending LF", need to
        // go back one position when calculating location
        if (_pendingLF > 1) { // 1 is used as marker for end-of-input
            --ptr;
        }
        return ptr - _currInputRowStart + 1; // 1-based
    }
    
    /*
    /**********************************************************************
    /* Helper methods, input handling
    /**********************************************************************
     */

    protected final long getTokenCharacterOffset() { return _tokenInputTotal; }
    protected final int getTokenLineNr() { return _tokenInputRow; }
    protected final int getTokenColumnNr() {
        // note: value of -1 means "not available"; otherwise convert from 0-based to 1-based
        int col = _tokenInputCol;
        return (col < 0) ? col : (col + 1);
    }
    
    protected void releaseBuffers() throws IOException
    {
        _textBuffer.releaseBuffers();
        char[] buf = _inputBuffer;
        if (buf != null) {
            _inputBuffer = null;
            _ioContext.releaseTokenBuffer(buf);
        }
    }

    protected void _closeInput() throws IOException
    {
        _pendingLF = 1; // just to ensure we'll also check _closed flag later on

        /* 25-Nov-2008, tatus: As per [JACKSON-16] we are not to call close()
         *   on the underlying Reader, unless we "own" it, or auto-closing
         *   feature is enabled.
         *   One downside is that when using our optimized
         *   Reader (granted, we only do that for UTF-32...) this
         *   means that buffer recycling won't work correctly.
         */
        if (_inputReader != null) {
            if (_autoCloseInput || _ioContext.isResourceManaged()) {
                _inputReader.close();
            }
            _inputReader = null;
        }
    }
    
    protected final boolean loadMore() throws IOException
    {
        _currInputProcessed += _inputEnd;
        _currInputRowStart -= _inputEnd;
        
        if (_inputReader != null) {
            int count = _inputReader.read(_inputBuffer, 0, _inputBuffer.length);
            _inputEnd = count;
            if (count > 0) {
                _inputPtr = 0;
                return true;
            }
            /* End of input; close here --  but note, do NOT yet call releaseBuffers()
             * as there may be buffered input to handle
             */
            _closeInput();
            // Should never return 0, so let's fail
            if (count == 0) {
                throw new IOException("InputStream.read() returned 0 characters when trying to read "+_inputBuffer.length+" bytes");
            }
        }
        return false;
    }

    /*
    /**********************************************************************
    /* Actual parsing, access methods
    /**********************************************************************
     */

    public String getText() throws IOException {
        return _textBuffer.contentsAsString();
    }

    /**
     * Method that can be called to see if there is at least one more
     * character to be parsed.
     */
    public boolean hasMoreInput() throws IOException
    {
        if (_inputPtr < _inputEnd) {
            return true;
        }
        return loadMore();
    }

    /**
     * Method called to handle details of starting a new line, which may
     * include skipping a linefeed.
     * 
     * @return True if there is a new data line to handle; false if not
     */
    public boolean startNewLine() throws IOException
    {
        // first: if pending LF, skip it
        if (_pendingLF != 0) {
            if (_inputReader == null) {
                return false;
            }
            _handleLF();
        }
        return skipLinesWhenNeeded();
    }

    /**
     * Optionally skip lines that are empty or are comments, depending on the feature activated in the parser
     *
     * @return false if the end of input was reached
     *
     * @since 2.10.1
     */
    public boolean skipLinesWhenNeeded() throws IOException {
        if (_allowComments) {
            return _skipCommentLines();
        }
        if (!_skipBlankLines) {
            return hasMoreInput();
        }

        // only need to skip fully empty lines
        while (hasMoreInput()) {
            char ch = _inputBuffer[_inputPtr];
            if (ch == '\r' || ch == '\n') {
                ++_inputPtr;
                _pendingLF = ch;
                _handleLF();
                continue;
            }
            if (ch != ' ') {
                return true; // processing can go on
            }
            ++_inputPtr;
        }
        return false; // end of input
    }

    public boolean _skipCommentLines() throws IOException
    {
        while ((_inputPtr < _inputEnd) || loadMore()) {
            char ch = _inputBuffer[_inputPtr];
            switch (ch) {
            case '#':
                ++_inputPtr;
                _skipCommentContents();
                continue;
            case '\r':
            case '\n':
                ++_inputPtr;
                _pendingLF = ch;
                _handleLF();
                continue;
            case ' ':
                // skip all blanks (in both comments/blanks skip mode)
                ++_inputPtr;
                continue;
            default:
                return true;
            }
        }
        return false; // end of input
    }

    private void _skipCommentContents() throws IOException
    {
        while ((_inputPtr < _inputEnd) || loadMore()) {
            char ch = _inputBuffer[_inputPtr++];
            if (ch == '\r' || ch == '\n') {
                _pendingLF = ch;
                _handleLF();
                break;
            }
        }
    }

    /*
    private final static int INT_HASH = '#';

    protected int _skipCommentLines() throws IOException
    {
        while ((_inputPtr < _inputEnd) || loadMore()) {
            char ch = _inputBuffer[_inputPtr++];
            if (ch >= ' ' || (ch != '\r' && ch != '\n')) {
                continue;
            }
            _pendingLF = ch;
            _handleLF();

            // Ok, skipped the end of the line. Check next one...
            int i = _nextChar();
            if (i != INT_HASH) {
                --_inputPtr;
                return true;
            }
        }
        return -1; // end of input
    }
    */

    /**
     * Method called to blindly skip a single line of content, without considering
     * aspects like quoting or escaping. Used currently simply to skip the first
     * line of input document, if instructed to do so.
     */
    public boolean skipLine() throws IOException
    {
        if (_pendingLF != 0) {
            if (_inputReader == null) {
                return false;
            }
            _handleLF();
        }
        while (_inputPtr < _inputEnd || loadMore()) {
            char c = _inputBuffer[_inputPtr++];
            if (c == '\r' || c == '\n') {
                // important: handle trailing linefeed now, so caller need not bother
                _pendingLF = c;
                _handleLF();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Method called to parse the next token when we don't have any type
     * information, so that all tokens are exposed as basic String
     * values.
     * 
     * @return Column value if more found; null to indicate end of line
     *  of input
     */
    public String nextString() throws IOException
    {
        _numTypesValid = NR_UNKNOWN;
        _tokenLength = 0;
        
        if (_pendingLF > 0) { // either pendingLF, or closed
            if (_inputReader != null) { // if closed, we just need to return null
                _handleLF();
            }
            return null; // end of line without new value
        }
        int i;

        if (_trimSpaces) {
            i = _skipLeadingSpace();
        } else {
            i = _nextChar();
        }
        // First, need to ensure we know the starting location of token
        _tokenInputTotal = _currInputProcessed + _inputPtr - 1;
        _tokenInputRow = _currInputRow;
        _tokenInputCol = _inputPtr - _currInputRowStart - 1;

        if (i < 0) { // EOF at this point signifies empty value
            _textBuffer.resetWithString("");
            return "";
        }

        if (i == INT_CR || i == INT_LF) { // end-of-line means end of record; but also need to handle LF later on
            _pendingLF = i;
            _textBuffer.resetWithString("");
            return "";
        }
        // two modes: quoted, unquoted
        if (i == _quoteChar) { // offline quoted case (longer)
            return _nextQuotedString();
        }
        if (i == _separatorChar) {
            _textBuffer.resetWithString("");
            return "";
        }
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        outBuf[0] = (char) i;
        _tokenLength++;
        int outPtr = 1;

        if (i == _escapeChar) {
            // Reset the escaped character
            outBuf[0] = _unescape();
            return _nextUnquotedString(outBuf, outPtr);
        }

        int ptr = _inputPtr;
        if (ptr >= _inputEnd) {
            if (!loadMore()) { // ok to have end-of-input but...
                return _textBuffer.finishAndReturn(outPtr, _trimSpaces);
            }
            ptr = _inputPtr;
        }
        final int end;
        
        {
            int max = Math.min(_inputEnd - ptr, outBuf.length - outPtr);
            end = ptr + max;
        }

        // handle unquoted case locally if it can be handled without
        // crossing buffer boundary...
        char[] inputBuffer = _inputBuffer;

        while (ptr < end) {
            char c = inputBuffer[ptr++];
            if (c <= _maxSpecialChar) {
                if (c == _separatorChar) { // end of value, yay!
                    _inputPtr = ptr;
                    return _textBuffer.finishAndReturn(outPtr, _trimSpaces);
                }
                if (c == '\r' || c == '\n') {
                    _pendingLF = c;
                    _inputPtr = ptr;
                    return _textBuffer.finishAndReturn(outPtr, _trimSpaces);
                }
                if (c == _escapeChar) {
                    --ptr;
                    break;
                }
            }
            if (_maxTokenLength == -1 || _tokenLength < _maxTokenLength) {
                outBuf[outPtr++] = c;
                _tokenLength++;
            }
        }
        // ok, either input or output across buffer boundary, offline
        _inputPtr = ptr;
        return _nextUnquotedString(outBuf, outPtr);
    }

    public JsonToken nextStringOrLiteral() throws IOException
    {
        _numTypesValid = NR_UNKNOWN;
        // !!! TODO: implement properly
        String value = nextString();
        if (value == null) {
            return null;
        }
        return JsonToken.VALUE_STRING;
    }

    public JsonToken nextNumber() throws IOException
    {
        _numTypesValid = NR_UNKNOWN;
        // !!! TODO: implement properly
        String value = nextString();
        if (value == null) {
            return null;
        }
        return JsonToken.VALUE_STRING;
    }
    public JsonToken nextNumberOrString() throws IOException
    {
        _numTypesValid = NR_UNKNOWN;
        // !!! TODO: implement properly
        String value = nextString();
        if (value == null) {
            return null;
        }
        return JsonToken.VALUE_STRING;
    }
    
    /*
    /**********************************************************************
    /* Actual parsing, private helper methods
    /**********************************************************************
     */
    
    protected String _nextUnquotedString(char[] outBuf, int outPtr) throws IOException
    {
        int c;
        final char[] inputBuffer = _inputBuffer;
        
        main_loop:
        while (true) {
            int ptr = _inputPtr;
            if (ptr >= _inputEnd) {
                if (!loadMore()) { // ok to have end-of-input, are done
                    _inputPtr = ptr;
                    break main_loop;
                }
                ptr = _inputPtr;
            }
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            final int max = Math.min(_inputEnd, (ptr + (outBuf.length - outPtr)));
            while (ptr < max) {
                c = inputBuffer[ptr++];
                if (c <= _maxSpecialChar) {
                    if (c == _separatorChar) { // end of value, yay!
                        _inputPtr = ptr;
                        break main_loop;
                    }
                    if (c == '\r' || c == '\n') { // end of line is end of value as well
                        _inputPtr = ptr;
                        _pendingLF = c;
                        break main_loop;
                    }
                    if (c == _escapeChar) {
                        _inputPtr = ptr;
                        outBuf[outPtr++] = _unescape();
                        // May have passed input boundary, need to re-set
                        continue main_loop;
                    }
                }
                if (_maxTokenLength == -1 || _tokenLength < _maxTokenLength) {
                    outBuf[outPtr++] = (char) c;
                    _tokenLength++;
                }
            }
            _inputPtr = ptr;
        }
        return _textBuffer.finishAndReturn(outPtr, _trimSpaces);
    }
    
    protected String _nextQuotedString() throws IOException
    {
        char[] outBuf = _textBuffer.emptyAndGetCurrentSegment();
        int outPtr = 0;

        final char[] inputBuffer = _inputBuffer;
        boolean checkLF = false; // marker for split CR+LF

        main_loop:
        while (true) {
            int ptr = _inputPtr;
            if (ptr >= _inputEnd) {
                if (!loadMore()) { // not ok, missing end quote
                    _owner._reportParsingError("Missing closing quote for value"); // should indicate start position?
                }
                ptr = _inputPtr;
                if (checkLF && inputBuffer[ptr] == '\n') {
                    // undo earlier advancement, to keep line number correct
                    --_currInputRow;
                }
            }
            if (checkLF) { // had a "hanging" CR in parse loop; check now
            }
            if (outPtr >= outBuf.length) {
                outBuf = _textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            final int max = Math.min(_inputEnd, (ptr + (outBuf.length - outPtr)));

            inner_loop:
            while (true) {
                char c = inputBuffer[ptr++];
                if (c <= _maxSpecialChar) {
                    if (c == _quoteChar) {
                        _inputPtr = ptr;
                        break;
                    }
                    // Embedded linefeeds are fine
                    if (c == '\r') {
                        // bit crappy check but has to do:
                        if (ptr >= max) {
                            checkLF = true; // will need to be checked in beginning of next loop
                            ++_currInputRow;
                            _currInputRowStart = ptr;
                        } else if (inputBuffer[ptr] != '\n') {
                            ++_currInputRow;
                            _currInputRowStart = ptr;
                        }
                    } else if (c == '\n') {
                        ++_currInputRow;
                        _currInputRowStart = ptr;
                    } else if (c == _escapeChar) {
                        _inputPtr = ptr;
                        c = _unescape();
                        if (_maxTokenLength == -1 || _tokenLength < _maxTokenLength) {
                            outBuf[outPtr++] = c;
                            _tokenLength++;
                        }
                        // May have passed input boundary, need to re-set
                        continue main_loop;
                    }
                }
                if (_maxTokenLength == -1 || _tokenLength < _maxTokenLength) {
                    outBuf[outPtr++] = c;
                    _tokenLength++;
                }
                if (ptr >= max) {
                    _inputPtr = ptr;
                    continue main_loop;
                }
                continue inner_loop;
            }
            // We get here if we hit a quote: check if it's doubled up, or end of value:
            if (_inputPtr < _inputEnd || loadMore()) { 
                if (_inputBuffer[_inputPtr] == _quoteChar) { // doubled up, append
                    // note: should have enough room, is safe
                    if (_maxTokenLength == -1 || _tokenLength < _maxTokenLength) {
                        outBuf[outPtr++] = (char) _quoteChar;
                        _tokenLength++;
                    }
                    ++_inputPtr;
                    continue main_loop;
                }
            }
            // Not doubled; leave next char as is
            break;
        }
        // note: do NOT trim from within quoted Strings
        String result = _textBuffer.finishAndReturn(outPtr, false);
        // good, but we also need to locate and skip trailing space, separator
        // (note: space outside quotes never included, but must be skipped)
        while (_inputPtr < _inputEnd || loadMore()) { // end-of-input is fine
            int ch = _inputBuffer[_inputPtr++];
            if (ch == _separatorChar) { // common case, separator between columns
                break;
            }
            if (ch <= INT_SPACE) { // extra space, fine as well
                if (ch == INT_CR || ch == INT_LF) { // but end-of-line can't be yet skipped
                    _pendingLF = ch;
                    break;
                }
                continue;
            }
            _owner._reportUnexpectedCsvChar(ch, String.format(
                    "Expected column separator character (%s) or end-of-line", _getCharDesc(_separatorChar)));
        }
        return result;
    }

    protected final void _handleLF() throws IOException
    {
        // already skipped past first part; but may get \r\n so skip the other char too?
        if (_pendingLF == INT_CR) {
            if (_inputPtr < _inputEnd || loadMore()) {
                if (_inputBuffer[_inputPtr] == '\n') {
                    ++_inputPtr;
                }
            }
        }
        _pendingLF = 0;
        ++_currInputRow;
        _currInputRowStart = _inputPtr;
    }

    protected char _unescape() throws IOException
    {
        if (_inputPtr >= _inputEnd) {
            if (!loadMore()) {
                _reportError("Unexpected EOF in escaped character");
            }
        }
        // Some characters are more special than others, so:
        char c = _inputBuffer[_inputPtr++];
        switch (c) {
        case '0':
            return '\0';
        case 'n':
            return '\n';
        case 'r':
            return '\r';
        case 't':
            return '\t';
        }
        // others, return as is...
        return c;
    }
    
    protected final int _nextChar() throws IOException
    {
        if (_inputPtr >= _inputEnd) {
            if (!loadMore()) {
                return -1;
            }
        }
        return _inputBuffer[_inputPtr++];
    }

    protected final int _skipLeadingSpace() throws IOException
    {
        final int sep = _separatorChar;
        while (true) {
            if (_inputPtr >= _inputEnd) {
                if (!loadMore()) {
                    return -1;
                }
            }
            char ch = _inputBuffer[_inputPtr++];
            if ((ch > ' ') || (ch == sep)) {
                return ch;
            }
            switch (ch) {
            case '\r':
            case '\n':
                return ch;
            }
        }
    }

    /*
    /**********************************************************************
    /* Numeric accessors for CsvParser
    /**********************************************************************
     */

    /**
     * Method used by {@link CsvParser#isExpectedNumberIntToken()} to coerce
     * current token into integer number, if it looks like one.
     *
     * @since 2.12
     */
    public boolean isExpectedNumberIntToken() throws IOException
    {
        if (looksLikeInt()) {
            _parseIntValue();
            return true;
        }
        return false;
    }

    /**
     * @param exact Whether we should try to retain maximum precision or not;
     *    passed as {@code true} by {@code getNumberValueExact()}, and as
     *    {@code false} by regular {@code getNumberValue)}.
     */
    public Number getNumberValue(boolean exact) throws IOException
    {
        if (_numTypesValid == NR_UNKNOWN) {
            _parseNumericValue(exact); // will also check event type
        }
        // Separate types for int types
        if ((_numTypesValid & NR_INT) != 0) {
            return Integer.valueOf(_numberInt);
        }
        if ((_numTypesValid & NR_LONG) != 0) {
            return Long.valueOf(_numberLong);
        }
        if ((_numTypesValid & NR_BIGINT) != 0) {
            return _getBigInteger();
        }
        // And then floating point types. But here optimal type
        // needs to be big decimal, to avoid losing any data?
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            return _getBigDecimal();
        }
        if ((_numTypesValid & NR_DOUBLE) == 0) { // sanity check
            _throwInternal();
        }
        return Double.valueOf(_numberDouble);
    }
    
    public NumberType getNumberType() throws IOException
    {
        if (_numTypesValid == NR_UNKNOWN) {
            _parseNumericValue(false); // will also check event type
        }
        if ((_numTypesValid & NR_INT) != 0) {
            return NumberType.INT;
        }
        if ((_numTypesValid & NR_LONG) != 0) {
            return NumberType.LONG;
        }
        if ((_numTypesValid & NR_BIGINT) != 0) {
            return NumberType.BIG_INTEGER;
        }
    
        // And then floating point types. Here optimal type
        // needs to be big decimal, to avoid losing any data?
        // However... using BD is slow, so let's allow returning
        // double as type if no explicit call has been made to access data as BD?
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            return NumberType.BIG_DECIMAL;
        }
        return NumberType.DOUBLE;
    }
    
    public int getIntValue() throws IOException
    {
        if ((_numTypesValid & NR_INT) == 0) {
            if (_numTypesValid == NR_UNKNOWN) { // not parsed at all
                _parseNumericValue(false); // will also check event type
            }
            if ((_numTypesValid & NR_INT) == 0) { // wasn't an int natively?
                convertNumberToInt(); // let's make it so, if possible
            }
        }
        return _numberInt;
    }
    
    public long getLongValue() throws IOException
    {
        if ((_numTypesValid & NR_LONG) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(false);
            }
            if ((_numTypesValid & NR_LONG) == 0) {
                convertNumberToLong();
            }
        }
        return _numberLong;
    }
    
    public BigInteger getBigIntegerValue() throws IOException
    {
        if ((_numTypesValid & NR_BIGINT) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(true);
            }
            if ((_numTypesValid & NR_BIGINT) == 0) {
                convertNumberToBigInteger();
            }
        }
        return _getBigInteger();
    }
    
    public float getFloatValue() throws IOException
    {
        double value = getDoubleValue();
        // Bounds/range checks would be tricky here, so let's not bother...
        return (float) value;
    }
    
    public double getDoubleValue() throws IOException
    {
        if ((_numTypesValid & NR_DOUBLE) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(false);
            }
            if ((_numTypesValid & NR_DOUBLE) == 0) {
                convertNumberToDouble();
            }
        }
        return _numberDouble;
    }
    
    public BigDecimal getDecimalValue() throws IOException
    {
        if ((_numTypesValid & NR_BIGDECIMAL) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(true);
            }
            if ((_numTypesValid & NR_BIGDECIMAL) == 0) {
                convertNumberToBigDecimal();
            }
        }
        return _getBigDecimal();
    }

    /**
     * Internal accessor that needs to be used for accessing number value of type
     * {@link BigInteger} which -- as of 2.14 -- is typically lazily parsed.
     *
     * @since 2.14
     */
    protected BigInteger _getBigInteger() throws IOException {
        if (_numberBigInt != null) {
            return _numberBigInt;
        } else if (_numberString == null) {
            throw new IllegalStateException("cannot get BigInteger from current parser state");
        }
        _ioContext.streamReadConstraints().validateIntegerLength(_numberString.length());
        _numberBigInt = NumberInput.parseBigInteger(
                _numberString, _owner.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        _numberString = null;
        return _numberBigInt;
    }

    /**
     * Internal accessor that needs to be used for accessing number value of type
     * {@link BigDecimal} which -- as of 2.14 -- is typically lazily parsed.
     *
     * @since 2.14
     */
    protected BigDecimal _getBigDecimal() throws IOException{
        if (_numberBigDecimal != null) {
            return _numberBigDecimal;
        } else if (_numberString == null) {
            throw new IllegalStateException("cannot get BigDecimal from current parser state");
        }
        _ioContext.streamReadConstraints().validateFPLength(_numberString.length());
        _numberBigDecimal = NumberInput.parseBigDecimal(
                _numberString, _owner.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        _numberString = null;
        return _numberBigDecimal;
    }

    /*
    /**********************************************************************
    /* Conversion from textual to numeric representation
    /**********************************************************************
     */

    /**
     * Method that will parse actual numeric value out of a syntactically
     * valid number value. Type it will parse into depends on whether
     * it is a floating point number, as well as its magnitude: smallest
     * legal type (of ones available) is used for efficiency.
     *
     * @param exactNumber Whether to try to retain the highest precision for
     *    floating-point values or not
     */
    protected void _parseNumericValue(boolean exactNumber)
        throws IOException
    {
        // Int or float?
        if (looksLikeInt()) {
            _parseIntValue();
            return;
        }
        /*
        if (_hasFloatToken() == JsonToken.VALUE_NUMBER_FLOAT) {
            _parseSlowFloatValue(expType);
            return;
        }
        _reportError("Current token ("+_currToken+") not numeric, can not use numeric value accessors");
        */
        _parseSlowFloatValue(exactNumber);
    }

    private boolean looksLikeInt() throws IOException {
        final char[] ch = _textBuffer.contentsAsArray();
        final int len = ch.length;

        if (len == 0) {
            return false;
        }

        int i = 0;
        char c = ch[0];
        if (c == '-' || c == '+') {
            if (len == 1) {
                return false;
            }
            ++i;
        }
        for (; i < len; ++i) {
            c = ch[i];
            if (c > '9' || c < '0') {
                return false;
            }
        }
        return true;
    }

    // @since 2.12
    protected void _parseIntValue() throws IOException
    {
        char[] buf = _textBuffer.getTextBuffer();
        int offset = _textBuffer.getTextOffset();
        char c = buf[offset];
        boolean neg;

        if (c == '-') {
            neg = true;
            ++offset;
        } else {
            neg = false;
            if (c == '+') {
                ++offset;
            }
        }
        int len = buf.length - offset;
        if (len <= 9) { // definitely fits in int
            int i = NumberInput.parseInt(buf, offset, len);
            _numberInt = neg ? -i : i;
            _numTypesValid = NR_INT;
            return;
        }
        if (len <= 18) { // definitely fits AND is easy to parse using 2 int parse calls
            long l = NumberInput.parseLong(buf, offset, len);
            if (neg) {
                l = -l;
            }
            // [JACKSON-230] Could still fit in int, need to check
            if (len == 10) {
                if (neg) {
                    if (l >= MIN_INT_L) {
                        _numberInt = (int) l;
                        _numTypesValid = NR_INT;
                        return;
                    }
                } else {
                    if (l <= MAX_INT_L) {
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
        _parseSlowIntValue(buf, offset, len, neg);
    }
    
    private final void _parseSlowFloatValue(boolean exactNumber)
        throws IOException
    {
        /* Nope: floating point. Here we need to be careful to get
         * optimal parsing strategy: choice is between accurate but
         * slow (BigDecimal) and lossy but fast (Double). For now
         * let's only use BD when explicitly requested -- it can
         * still be constructed correctly at any point since we do
         * retain textual representation
         */
        try {
            if (exactNumber) {
                _numberBigDecimal = null;
                _numberString = _textBuffer.contentsAsString();
                _numTypesValid = NR_BIGDECIMAL;
            } else {
                // Otherwise double has to do
                _numberDouble = _textBuffer.contentsAsDouble(_owner.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
                _numTypesValid = NR_DOUBLE;
            }
        } catch (NumberFormatException nex) {
            // Can this ever occur? Due to overflow, maybe?
            throw constructError("Malformed numeric value '"+_textBuffer.contentsAsString()+"'", nex);
        }
    }

    private final void _parseSlowIntValue(char[] buf, int offset, int len,
            boolean neg)
        throws IOException
    {
        String numStr = _textBuffer.contentsAsString();
        try {
            if (NumberInput.inLongRange(buf, offset, len, neg)) {
                // Probably faster to construct a String, call parse, than to use BigInteger
                _numberLong = Long.parseLong(numStr);
                _numTypesValid = NR_LONG;
            } else {
                // nope, need the heavy guns... (rare case)
                _numberBigInt = null;
                _numberString = numStr;
                _numTypesValid = NR_BIGINT;
            }
        } catch (NumberFormatException nex) {
            // Can this ever occur? Due to overflow, maybe?
            throw constructError("Malformed numeric value '"+numStr+"'", nex);
        }
    }

    /*
    /**********************************************************************
    /* Numeric conversions
    /**********************************************************************
     */    

    protected void convertNumberToInt() throws IOException
    {
        // First, converting from long ought to be easy
        if ((_numTypesValid & NR_LONG) != 0) {
            // Let's verify it's lossless conversion by simple roundtrip
            int result = (int) _numberLong;
            if (result != _numberLong) {
                _reportError("Numeric value ("+getText()+") out of range of int");
            }
            _numberInt = result;
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            // !!! Should check for range...
            _numberInt = _getBigInteger().intValue();
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            // Need to check boundaries
            if (_numberDouble < MIN_INT_D || _numberDouble > MAX_INT_D) {
                reportOverflowInt();
            }
            _numberInt = (int) _numberDouble;
        } else if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            final BigDecimal bigDecimal = _getBigDecimal();
            if (BD_MIN_INT.compareTo(bigDecimal) > 0
                || BD_MAX_INT.compareTo(bigDecimal) < 0) {
                reportOverflowInt();
            }
            _numberInt = bigDecimal.intValue();
        } else {
            _throwInternal(); // should never get here
        }
    
        _numTypesValid |= NR_INT;
    }
    
    protected void convertNumberToLong() throws IOException
    {
        if ((_numTypesValid & NR_INT) != 0) {
            _numberLong = _numberInt;
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            // !!! Should check for range...
            _numberLong = _getBigInteger().longValue();
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            // Need to check boundaries
            if (_numberDouble < MIN_LONG_D || _numberDouble > MAX_LONG_D) {
                reportOverflowLong();
            }
            _numberLong = (long) _numberDouble;
        } else if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            final BigDecimal bigDecimal = _getBigDecimal();
            if (BD_MIN_LONG.compareTo(bigDecimal) > 0
                || BD_MAX_LONG.compareTo(bigDecimal) < 0) {
                reportOverflowLong();
            }
            _numberLong = bigDecimal.longValue();
        } else {
            _throwInternal(); // should never get here
        }
    
        _numTypesValid |= NR_LONG;
    }
    
    protected void convertNumberToBigInteger()
        throws IOException
    {
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            // here it'll just get truncated, no exceptions thrown
            final BigDecimal bd = _getBigDecimal();
            _ioContext.streamReadConstraints().validateBigIntegerScale(bd.scale());
            _numberBigInt = bd.toBigInteger();
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberBigInt = BigInteger.valueOf(_numberLong);
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberBigInt = BigInteger.valueOf(_numberInt);
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            _numberBigInt = BigDecimal.valueOf(_numberDouble).toBigInteger();
        } else {
            _throwInternal(); // should never get here
        }
        _numTypesValid |= NR_BIGINT;
    }
    
    protected void convertNumberToDouble()
        throws IOException
    {
        /* 05-Aug-2008, tatus: Important note: this MUST start with
         *   more accurate representations, since we don't know which
         *   value is the original one (others get generated when
         *   requested)
         */
    
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            _numberDouble = _getBigDecimal().doubleValue();
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            _numberDouble = _getBigInteger().doubleValue();
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberDouble = _numberLong;
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberDouble = _numberInt;
        } else {
            _throwInternal(); // should never get here
        }
    
        _numTypesValid |= NR_DOUBLE;
    }
    
    protected void convertNumberToBigDecimal() throws IOException
    {
        if ((_numTypesValid & NR_DOUBLE) != 0) {
            /* Let's actually parse from String representation, to avoid
             * rounding errors that non-decimal floating operations could incur
             */
            final String text = getText();
            _ioContext.streamReadConstraints().validateFPLength(text.length());
            _numberBigDecimal = NumberInput.parseBigDecimal(
                    text, _owner.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            _numberBigDecimal = new BigDecimal(_getBigInteger());
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberBigDecimal = BigDecimal.valueOf(_numberLong);
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberBigDecimal = BigDecimal.valueOf(_numberInt);
        } else {
            _throwInternal(); // should never get here
        }
        _numTypesValid |= NR_BIGDECIMAL;
    }

    /*
    /**********************************************************
    /* Number handling exceptions
    /**********************************************************
     */    
    
    protected void reportUnexpectedNumberChar(int ch, String comment)
        throws JsonParseException
    {
        String msg = "Unexpected character ("+_getCharDesc(ch)+") in numeric value";
        if (comment != null) {
            msg += ": "+comment;
        }
        _reportError(msg);
    }
    
    protected void reportInvalidNumber(String msg) throws JsonParseException {
        _reportError("Invalid numeric value: "+msg);
    }
    
    protected void reportOverflowInt() throws IOException {
        _reportError("Numeric value ("+getText()+") out of range of int ("+Integer.MIN_VALUE+" - "+Integer.MAX_VALUE+")");
    }
    
    protected void reportOverflowLong() throws IOException {
        _reportError("Numeric value ("+getText()+") out of range of long ("+Long.MIN_VALUE+" - "+Long.MAX_VALUE+")");
    }

    protected final JsonParseException constructError(String msg, Throwable t) {
        return new JsonParseException(_owner, msg, t);
    }
    
    protected final static String _getCharDesc(int ch)
    {
        char c = (char) ch;
        if (Character.isISOControl(c)) {
            return "(CTRL-CHAR, code "+ch+")";
        }
        if (ch > 255) {
            return "'"+c+"' (code "+ch+" / 0x"+Integer.toHexString(ch)+")";
        }
        return "'"+c+"' (code "+ch+")";
    }
    
    private void _throwInternal() {
        throw new IllegalStateException("Internal error: code path should never get executed");
    }

    /**
     * Method for reporting low-level decoding (parsing) problems
     */
    protected final void _reportError(String msg) throws JsonParseException {
        throw new JsonParseException(_owner, msg);
    }
}
