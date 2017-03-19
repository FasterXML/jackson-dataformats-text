package com.fasterxml.jackson.dataformat.javaprop.io;

import java.io.*;

import com.fasterxml.jackson.core.io.IOContext;

/**
 * Optimized Reader that reads ISO-8859-1 encoded content from an input stream.
 * The reason for custom implementation is that this allows recycling of
 * underlying read buffer, which is important for small content.
 */
public final class Latin1Reader extends Reader
{
    /**
     * IO context to use for returning input buffer, iff
     * buffer is to be recycled when input ends.
     */
    private final IOContext _ioContext;

    private InputStream _inputSource;

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
     * Total read character count; used for error reporting purposes
     */
    private int _charCount = 0;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    /*
    public Latin1Reader(IOContext ctxt, InputStream in,
            byte[] buf, int ptr, int len)
    {
        super((in == null) ? buf : in);
        _ioContext = ctxt;
        _inputSource = in;
        _inputBuffer = buf;
        _inputPtr = ptr;
        _inputEnd = ptr+len;
    }
    */

    public Latin1Reader(byte[] buf, int ptr, int len)
    {
        super(new Object());
        _ioContext = null;
        _inputSource = null;
        _inputBuffer = buf;
        _inputPtr = ptr;
        _inputEnd = ptr+len;
    }

    public Latin1Reader(IOContext ctxt, InputStream in)
    {
        super(in);
        _ioContext = ctxt;
        _inputSource = in;
        _inputBuffer = ctxt.allocReadIOBuffer();
        _inputPtr = 0;
        _inputEnd = 0;
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    public int getReadCharsCount() {
        return _charCount;
    }

    /*
    /**********************************************************************
    /* Reader API
    /**********************************************************************
     */

    @Override
    public void close() throws IOException
    {
        _inputSource = null;
        freeBuffers();
    }

    private char[] _tmpBuffer = null;

    // Although this method is implemented by the base class, AND it should
    // never be called by parser code, let's still implement it bit more
    // efficiently just in case
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
    public int read(char[] cbuf, int start, int len) throws IOException
    {
        if (_inputBuffer == null) {
            return -1;
        }
        if (len < 1) {
            return len;
        }

        // To prevent unnecessary blocking (esp. with network streams),
        // we'll only require decoding of a single char
        int left = (_inputEnd - _inputPtr);
        if (left < 1) {
            if (!loadMore()) { // (legal) EOF?
                return -1;
            }
            left = (_inputEnd - _inputPtr);
        }
        if (left > len) {
            left = len;
        }

        final byte[] inBuf = _inputBuffer;
        int inPtr = _inputPtr;
        int outPtr = start;
        final int inEnd = inPtr + left;

        do {
            cbuf[outPtr++] = (char) inBuf[inPtr++];
        } while (inPtr < inEnd);
        _inputPtr = inPtr;
        return left;
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
    private boolean loadMore() throws IOException
    {
        _charCount += _inputEnd;
        _inputPtr = 0;
        _inputEnd = 0;
        if (_inputSource == null) {
            freeBuffers();
            return false;
        }
        int count = _inputSource.read(_inputBuffer, 0, _inputBuffer.length);
        if (count < 1) {
            freeBuffers();
            if (count < 0) { // -1
                return false;
            }
            // 0 count is no good; let's err out
            throw new IOException("Strange I/O stream, returned 0 bytes on read");
        }
        _inputEnd = count;
        return true;
    }

    /**
     * This method should be called along with (or instead of) normal
     * close. After calling this method, no further reads should be tried.
     * Method will try to recycle read buffers (if any).
     */
    private final void freeBuffers()
    {
        if (_ioContext != null) {
            byte[] buf = _inputBuffer;
            if (buf != null) {
                _inputBuffer = null;
                _ioContext.releaseReadIOBuffer(buf);
            }
        }
    }
}
