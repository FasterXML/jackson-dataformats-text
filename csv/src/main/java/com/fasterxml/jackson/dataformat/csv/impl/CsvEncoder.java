package com.fasterxml.jackson.dataformat.csv.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * Helper class that handles actual low-level construction of
 * CSV output, based only on indexes given without worrying about reordering,
 * or binding from logical properties.
 */
public class CsvEncoder
{
    /* As an optimization we try coalescing short writes into
     * buffer; but pass longer directly.
     */
    final protected static int SHORT_WRITE = 32;

    /* Also: only do check for optional quotes for short
     * values; longer ones will always be quoted.
     */
    final protected static int MAX_QUOTE_CHECK = 24;
    
    final protected BufferedValue[] NO_BUFFERED = new BufferedValue[0];

    private final static char[] TRUE_CHARS = "true".toCharArray();
    private final static char[] FALSE_CHARS = "false".toCharArray();
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    final protected IOContext _ioContext;

    /**
     * Underlying {@link Writer} used for output.
     */
    final protected Writer _out;
    
    final protected char _cfgColumnSeparator;

    final protected int _cfgQuoteCharacter;

    /**
     * @since 2.7
     */
    final protected int _cfgEscapeCharacter;
    
    final protected char[] _cfgLineSeparator;

    /**
     * @since 2.5
     */
    final protected char[] _cfgNullValue;
    
    final protected int _cfgLineSeparatorLength;

    protected int _cfgMaxQuoteCheckChars;
    
    /**
     * Lowest-valued character that is safe to output without using
     * quotes around value, NOT including possible escape character.
     */
    final protected int _cfgMinSafeChar;

    protected int _csvFeatures;

    /**
     * Marker flag used to determine if to do optimal (aka "strict") quoting
     * checks or not (looser conservative check)
     * 
     * @since 2.4
     */
    protected boolean _cfgOptimalQuoting;

    /**
     * @since 2.4
     */
    protected boolean _cfgIncludeMissingTail;

    /**
     * @since 2.5
     */
    protected boolean _cfgAlwaysQuoteStrings;

    protected boolean _cfgAlwaysQuoteEmptyStrings;

    protected boolean _cfgEscapeQuoteCharWithEscapeChar;

    protected final char _cfgQuoteCharEscapeChar;

    /*
    /**********************************************************
    /* Output state
    /**********************************************************
     */

    /**
     * @since 2.4
     */
    protected int _columnCount;
    
    /**
     * Index of column we expect to write next
     */
    protected int _nextColumnToWrite = 0;

    /**
     * And if output comes in shuffled order we will need to do 
     * bit of ordering.
     */
    protected BufferedValue[] _buffered = NO_BUFFERED;

    /**
     * Index of the last buffered value
     */
    protected int _lastBuffered = -1;
    
    /*
    /**********************************************************
    /* Output buffering, low-level
    /**********************************************************
     */

    /**
     * Intermediate buffer in which contents are buffered before
     * being written using {@link #_out}.
     */
    protected char[] _outputBuffer;

    /**
     * Flag that indicates whether the <code>_outputBuffer</code> is recyclable (and
     * needs to be returned to recycler once we are done) or not.
     */
    protected boolean _bufferRecyclable;
    
    /**
     * Pointer to the next available char position in {@link #_outputBuffer}
     */
    protected int _outputTail = 0;

    /**
     * Offset to index after the last valid index in {@link #_outputBuffer}.
     * Typically same as length of the buffer.
     */
    protected final int _outputEnd;
    
    /**
     * Let's keep track of how many bytes have been output, may prove useful
     * when debugging. This does <b>not</b> include bytes buffered in
     * the output buffer, just bytes that have been written using underlying
     * stream writer.
     */
    protected int _charsWritten;

    /*
    /**********************************************************
    /* Construction, (re)configuration
    /**********************************************************
     */

