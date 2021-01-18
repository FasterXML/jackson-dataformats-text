package com.fasterxml.jackson.dataformat.javaprop.impl;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsGenerator;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;
import com.fasterxml.jackson.dataformat.javaprop.io.JPropEscapes;

public class WriterBackedGenerator extends JavaPropsGenerator
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Underlying {@link Writer} used for output.
     */
    final protected Writer _out;

    /*
    /**********************************************************
    /* Output buffering
    /**********************************************************
     */

    /**
     * Intermediate buffer in which contents are buffered before
     * being written using {@link #_out}.
     */
    protected char[] _outputBuffer;

    /**
     * Pointer to the next available location in {@link #_outputBuffer}
     */
    protected int _outputTail = 0;

    /**
     * Offset to index after the last valid index in {@link #_outputBuffer}.
     * Typically same as length of the buffer.
     */
    protected final int _outputEnd;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public WriterBackedGenerator(ObjectWriteContext writeCtxt, IOContext ioCtxt,
            int stdFeatures, JavaPropsSchema schema,
            Writer out)
    {
        super(writeCtxt, ioCtxt, stdFeatures, schema);
        _out = out;
        _outputBuffer = ioCtxt.allocConcatBuffer();
        _outputEnd = _outputBuffer.length;
    }

    /*
    /**********************************************************
    /* Overridden methods, configuration
    /**********************************************************
     */

    @Override
    public Object getOutputTarget() {
        return _out;
    }

    @Override
    public int getOutputBuffered() {
        return _outputTail;
    }

    /*
    /**********************************************************
    /* Overridden methods: low-level I/O
    /**********************************************************
     */

    @Override
    public void close()
    {
        super.close();
        _flushBuffer();
        _outputTail = 0; // just to ensure we don't think there's anything buffered

        if (_out != null) {
            try {
                if (_ioContext.isResourceManaged() || isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET)) {
                    _out.close();
                } else if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
                    // If we can't close it, we should at least flush
                    _out.flush();
                }
            } catch (IOException e) {
                throw _wrapIOFailure(e);
            }
        }
        // Internal buffer(s) generator has can now be released as well
        _releaseBuffers();
    }

    @Override
    public void flush()
    {
        _flushBuffer();
        if (_out != null) {
            if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
                try {
                    _out.flush();
                } catch (IOException e) {
                    throw _wrapIOFailure(e);
                }
            }
        }
    }

    /*
    /**********************************************************
    /* Implementations for methods from base class
    /**********************************************************
     */

    @Override
    protected void _releaseBuffers()
    {
        char[] buf = _outputBuffer;
        if (buf != null) {
            _outputBuffer = null;
            _ioContext.releaseConcatBuffer(buf);
        }
    }

    protected void _flushBuffer() throws JacksonException
    {
        if (_outputTail > 0) {
            try {
                _out.write(_outputBuffer, 0, _outputTail);
            } catch (IOException e) {
                throw _wrapIOFailure(e);
            }
            _outputTail = 0;
        }
    }

    @Override
    protected void _appendFieldName(StringBuilder path, String name) {
        // Note that escaping needs to be applied now...
        JPropEscapes.appendKey(_basePath, name);
        // NOTE: we do NOT yet write the key; wait until we have value; just append to path
    }

    /*
    /**********************************************************
    /* Internal methods; escaping writes
    /**********************************************************
     */

    @Override
    protected void _writeEscapedEntry(String value) throws JacksonException
    {
        // note: key has been already escaped so:
        _writeRaw(_basePath);
        _writeRaw(_schema.keyValueSeparator());

        _writeEscaped(value);
        _writeLinefeed();
    }

    @Override
    protected void _writeEscapedEntry(char[] text, int offset, int len) throws JacksonException
    {
        // note: key has been already escaped so:
        _writeRaw(_basePath);
        _writeRaw(_schema.keyValueSeparator());

        _writeEscaped(text, offset, len);
        _writeLinefeed();
    }

    @Override
    protected void _writeUnescapedEntry(String value) throws JacksonException
    {
        // note: key has been already escaped so:
        _writeRaw(_basePath);
        _writeRaw(_schema.keyValueSeparator());

        _writeRaw(value);
        _writeLinefeed();
    }

    protected void _writeEscaped(String value) throws JacksonException
    {
        StringBuilder sb = JPropEscapes.appendValue(value);
        if (sb == null) {
            _writeRaw(value);
        } else {
            _writeRaw(sb);
        }
    }

    protected void _writeEscaped(char[] text, int offset, int len) throws JacksonException
    {
        _writeEscaped(new String(text, offset, len));
    }

    protected void _writeLinefeed() throws JacksonException
    {
        _writeRaw(_schema.lineEnding());
    }

    /*
    /**********************************************************
    /* Internal methods; raw writes
    /**********************************************************
     */

    @Override
    protected void _writeRaw(char c) throws JacksonException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = c;
    }

    @Override
    protected void _writeRaw(String text) throws JacksonException
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
            _writeRawLong(text);
        }
    }

    @Override
    protected void _writeRaw(StringBuilder text) throws JacksonException
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
            _writeRawLong(text);
        }
    }

    @Override
    protected void _writeRaw(char[] text, int offset, int len) throws JacksonException
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
        try {
            _out.write(text, offset, len);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
    }

    protected void _writeRawLong(String text) throws JacksonException
    {
        int room = _outputEnd - _outputTail;
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

    protected void _writeRawLong(StringBuilder text) throws JacksonException
    {
        int room = _outputEnd - _outputTail;
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
}
