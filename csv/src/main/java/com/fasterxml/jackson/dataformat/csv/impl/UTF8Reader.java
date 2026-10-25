package com.fasterxml.jackson.dataformat.csv.impl;

import java.io.*;

import com.fasterxml.jackson.core.io.IOContext;

/**
 * Optimized Reader that reads UTF-8 encoded content from an input stream.
 * In addition to doing (hopefully) optimal conversion, it can also take
 * array of "pre-read" (leftover) bytes; this is necessary when preliminary
 * stream/reader is trying to figure out underlying character encoding.
 */
public final class UTF8Reader
    extends Reader
{
    /**
     * IO context to use for returning input buffer, iff
     * buffer is to be recycled when input ends.
     */
    private final IOContext _ioContext;

    private InputStream _inputSource;

    private final boolean _autoClose;

    private byte[] _inputBuffer;

    /**
     * Flag set to indicate {@code inputBuffer} is read-only, and its
     * content should not be modified. This is the case when caller
     * has passed in a buffer of contents already read, instead of Jackson
     * allocating read buffer.
     *
     * @since 2.19
     */
    private final boolean _inputBufferReadOnly;

    /**
     * Pointer to the next available byte (if any), iff less than
     * <code>mByteBufferEnd</code>
     */
    private int _inputPtr;

    /**
     * Pointed to the end marker, that is, position one after the last
     * valid available byte.
     */
    private int _inputEnd;

    /**
     * Decoded first character of a surrogate pair, if one needs to be buffered
     */
    private int _surrogate = -1;
    
    /**
     * Total read character count; used for error reporting purposes
     */
    private long _charCount = 0;

    /**
     * Number of input bytes that precede {@code _inputBuffer[0]};
     * used for error reporting purposes
     */
    private long _byteCount;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public UTF8Reader(IOContext ctxt, InputStream in, boolean autoClose,
            byte[] buf, int ptr, int len)
    {
        super((in == null) ? buf : in);
        _ioContext = ctxt;
        _inputSource = in;
        _inputBuffer = buf;
        _inputPtr = ptr;
        _inputEnd = ptr+len;
        _autoClose = autoClose;
        // Unmodifiable if there is no stream to actually read from
        // (ideally caller should pass explicitly)
        _inputBufferReadOnly = (in == null);
        // so that byte positions are relative to start of content, not of buffer
        _byteCount = -ptr;
    }

    public UTF8Reader(IOContext ctxt, byte[] buf, int ptr, int len)
    {
        super(new Object());
        _ioContext = ctxt;
        _inputSource = null;
        _inputBuffer = buf;
        _inputPtr = ptr;
        _inputEnd = ptr+len;
        _autoClose = true;
        // This is the case when we have a buffer of contents already read
        _inputBufferReadOnly = true;
        _byteCount = -ptr;
    }

    public UTF8Reader(IOContext ctxt, InputStream in, boolean autoClose)
    {
        super(in);
        _ioContext = ctxt;
        _inputSource = in;
        _inputBuffer = ctxt.allocReadIOBuffer();
        _inputPtr = 0;
        _inputEnd = 0;
        _autoClose = autoClose; 
        // Buffer allocated above, modifiable as needed
        _inputBufferReadOnly = false;
    }

    /*
    /**********************************************************************
    /* Reader API
    /**********************************************************************
     */

    @Override
    public void close() throws IOException
    {
        InputStream in = _inputSource;

        if (in != null) {
            _inputSource = null;
            if (_autoClose) {
                in.close();
            }
        }
        freeBuffers();
    }

    private char[] _tmpBuffer = null;

    /**
     * Although this method is implemented by the base class, AND it should
     * never be called by parser code, let's still implement it bit more
     * efficiently just in case
     */
    @Override
    public int read() throws IOException
    {
        if (_tmpBuffer == null) {
            _tmpBuffer = new char[1];
        }
        if (read(_tmpBuffer, 0, 1) < 1) {
            return -1;
        }
        return _tmpBuffer[0];
    }

    @Override
    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }
    
    @Override
    public int read(final char[] cbuf, final int start, int len) throws IOException
    {
        // Already EOF?
        if (_inputBuffer == null) {
            return -1;
        }
        len += start;
        int outPtr = start;

        // Ok, first; do we have a surrogate from last round?
        if (_surrogate >= 0) {
            cbuf[outPtr++] = (char) _surrogate;
            _surrogate = -1;
            // No need to load more, already got one char
            // 15-Sep-2022, tatu: But need to avoid having empty buffer
            if (_inputPtr >= _inputEnd) {
                _charCount += 1;
                return 1;
            }
            // otherwise let things work the way they should
        } else {
            // To prevent unnecessary blocking (esp. with network streams),
            // we'll only require decoding of a single char
            int left = (_inputEnd - _inputPtr);

            // So; only need to load more if we can't provide at least one more character.
            // We need not do thorough check here, but let's check the common cases here:
            // either completely empty buffer (left == 0), or one with less than max. byte
            // count for a single char, and starting of a multi-byte encoding (this leaves
            // possibility of a 2/3-byte char that is still fully accessible...
            // but that can be checked by the load method)
            if (left < 4) {
                // Need to load more?
                if (left < 1 || _inputBuffer[_inputPtr] < 0) {
                    if (!loadMore(left)) { // (legal) EOF?
                        return -1;
                    }
                }
            }
        }
        final byte[] buf = _inputBuffer;
        int inPtr = _inputPtr;
        final int inBufLen = _inputEnd;
        // start of current multi-byte sequence, and whether it is invalid: details are
        // only figured out if (and when) error is reported
        int seqStart = 0;
        boolean invalid = false;

        main_loop:
        while (outPtr < len) {
            // At this point we have at least one byte available
            int c = buf[inPtr++];

            // Let's first do the quickie loop for common case; 7-bit ASCII
            if (c >= 0) { // ASCII? can probably loop, then
                cbuf[outPtr++] = (char) c; // ok since MSB is never on

                // Ok, how many such chars could we safely process without overruns?
                // (will combine 2 in-loop comparisons into just one)
                int outMax = (len - outPtr); // max output
                int inMax = (inBufLen - inPtr); // max input
                int inEnd = inPtr + ((inMax < outMax) ? inMax : outMax);

                ascii_loop:
                while (true) {
                    if (inPtr >= inEnd) {
                        break main_loop;
                    }
                    c = buf[inPtr++];
                    if (c < 0) { // or multi-byte
                        break ascii_loop;
                    }
                    cbuf[outPtr++] = (char) c;
                }
            }

            int needed;
            seqStart = inPtr - 1;

            // Ok; if we end here, we got multi-byte combination
            if ((c & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF), from 110xxxxx
                c = (c & 0x1F);
                needed = 1;
            } else if ((c & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF), from 1110xxxx
                c = (c & 0x0F);
                needed = 2;
            } else if ((c & 0xF8) == 0xF0) { // 4 bytes from 11110xxx (double-char w/ surrogates and all)
                c = (c & 0x07);
                needed = 3;
            } else {
                invalid = true;
                break main_loop;
            }
            /* Do we have enough bytes? If not, let's just push back the
             * byte and leave, since we have already gotten at least one
             * char decoded. This way we will only block (with read from
             * input stream) when absolutely necessary.
             */
            if ((inBufLen - inPtr) < needed) {
                // ... unless bytes we do have already make the sequence invalid
                if (findInvalidMiddleByte(buf, inPtr, inBufLen) >= 0) {
                    invalid = true;
                    break main_loop;
                }
                --inPtr;
                break main_loop;
            }

            int d = buf[inPtr++];
            if ((d & 0xC0) != 0x080) {
                invalid = true;
                break main_loop;
            }
            c = (c << 6) | (d & 0x3F);

            if (needed > 1) { // needed == 1 means 2 bytes total
                d = buf[inPtr++]; // 3rd byte
                if ((d & 0xC0) != 0x080) {
                    invalid = true;
                    break main_loop;
                }
                c = (c << 6) | (d & 0x3F);
                if (needed > 2) { // 4 bytes? (need surrogates)
                    d = buf[inPtr++];
                    if ((d & 0xC0) != 0x080) {
                        invalid = true;
                        break main_loop;
                    }
                    c = (c << 6) | (d & 0x3F);
                    // overlong, or beyond U+10FFFF?
                    if (c < 0x10000 || c > Character.MAX_CODE_POINT) {
                        invalid = true;
                        break main_loop;
                    }
                    /* Ugh. Need to mess with surrogates. Ok; let's inline them
                     * there, then, if there's room: if only room for one,
                     * need to save the surrogate for the rainy day...
                     */
                    c -= 0x10000; // to normalize it starting with 0x0
                    cbuf[outPtr++] = (char) (0xD800 + (c >> 10));
                    c = (0xDC00 | (c & 0x03FF));

                    // Room for second part?
                    if (outPtr >= len) { // nope
                        _surrogate = c;
                        break main_loop;
                    }
                    // sure, let's fall back to normal processing:
                } else if (c < 0x800 || (c >= 0xD800 && c <= 0xDFFF)) {
                    // overlong, or surrogate (illegal in UTF-8 as per [jackson-core#363])
                    invalid = true;
                    break main_loop;
                }
            } else if (c < 0x80) { // overlong
                invalid = true;
                break main_loop;
            }
            cbuf[outPtr++] = (char) c;
            if (inPtr >= inBufLen) {
                break main_loop;
            }
        }

        if (invalid) {
            // 25-Aug-2016, tatu: As per [dataformat-csv#132] defer error reporting if
            //   (but only if) some content has been decoded successfully: we then
            //   return that content, and next call starts from the invalid sequence
            //   and reports it.
            if (outPtr == start) {
                reportInvalidSequence(seqStart, inBufLen);
            }
            inPtr = seqStart;
        }

        _inputPtr = inPtr;
        final int actualLen = outPtr - start;
        _charCount += actualLen;
        return actualLen;
    }

    /*
    /**********************************************************************
    /* Internal/package methods:
    /**********************************************************************
     */

    protected final InputStream getStream() { return _inputSource; }

    /**
     * Method for reading as many bytes from the underlying stream as possible
     * (that fit in the buffer), to the beginning of the buffer.
     */
    protected final int readBytes()
        throws IOException
    {
        _inputPtr = 0;
        _inputEnd = 0;
        if (_inputSource != null) {
            int count = _inputSource.read(_inputBuffer, 0, _inputBuffer.length);
            if (count > 0) {
                _inputEnd = count;
            }
            return count;
        }
        return -1;
    }

    /**
     * Method for reading as many bytes from the underlying stream as possible
     * (that fit in the buffer considering offset), to the specified offset.
     *
     * @return Number of bytes read, if any; -1 to indicate none available
     *  (that is, end of input)
     */
    protected final int readBytesAt(int offset)
        throws IOException
    {
        // shouldn't modify mBytePtr, assumed to be 'offset'
        if (_inputSource != null) {
            int count = _inputSource.read(_inputBuffer, offset, _inputBuffer.length - offset);
            if (count > 0) {
                _inputEnd += count;
            }
            return count;
        }
        return -1;
    }

    /**
     * This method should be called along with (or instead of) normal
     * close. After calling this method, no further reads should be tried.
     * Method will try to recycle read buffers (if any).
     */
    public final void freeBuffers()
    {
        if (_ioContext != null) {
            byte[] buf = _inputBuffer;
            if (buf != null) {
                _inputBuffer = null;
                _ioContext.releaseReadIOBuffer(buf);
            }
        }
    }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    /**
     * @param available Number of "unused" bytes in the input buffer
     *
     * @return True, if enough bytes were read to allow decoding of at least
     *   one full character (or to report it invalid); false if EOF was
     *   encountered instead.
     */
    private boolean loadMore(int available) throws IOException
    {
        if (available > 0) {
            // Should we move bytes to the beginning of buffer?
            if (_inputPtr > 0) {
                // Can only do so if buffer mutable
                if (!_inputBufferReadOnly) {
                    _byteCount += _inputPtr;
                    for (int i = 0; i < available; ++i) {
                        _inputBuffer[i] = _inputBuffer[_inputPtr+i];
                    }
                    _inputPtr = 0;
                    _inputEnd = available;
                }
            }
        } else {
            _byteCount += _inputEnd;
            // Ok; here we can actually reasonably expect an EOF, so let's do a separate read right away:
            int count = readBytes();
            if (count < 1) {
                freeBuffers(); // to help GC?
                if (count < 0) { // -1
                    return false;
                }
                // 0 count is no good; let's err out
                reportStrangeStream();
            }
        }

        // We now have at least one byte... and that allows us to
        // calculate exactly how many bytes we need!
        @SuppressWarnings("cast")
        int c = (int) _inputBuffer[_inputPtr];
        if (c >= 0) { // single byte (ascii) char... cool, can return
            return true;
        }

        // Ok, a multi-byte char, let's check how many bytes we'll need:
        int needed;
        if ((c & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF)
            needed = 2;
        } else if ((c & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF)
            needed = 3;
        } else if ((c & 0xF8) == 0xF0) {
            // 4 bytes; double-char BS, with surrogates and all...
            needed = 4;
        } else {
            // 25-Aug-2016, tatu: As per [dataformat-csv#132], let's not throw
            //    exception from here but let caller handle
            return true;
        }

        // And then we'll just need to load up to that many bytes;
        // if an EOF is hit, that'll be an error. But we need not do
        // actual decoding here, just load enough bytes.
        while ((_inputPtr + needed) > _inputEnd) {
            // No point in waiting for (or failing on lack of) more bytes if the ones
            // we have make sequence invalid: let caller report the actual problem
            if (findInvalidMiddleByte(_inputBuffer, _inputPtr+1, _inputEnd) >= 0) {
                return true;
            }
            int count = readBytesAt(_inputEnd);
            if (count < 1) {
                if (count < 0) { // -1, EOF... no good!
                    try {
                        // but report sequence as invalid if possible, not as truncated
                        reportInvalidSequence(_inputPtr, _inputEnd);
                    } finally {
                        freeBuffers();
                    }
                }
                // 0 count is no good; let's err out
                reportStrangeStream();
            }
        }
        return true;
    }

    // Index of first byte in buf[start, end) that is not a valid continuation
    // byte (10xxxxxx), if any; -1 if none
    private static int findInvalidMiddleByte(byte[] buf, int start, int end) {
        for (int i = start; i < end; ++i) {
            if ((buf[i] & 0xC0) != 0x080) {
                return i;
            }
        }
        return -1;
    }

    protected void reportBounds(char[] cbuf, int start, int len) throws IOException {
        throw new ArrayIndexOutOfBoundsException("read(buf,"+start+","+len+"), cbuf["+cbuf.length+"]");
    }

    protected void reportStrangeStream() throws IOException {
        throw new IOException("Strange I/O stream, returned 0 bytes on read");
    }

    private final static String NOT_UTF8 = ": check content encoding, does not look like UTF-8";

    private static String invalidMiddleByte(int b) {
        return "Invalid UTF-8 middle byte 0x"+Integer.toHexString(b & 0xFF);
    }

    // Smallest code point that needs given number of continuation bytes
    private final static int[] MIN_CODE_POINTS = { 0, 0x80, 0x800, 0x10000 };

    /**
     * Method called to report invalid (or, at end of input, truncated) multi-byte
     * sequence that starts at {@code _inputBuffer[seqStart]}, with bytes available up to
     * (but not including) {@code end}. Kept separate from the decode loop which only
     * notes that the sequence is invalid: as errors are rare, the specific problem is
     * only figured out here.
     *<p>
     * For truncated sequences, the bytes available may already prove sequence
     * invalid (start byte like {@code 0xC0} that can only be used for overlong
     * encodings, for example); if not, sequence is reported as truncated.
     */
    private void reportInvalidSequence(int seqStart, int end) throws IOException
    {
        final byte[] buf = _inputBuffer;
        int c = buf[seqStart] & 0xFF;
        int needed;
        if ((c & 0xE0) == 0xC0) {
            c &= 0x1F;
            needed = 1;
        } else if ((c & 0xF0) == 0xE0) {
            c &= 0x0F;
            needed = 2;
        } else if ((c & 0xF8) == 0xF0) {
            c &= 0x07;
            needed = 3;
        } else {
            reportInvalid("Invalid UTF-8 start byte 0x"+Integer.toHexString(c), seqStart, true);
            return;
        }
        final int avail = Math.min(needed, end - seqStart - 1);
        for (int i = seqStart + 1, last = seqStart + avail; i <= last; ++i) {
            int d = buf[i];
            if ((d & 0xC0) != 0x080) {
                reportInvalid(invalidMiddleByte(d), i, true);
            }
            c = (c << 6) | (d & 0x3F);
        }
        // Range of values sequence may decode to (single value if complete)
        final int shift = 6 * (needed - avail);
        final int min = c << shift;
        final int max = min | ((1 << shift) - 1);
        String reason = null;
        if (max < MIN_CODE_POINTS[needed]) {
            reason = "overlong encoding";
        } else if (min > Character.MAX_CODE_POINT || (min >= 0xD800 && max <= 0xDFFF)) {
            // as per [jackson-core#363], surrogates are illegal in UTF-8
            reason = "not a valid Unicode scalar value";
        }
        if (reason != null) {
            StringBuilder sb = new StringBuilder("Invalid UTF-8 ");
            if (avail == needed) {
                sb.append("character 0x").append(Integer.toHexString(c));
            } else {
                sb.append("sequence");
                for (int i = seqStart, last = seqStart + avail; i <= last; ++i) {
                    sb.append(" 0x").append(Integer.toHexString(buf[i] & 0xFF));
                }
            }
            sb.append(" (").append(reason).append(')');
            reportInvalid(sb.toString(), seqStart, false);
        }
        if (avail < needed) { // only called for truncated sequences at end of input
            reportUnexpectedEOF(avail + 1, needed + 1, seqStart);
        }
        // should never get here
        throw new IllegalStateException("Internal error: valid UTF-8 sequence reported as invalid"
                +_position(seqStart));
    }

    // Errors are only reported before anything is decoded by the current read() call
    // (see [dataformat-csv#132] above), so the offending char is the one after
    // `_charCount`. `byteIndex` is the index in `_inputBuffer` of the offending byte:
    // `_inputPtr` is not updated until read() returns, so callers pass it in.
    private void reportInvalid(String msg, int byteIndex, boolean notUtf8)
        throws IOException
    {
        throw new CharConversionException(msg+_position(byteIndex)
                +(notUtf8 ? NOT_UTF8 : ""));
    }

    private void reportUnexpectedEOF(int gotBytes, int needed, int byteIndex) throws IOException
    {
        throw new CharConversionException(String.format(
                "Unexpected EOF in the middle of a multi-byte UTF-8 character: got %d, needed %d%s",
                gotBytes, needed, _position(byteIndex)));
    }

    // 1-based positions, relative to start of content (after BOM, if any)
    private String _position(int byteIndex) {
        return " (at char #"+(_charCount + 1)+", byte #"+(_byteCount + byteIndex + 1)+")";
    }
}