    public CsvEncoder(IOContext ctxt, int csvFeatures, Writer out, CsvSchema schema)
    {
        _ioContext = ctxt;
        _csvFeatures = csvFeatures;
        _cfgOptimalQuoting = CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING.enabledIn(csvFeatures);
        _cfgIncludeMissingTail = !CsvGenerator.Feature.OMIT_MISSING_TAIL_COLUMNS.enabledIn(_csvFeatures);
        _cfgAlwaysQuoteStrings = CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS.enabledIn(csvFeatures);
        _cfgAlwaysQuoteEmptyStrings = CsvGenerator.Feature.ALWAYS_QUOTE_EMPTY_STRINGS.enabledIn(csvFeatures);
        _cfgEscapeQuoteCharWithEscapeChar = CsvGenerator.Feature.ESCAPE_QUOTE_CHAR_WITH_ESCAPE_CHAR.enabledIn(csvFeatures);

        _outputBuffer = ctxt.allocConcatBuffer();
        _bufferRecyclable = true;
        _outputEnd = _outputBuffer.length;
        _out = out;

        _cfgColumnSeparator = schema.getColumnSeparator();
        _cfgQuoteCharacter = schema.getQuoteChar();
        _cfgEscapeCharacter = schema.getEscapeChar();
        _cfgLineSeparator = schema.getLineSeparator();
        _cfgLineSeparatorLength = (_cfgLineSeparator == null) ? 0 : _cfgLineSeparator.length;
        _cfgNullValue = schema.getNullValueOrEmpty();
        
        _columnCount = schema.size();

        _cfgMinSafeChar = _calcSafeChar();

        _cfgMaxQuoteCheckChars = MAX_QUOTE_CHECK;

        _cfgQuoteCharEscapeChar = _getQuoteCharEscapeChar(
          _cfgEscapeQuoteCharWithEscapeChar,
          _cfgQuoteCharacter,
          _cfgEscapeCharacter
        );
    }

    public CsvEncoder(CsvEncoder base, CsvSchema newSchema)
    {
        _ioContext = base._ioContext;
        _csvFeatures = base._csvFeatures;
        _cfgOptimalQuoting = base._cfgOptimalQuoting;
        _cfgIncludeMissingTail = base._cfgIncludeMissingTail;
        _cfgAlwaysQuoteStrings = base._cfgAlwaysQuoteStrings;
        _cfgAlwaysQuoteEmptyStrings = base._cfgAlwaysQuoteEmptyStrings;
        _cfgEscapeQuoteCharWithEscapeChar = base._cfgEscapeQuoteCharWithEscapeChar;

        _outputBuffer = base._outputBuffer;
        _bufferRecyclable = base._bufferRecyclable;
        _outputEnd = base._outputEnd;
        _out = base._out;
        _cfgMaxQuoteCheckChars = base._cfgMaxQuoteCheckChars;

        _cfgColumnSeparator = newSchema.getColumnSeparator();
        _cfgQuoteCharacter = newSchema.getQuoteChar();
        _cfgEscapeCharacter = newSchema.getEscapeChar();
        _cfgLineSeparator = newSchema.getLineSeparator();
        _cfgLineSeparatorLength = _cfgLineSeparator.length;
        _cfgNullValue = newSchema.getNullValueOrEmpty();
        _cfgMinSafeChar = _calcSafeChar();
        _columnCount = newSchema.size();
        _cfgQuoteCharEscapeChar = _getQuoteCharEscapeChar(
          base._cfgEscapeQuoteCharWithEscapeChar,
          newSchema.getQuoteChar(),
          newSchema.getEscapeChar()
        );
    }

    private final char _getQuoteCharEscapeChar(
        final boolean escapeQuoteCharWithEscapeChar,
        final int quoteCharacter,
        final int escapeCharacter) {

      final char quoteEscapeChar;

      if (_cfgEscapeQuoteCharWithEscapeChar && _cfgEscapeCharacter > 0) {
        quoteEscapeChar = (char) _cfgEscapeCharacter;
      }
      else if (_cfgQuoteCharacter > 0) {
        quoteEscapeChar = (char) _cfgQuoteCharacter;
      }
      else {
        quoteEscapeChar = '\\';
      }

      return quoteEscapeChar;
    }

