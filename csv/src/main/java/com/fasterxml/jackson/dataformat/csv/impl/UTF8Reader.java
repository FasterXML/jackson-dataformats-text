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
    private int _charCount = 0;

    /**
     * Total read byte count; used for error reporting purposes
     */
    private int _byteCount = 0;

    /**
     * Flag that is set when a pending decode error has been detected; needed
     * to properly handle deferred reporting.
     */
    private int _decodeErrorOffset;

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
    }

    /**
     * Method that can be used to see if we can actually modify the
     * underlying buffer. This is the case if we are managing the buffer,
     * but not if it was just given to us.
     */
    protected final boolean canModifyBuffer() {
        return (_ioContext != null);
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
    public int read(char[] cbuf, int start, int len)
        throws IOException
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
        } else {
            if (_decodeErrorOffset != 0) {
                reportDeferredInvalid();
            }

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

            // Ok; if we end here, we got multi-byte combination
            if ((c & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF), from 110xxxxx
                c = (c & 0x1F);
                needed = 1;
            } else if ((c & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF), from 1110xxxx
                c = (c & 0x0F);
                needed = 2;
            } else if ((c & 0xF8) == 0xF0) { // 4 bytes from 11110xxx (double-char w/ surrogates and all)
                c = (c & 0x0F);
                needed = 3;
            } else {
                reportInvalidInitial(c & 0xFF, outPtr-start);
                // 25-Aug-2016, tatu: As per [dataformat-csv#132], only returns
                // if we are ok to return already decoded content and defer error reporting
                --inPtr;
                break main_loop;
            }
            /* Do we have enough bytes? If not, let's just push back the
             * byte and leave, since we have already gotten at least one
             * char decoded. This way we will only block (with read from
             * input stream) when absolutely necessary.
             */
            if ((inBufLen - inPtr) < needed) {
                --inPtr;
                break main_loop;
            }

            int d = buf[inPtr];
            if ((d & 0xC0) != 0x080) {
                reportInvalidOther(d & 0xFF, outPtr-start, 2);
                break main_loop;
            }
            ++inPtr;
            c = (c << 6) | (d & 0x3F);

            if (needed > 1) { // needed == 1 means 2 bytes total
                d = buf[inPtr]; // 3rd byte
                if ((d & 0xC0) != 0x080) {
                    reportInvalidOther(d & 0xFF, outPtr-start, 3);
                    break main_loop;
                }
                ++inPtr;
                c = (c << 6) | (d & 0x3F);
                if (needed > 2) { // 4 bytes? (need surrogates)
                    d = buf[inPtr];
                    if ((d & 0xC0) != 0x080) {
                        reportInvalidOther(d & 0xFF, outPtr-start, 4);
                        break main_loop;
                    }
                    ++inPtr;
                    c = (c << 6) | (d & 0x3F);
                    /* Ugh. Need to mess with surrogates. Ok; let's inline them
                     * there, then, if there's room: if only room for one,
                     * need to save the surrogate for the rainy day...
                     */
                    c -= 0x10000; // to normalize it starting with 0x0
                    cbuf[outPtr++] = (char) (0xD800 + (c >> 10));
                    // hmmh. can this ever be 0? (not legal, at least?)
                    c = (0xDC00 | (c & 0x03FF));

                    // Room for second part?
                    if (outPtr >= len) { // nope
                        _surrogate = c;
                        break main_loop;
                    }
                    // sure, let's fall back to normal processing:
                }
                // Otherwise, should we check that 3-byte chars are
                // legal ones (should not expand to surrogates?
                // For now, let's not...
                /*
                else {
                    if (c >= 0xD800 && c < 0xE000) {
                        reportInvalid(c, outPtr-start, "(a surrogate character) ");
                    }
                }
                */
            }
            cbuf[outPtr++] = (char) c;
            if (inPtr >= inBufLen) {
                break main_loop;
            }
        }

        _inputPtr = inPtr;
        len = outPtr - start;
        _charCount += len;
        return len;
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
     *   one full character; false if EOF was encountered instead.
     */
    private boolean loadMore(int available)
        throws IOException
    {
        _byteCount += (_inputEnd - available);

        // Bytes that need to be moved to the beginning of buffer?
        if (available > 0) {
            if (_inputPtr > 0) {
                if (!canModifyBuffer()) {
                    throw new IllegalStateException("Need to move partially decoded character; buffer not modifiable");
                }
                for (int i = 0; i < available; ++i) {
                    _inputBuffer[i] = _inputBuffer[_inputPtr+i];
                }
                _inputPtr = 0;
                _inputEnd = available;
            }
        } else {
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

        /* And then we'll just need to load up to that many bytes;
         * if an EOF is hit, that'll be an error. But we need not do
         * actual decoding here, just load enough bytes.
         */
        while ((_inputPtr + needed) > _inputEnd) {
            int count = readBytesAt(_inputEnd);
            if (count < 1) {
                if (count < 0) { // -1, EOF... no good!
                    freeBuffers();
                    reportUnexpectedEOF(_inputEnd, needed);
                }
                // 0 count is no good; let's err out
                reportStrangeStream();
            }
        }
        return true;
    }

    protected void reportBounds(char[] cbuf, int start, int len) throws IOException {
        throw new ArrayIndexOutOfBoundsException("read(buf,"+start+","+len+"), cbuf["+cbuf.length+"]");
    }

    protected void reportStrangeStream() throws IOException {
        throw new IOException("Strange I/O stream, returned 0 bytes on read");
    }

    protected void reportInvalidInitial(int mask, int outputDecoded) throws IOException
    {
        // 25-Aug-2016, tatu: As per [dataformat-csv#132] defer error reporting if
        //   (but only if) some content has been decoded successfully
        if (_decodeErrorOffset == 0) {
            if (outputDecoded > 0) {
                _decodeErrorOffset = 1;
                return;
            }
        }
        
        // input (byte) ptr has been advanced by one, by now:
        int bytePos = _byteCount + _inputPtr - 1;
        int charPos = _charCount + outputDecoded + 1;

        throw new CharConversionException(String.format(
                "Invalid UTF-8 start byte 0x%s (at char #%d, byte #%d): check content encoding, does not look like UTF-8",
                Integer.toHexString(mask), charPos, bytePos));
    }

    protected void reportInvalidOther(int mask, int outputDecoded, int errorPosition) throws IOException
    {
        // 25-Aug-2016, tatu: As per [dataformat-csv#132] defer error reporting if
        //   (but only if) some content has been decoded successfully
        if (_decodeErrorOffset == 0) {
            if (outputDecoded > 0) {
                _decodeErrorOffset = errorPosition;
                return;
            }
        }

        int bytePos = _byteCount + _inputPtr - 1;
        int charPos = _charCount + outputDecoded + 1;

        throw new CharConversionException(String.format(
                "Invalid UTF-8 middle byte 0x%s (at char #%d, byte #%d): check content encoding, does not look like UTF-8",
                Integer.toHexString(mask), charPos, bytePos));
    }

    protected void reportDeferredInvalid() throws IOException
    {
        int ch = _inputBuffer[_inputPtr] & 0xFF;
        if (_decodeErrorOffset == 1) {
            reportInvalidInitial(ch, 0);
        } else {
            reportInvalidOther(ch, 0, _decodeErrorOffset);
        }
    }

    protected void reportUnexpectedEOF(int gotBytes, int needed) throws IOException
    {
        int bytePos = _byteCount + gotBytes;
        int charPos = _charCount;

        throw new CharConversionException(String.format(
                "Unexpected EOF in the middle of a multi-byte UTF-8 character: got %d, needed %d, at char #%d, byte #%d)",
                gotBytes, needed, charPos, bytePos));
    }
}