    private final int _calcSafeChar()
    {
        // note: quote char may be -1 to signify "no quoting":
        int min = Math.max(_cfgColumnSeparator, _cfgQuoteCharacter);
        // 06-Nov-2015, tatu: We will NOT apply escape character, because it usually
        //    has higher ascii value (with backslash); better handle separately.
        for (int i = 0; i < _cfgLineSeparatorLength; ++i) {
            min = Math.max(min, _cfgLineSeparator[i]);
        }
        return min+1;
    }

    public CsvEncoder withSchema(CsvSchema schema) {
        return new CsvEncoder(this, schema);
    }

    public CsvEncoder overrideFormatFeatures(int feat) {
        if (feat != _csvFeatures) {
            _csvFeatures = feat;
            _cfgOptimalQuoting = CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING.enabledIn(feat);
            _cfgIncludeMissingTail = !CsvGenerator.Feature.OMIT_MISSING_TAIL_COLUMNS.enabledIn(feat);
            _cfgAlwaysQuoteStrings = CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS.enabledIn(feat);
            _cfgAlwaysQuoteEmptyStrings = CsvGenerator.Feature.ALWAYS_QUOTE_EMPTY_STRINGS.enabledIn(feat);
            _cfgEscapeQuoteCharWithEscapeChar = CsvGenerator.Feature.ESCAPE_QUOTE_CHAR_WITH_ESCAPE_CHAR.enabledIn(feat);
        }
        return this;
    }

    /*
    /**********************************************************
    /* Read-access to output state
    /**********************************************************
     */

    public Object getOutputTarget() {
        return _out;
    }

    /**
     * NOTE: while value does indeed indicate amount that has been written in the buffer,
     * there may be more intermediate data that is buffered as values but not yet in
     * buffer.
     */
    public int getOutputBuffered() {
        return _outputTail;
    }

    public int nextColumnIndex() {
        return _nextColumnToWrite;
    }

    /*
    /**********************************************************
    /* Writer API, writes from generator
    /**********************************************************
     */

    public final void write(int columnIndex, String value) throws IOException
    {
        // easy case: all in order
        if (columnIndex == _nextColumnToWrite) {
            // inlined 'appendValue(String)`
            if (_outputTail >= _outputEnd) {
                _flushBuffer();
            }
            if (_nextColumnToWrite > 0) {
                appendColumnSeparator();
            }
            final int len = value.length();
            if (_cfgAlwaysQuoteStrings || _mayNeedQuotes(value, len)) {
                if (_cfgEscapeCharacter > 0) {
                    _writeQuotedAndEscaped(value, (char) _cfgEscapeCharacter);
                } else {
                    _writeQuoted(value);
                }
            } else {
                writeRaw(value);
            }
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.buffered(value));
    }

    public final void write(int columnIndex, char[] ch, int offset, int len) throws IOException
    {
        // !!! TODO: optimize
        write(columnIndex, new String(ch, offset, len));
    }

    public final void write(int columnIndex, int value) throws IOException
    {
        // easy case: all in order
        if (columnIndex == _nextColumnToWrite) {
            // inlined 'appendValue(int)'
            // up to 10 digits and possible minus sign, leading comma
            if ((_outputTail + 12) > _outputTail) {
                _flushBuffer();
            }
            if (_nextColumnToWrite > 0) {
                _outputBuffer[_outputTail++] = _cfgColumnSeparator;
            }
            _outputTail = NumberOutput.outputInt(value, _outputBuffer, _outputTail);
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.buffered(value));
    }

    public final void write(int columnIndex, long value) throws IOException
    {
        // easy case: all in order
        if (columnIndex == _nextColumnToWrite) {
            // inlined 'appendValue(int)'
            // up to 20 digits, minus sign, leading comma
            if ((_outputTail + 22) > _outputTail) {
                _flushBuffer();
            }
            if (_nextColumnToWrite > 0) {
                _outputBuffer[_outputTail++] = _cfgColumnSeparator;
            }
            _outputTail = NumberOutput.outputLong(value, _outputBuffer, _outputTail);
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.buffered(value));
    }

    public final void write(int columnIndex, float value) throws IOException
    {
        // easy case: all in order
        if (columnIndex == _nextColumnToWrite) {
            appendValue(value);
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.buffered(value));
    }

    public final void write(int columnIndex, double value) throws IOException
    {
        // easy case: all in order
        if (columnIndex == _nextColumnToWrite) {
            appendValue(value);
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.buffered(value));
    }

    public final void write(int columnIndex, boolean value) throws IOException
    {
        // easy case: all in order
        if (columnIndex == _nextColumnToWrite) {
            appendValue(value);
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.buffered(value));
    }

    /**
     * @since 2.5
     */
    public final void writeNonEscaped(int columnIndex, String rawValue) throws IOException
    {
        if (columnIndex == _nextColumnToWrite) {
            appendRawValue(rawValue);
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.bufferedRaw(rawValue));
    }
        
    public final void writeNull(int columnIndex) throws IOException
    {
        if (columnIndex == _nextColumnToWrite) {
            appendNull();
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.bufferedNull());
    }

    public final void writeColumnName(String name) throws IOException
    {
        appendValue(name);
        ++_nextColumnToWrite;
    }

    public void endRow() throws IOException
    {
        // First things first; any buffered?
        if (_lastBuffered >= 0) {
            final int last = _lastBuffered;
            _lastBuffered = -1;
            for (; _nextColumnToWrite <= last; ++_nextColumnToWrite) {
                BufferedValue value = _buffered[_nextColumnToWrite];
                if (value != null) {
                    _buffered[_nextColumnToWrite] = null;
                    value.write(this);
                } else if (_nextColumnToWrite > 0) { // ) {
                    // note: write method triggers prepending of separator; but for missing
                    // values we need to do it explicitly.
                    appendColumnSeparator();
                } 
            }
        } else if (_nextColumnToWrite <= 0) { // empty line; do nothing
            return;
        }
        // Any missing values?
        if (_nextColumnToWrite < _columnCount) {
            if (_cfgIncludeMissingTail) {
                do {
                    appendColumnSeparator();
                } while (++_nextColumnToWrite < _columnCount);
            }
        }
        // write line separator
        _nextColumnToWrite = 0;
        if ((_outputTail + _cfgLineSeparatorLength) > _outputEnd) {
            _flushBuffer();
        }
        System.arraycopy(_cfgLineSeparator, 0, _outputBuffer, _outputTail, _cfgLineSeparatorLength);
        _outputTail += _cfgLineSeparatorLength;
    }
    
    /*
    /**********************************************************
    /* Writer API, writes via buffered values
    /**********************************************************
     */

    protected void appendValue(String value) throws IOException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        if (_nextColumnToWrite > 0) {
            appendColumnSeparator();
        }
        /* First: determine if we need quotes; simple heuristics;
         * only check for short Strings, stop if something found
         */
        final int len = value.length();
        if (_cfgAlwaysQuoteStrings || _mayNeedQuotes(value, len)) {
            if (_cfgEscapeCharacter > 0) {
                _writeQuotedAndEscaped(value, (char) _cfgEscapeCharacter);
            } else {
                _writeQuoted(value);
            }
        } else {
            writeRaw(value);
        }
    }

    protected void appendRawValue(String value) throws IOException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        if (_nextColumnToWrite > 0) {
            appendColumnSeparator();
        }
        writeRaw(value);
    }

    protected void appendValue(int value) throws IOException
    {
        // up to 10 digits and possible minus sign, leading comma
        if ((_outputTail + 12) > _outputEnd) {
            _flushBuffer();
        }
        if (_nextColumnToWrite > 0) {
            _outputBuffer[_outputTail++] = _cfgColumnSeparator;
        }
        _outputTail = NumberOutput.outputInt(value, _outputBuffer, _outputTail);
    }

    protected void appendValue(long value) throws IOException
    {
        // up to 20 digits, minus sign, leading comma
        if ((_outputTail + 22) > _outputEnd) {
            _flushBuffer();
        }
        if (_nextColumnToWrite > 0) {
            _outputBuffer[_outputTail++] = _cfgColumnSeparator;
        }
        _outputTail = NumberOutput.outputLong(value, _outputBuffer, _outputTail);
    }

    protected void appendValue(float value) throws IOException
    {
        String str = NumberOutput.toString(value);
        final int len = str.length();
        if ((_outputTail + len) >= _outputEnd) { // >= to include possible comma too
            _flushBuffer();
        }
        if (_nextColumnToWrite > 0) {
            _outputBuffer[_outputTail++] = _cfgColumnSeparator;
        }
        writeRaw(str);
    }

    protected void appendValue(double value) throws IOException
    {
        String str = NumberOutput.toString(value);
        final int len = str.length();
        if ((_outputTail + len) >= _outputEnd) { // >= to include possible comma too
            _flushBuffer();
        }
        if (_nextColumnToWrite > 0) {
            _outputBuffer[_outputTail++] = _cfgColumnSeparator;
        }
        writeRaw(str);
    }

    protected void appendValue(boolean value) throws IOException {
        _append(value ? TRUE_CHARS : FALSE_CHARS);
    }

    protected void appendNull() throws IOException {
        _append(_cfgNullValue);
    }

    protected void _append(char[] ch) throws IOException {
        final int len = ch.length;
        if ((_outputTail + len) >= _outputEnd) { // >= to include possible comma too
            _flushBuffer();
        }
        if (_nextColumnToWrite > 0) {
            _outputBuffer[_outputTail++] = _cfgColumnSeparator;
        }
        if (len > 0) {
            System.arraycopy(ch, 0, _outputBuffer, _outputTail, len);
        }
        _outputTail += len;
    }
    
    protected void appendColumnSeparator() throws IOException {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = _cfgColumnSeparator;
    }

    /*
    /**********************************************************
    /* Output methods, unprocessed ("raw")
    /**********************************************************
     */
    
    public void writeRaw(String text) throws IOException
    {
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
            writeRawLong(text);
        }
    }

    public void writeRaw(String text, int start, int len) throws IOException
    {
        // Nothing to check, can just output as is
        int room = _outputEnd - _outputTail;

        if (room < len) {
            _flushBuffer();
            room = _outputEnd - _outputTail;
        }
        // But would it nicely fit in? If yes, it's easy
        if (room >= len) {
            text.getChars(start, start+len, _outputBuffer, _outputTail);
            _outputTail += len;
        } else {                
            writeRawLong(text.substring(start, start+len));
        }
    }

    public void writeRaw(char[] text, int offset, int len) throws IOException
    {
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

    public void writeRaw(char c) throws IOException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = c;
    }

    private void writeRawLong(String text) throws IOException
    {
        int room = _outputEnd - _outputTail;
        // If not, need to do it by looping
        text.getChars(0, room, _outputBuffer, _outputTail);
        _outputTail += room;
        _flushBuffer();
        int offset = room;
        int len = text.length() - room;

        while (len > _outputEnd) {
            int amount = _outputEnd;
            text.getChars(offset, offset+amount, _outputBuffer, 0);
            _outputTail = amount;
            _flushBuffer();
            offset += amount;
            len -= amount;
        }
        // And last piece (at most length of buffer)
        text.getChars(offset, offset+len, _outputBuffer, 0);
        _outputTail = len;
    }

    /*
    /**********************************************************
    /* Output methods, with quoting and escaping
    /**********************************************************
     */

    public void _writeQuoted(String text) throws IOException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        // NOTE: caller should guarantee quote char is valid (not -1) at this point:
        final char q = (char) _cfgQuoteCharacter;
        _outputBuffer[_outputTail++] = q;
        // simple case: if we have enough room, no need for boundary checks
        final int len = text.length();
        if ((_outputTail + len + len) >= _outputEnd) {
            _writeLongQuoted(text, q);
            return;
        }
        // 22-Jan-2015, tatu: Common case is that of no quoting needed, so let's
        //     make a speculative copy, then scan
        // 06-Nov-2015, tatu: Not sure if copy actually improves perf; it did with
        //   older JVMs (1.5 at least), but not sure about 1.8 and later
        final char[] buf = _outputBuffer;
        int ptr = _outputTail;

        text.getChars(0, len, buf, ptr);

        final int end = ptr+len;
        
        for (; ptr < end && buf[ptr] != q; ++ptr) { }

        if (ptr == end) { // all good, no quoting or escaping!
            _outputBuffer[ptr] = q;
            _outputTail = ptr+1;
        } else { // doh. do need quoting
            _writeQuoted(text, q, ptr - _outputTail);
        }
    }

    protected void _writeQuoted(String text, char q, int i) throws IOException
    {
        final char[] buf = _outputBuffer;
        _outputTail += i;
        final int len = text.length();
        for (; i < len; ++i) {
            char c = text.charAt(i);
            if (c == q) { // double up
                if (_outputTail >= _outputEnd) {
                    _flushBuffer();
                }
                buf[_outputTail++] = _cfgQuoteCharEscapeChar;
            }
            if (_outputTail >= _outputEnd) {
                _flushBuffer();
            }
            buf[_outputTail++] = c;
        }
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        buf[_outputTail++] = q;
    }

    private final void _writeLongQuoted(String text, char q) throws IOException
    {
        final int len = text.length();
        for (int i = 0; i < len; ++i) {
            if (_outputTail >= _outputEnd) {
                _flushBuffer();
            }
            char c = text.charAt(i);
            if (c == q) { // double up
                _outputBuffer[_outputTail++] = _cfgQuoteCharEscapeChar;
                if (_outputTail >= _outputEnd) {
                    _flushBuffer();
                }
            }
            _outputBuffer[_outputTail++] = c;
        }
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = q;
    }

    public void _writeQuotedAndEscaped(String text, char esc) throws IOException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        // NOTE: caller should guarantee quote char is valid (not -1) at this point:
        final char q = (char) _cfgQuoteCharacter;
        _outputBuffer[_outputTail++] = q;
        final int len = text.length();
        if ((_outputTail + len + len) >= _outputEnd) {
            _writeLongQuotedAndEscaped(text, esc);
            return;
        }
        final char[] buf = _outputBuffer;
        int ptr = _outputTail;

        text.getChars(0, len, buf, ptr);

        final int end = ptr+len;
        for (; ptr < end; ++ptr) {
            char c = buf[ptr];
            if ((c == q) || (c == esc)) {
                break;
            }
        }

        if (ptr == end) { // all good, no quoting or escaping!
            _outputBuffer[ptr] = q;
            _outputTail = ptr+1;
        } else { // quoting AND escaping
            _writeQuotedAndEscaped(text, q, esc, ptr - _outputTail);
        }
    }

    protected void _writeQuotedAndEscaped(String text, char q, char esc, int i) throws IOException
    {
        final char[] buf = _outputBuffer;
        _outputTail += i;
        final int len = text.length();
        for (; i < len; ++i) {
            char c = text.charAt(i);
            if ((c == q) || (c == esc)) { // double up, either way
                if (_outputTail >= _outputEnd) {
                    _flushBuffer();
                }
                buf[_outputTail++] = (c == q) ? _cfgQuoteCharEscapeChar : c;
            }
            if (_outputTail >= _outputEnd) {
                _flushBuffer();
            }
            buf[_outputTail++] = c;
        }
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        buf[_outputTail++] = q;
    }
    
    private final void _writeLongQuotedAndEscaped(String text, char esc) throws IOException
    {
        final int len = text.length();
        // NOTE: caller should guarantee quote char is valid (not -1) at this point:
        final char q = (char) _cfgQuoteCharacter;
        final char quoteEscape = _cfgEscapeQuoteCharWithEscapeChar ? esc : q;
        for (int i = 0; i < len; ++i) {
            if (_outputTail >= _outputEnd) {
                _flushBuffer();
            }
            char c = text.charAt(i);
            if ((c == q) || (c == esc)) { // double up, either way
                _outputBuffer[_outputTail++] = (c == q) ? quoteEscape : c;
                if (_outputTail >= _outputEnd) {
                    _flushBuffer();
                }
            }
            _outputBuffer[_outputTail++] = c;
        }
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = q;
    }

    /*
    /**********************************************************
    /* Writer API, state changes
    /**********************************************************
     */
    
    public void flush(boolean flushStream) throws IOException
    {
        _flushBuffer();
        if (flushStream) {
            _out.flush();
        }
    }

    public void close(boolean autoClose) throws IOException
    {
        _flushBuffer();
        if (autoClose) {
            _out.close();
        } else {
            // If we can't close it, we should at least flush
            _out.flush();
        }
        // Internal buffer(s) generator has can now be released as well
        _releaseBuffers();
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    /**
     * Helper method that determines whether given String is likely
     * to require quoting; check tries to optimize for speed.
     */
    protected boolean _mayNeedQuotes(String value, int length)
    {
        // 21-Mar-2014, tatu: If quoting disabled, don't quote
        if (_cfgQuoteCharacter < 0) {
            return false;
        }
        // may skip checks unless we want exact checking
        if (_cfgOptimalQuoting) {
            if (_cfgEscapeCharacter > 0) {
                return _needsQuotingStrict(value, _cfgEscapeCharacter);
            }
            return _needsQuotingStrict(value);
        }
        if (length > _cfgMaxQuoteCheckChars) {
            return true;
        }
        if (_cfgEscapeCharacter > 0) {
            return _needsQuotingLoose(value, _cfgEscapeCharacter);
        }
        if (_cfgAlwaysQuoteEmptyStrings && length == 0) {
            return true;
        }
        return _needsQuotingLoose(value);
    }

    /**
     *<p>
     * NOTE: final since checking is not expected to be changed here; override
     * calling method (<code>_mayNeedQuotes</code>) instead, if necessary.
     * 
     * @since 2.4
     */
    protected final boolean _needsQuotingLoose(String value)
    {
        for (int i = 0, len = value.length(); i < len; ++i) {
            if (value.charAt(i) < _cfgMinSafeChar) {
                return true;
            }
        }
        return false;
    }

    protected final boolean _needsQuotingLoose(String value, int esc)
    {
        for (int i = 0, len = value.length(); i < len; ++i) {
            int ch = value.charAt(i);
            if ((ch < _cfgMinSafeChar) || (ch == esc)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @since 2.4
     */
    protected boolean _needsQuotingStrict(String value)
    {
        final int minSafe = _cfgMinSafeChar;
        for (int i = 0, len = value.length(); i < len; ++i) {
            int c = value.charAt(i);
            if (c < minSafe) {
                if (c == _cfgColumnSeparator || c == _cfgQuoteCharacter
                        || c == '\r' || c == '\n'
                        // 31-Dec-2014, tatu: Comment lines start with # so quote if starts with #
                        || (c == '#' && i == 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @since 2.7
     */
    protected boolean _needsQuotingStrict(String value, int esc)
    {
        final int minSafe = _cfgMinSafeChar;
        for (int i = 0, len = value.length(); i < len; ++i) {
            int c = value.charAt(i);
            if (c < minSafe) {
                if (c == _cfgColumnSeparator || c == _cfgQuoteCharacter
                        || c == '\r' || c == '\n'
                        // 31-Dec-2014, tatu: Comment lines start with # so quote if starts with #
                        || (c == '#' && i == 0)) {
                    return true;
                }
            } else if (c == esc) {
                return true;
            }
        }
        return false;
    }
    
    protected void _buffer(int index, BufferedValue v)
    {
        _lastBuffered = Math.max(_lastBuffered, index);
        if (index >= _buffered.length) {
            _buffered = Arrays.copyOf(_buffered, Math.max(index+1, _columnCount));
        }
        _buffered[index] = v;
    }

    protected void _flushBuffer() throws IOException
    {
        if (_outputTail > 0) {
            _charsWritten += _outputTail;
            _out.write(_outputBuffer, 0, _outputTail);
            _outputTail = 0;
        }
    }

    public void _releaseBuffers()
    {
        char[] buf = _outputBuffer;
        if (buf != null && _bufferRecyclable) {
            _outputBuffer = null;
            _ioContext.releaseConcatBuffer(buf);
        }
    }
}
