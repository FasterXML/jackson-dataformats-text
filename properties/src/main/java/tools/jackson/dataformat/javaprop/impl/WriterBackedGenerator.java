package tools.jackson.dataformat.javaprop.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import tools.jackson.core.*;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.NumberOutput;
import tools.jackson.dataformat.javaprop.JavaPropsGenerator;
import tools.jackson.dataformat.javaprop.JavaPropsSchema;
import tools.jackson.dataformat.javaprop.io.JPropEscapes;

public class WriterBackedGenerator extends JavaPropsGenerator
{
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Underlying {@link Writer} used for output.
     */
    protected final Writer _out;

    /**
     * Caller-provided {@link OutputStream} that {@link #_out} was constructed (by
     * Jackson) to wrap, if any; {@code null} if {@link #_out} was handed to us by the
     * caller.
     *<p>
     * A {@link Writer} we construct is an internal buffer, so it must always be flushed
     * on {@link #flush()} and closed on {@link #close()}. It is shielded from this
     * stream (see {@link GuardedOutputStream}), which is instead flushed and closed
     * directly, as per {@link StreamWriteFeature#FLUSH_PASSED_TO_STREAM} and
     * {@link StreamWriteFeature#AUTO_CLOSE_TARGET}.
     *
     * @since 3.3
     */
    protected final OutputStream _target;

    /*
    /**********************************************************************
    /* Output buffering
    /**********************************************************************
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
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public WriterBackedGenerator(ObjectWriteContext writeCtxt, IOContext ioCtxt,
            int stdFeatures, JavaPropsSchema schema,
            Writer out)
    {
        this(writeCtxt, ioCtxt, stdFeatures, schema, out, null);
    }

    /**
     * @param out Writer to write to: either provided by the caller, or constructed
     *    by Jackson to wrap {@code target}
     * @param target Caller-provided stream that {@code out} wraps, if {@code out} was
     *    constructed by Jackson (and hence must always be flushed and closed);
     *    {@code null} if {@code out} was provided by the caller
     *
     * @since 3.3
     */
    public WriterBackedGenerator(ObjectWriteContext writeCtxt, IOContext ioCtxt,
            int stdFeatures, JavaPropsSchema schema,
            Writer out, OutputStream target)
    {
        super(writeCtxt, ioCtxt, stdFeatures, schema);
        _out = out;
        _target = target;
        _outputBuffer = ioCtxt.allocConcatBuffer();
        _outputEnd = _outputBuffer.length;
    }

    /*
    /**********************************************************************
    /* Overridden methods, configuration
    /**********************************************************************
     */

    @Override
    public Object streamWriteOutputTarget() {
        // (follow-up to [dataformats-text#734]): Writer we constructed ourselves is an implementation
        //   detail, shielded from caller's stream: so expose the stream instead
        return (_target != null) ? _target : _out;
    }

    @Override
    public int streamWriteOutputBuffered() {
        return _outputTail;
    }

    /*
    /**********************************************************************
    /* Overridden methods: low-level I/O
    /**********************************************************************
     */

    @Override
    public void close()
    {
        if (!isClosed()) {
            // (follow-up to [dataformats-text#734]): closing may fail too (on writing out buffered
            //   content): must not mask earlier failure
            Throwable fail = null;
            try {
                _flushBuffer();
            } catch (Throwable t) {
                fail = t;
            }
            _outputTail = 0; // just to ensure we don't think there's anything buffered
            try {
                super.close();
            } catch (Throwable t) {
                if (fail == null) {
                    fail = t;
                } else {
                    fail.addSuppressed(t);
                }
            }
            if (fail != null) {
                if (fail instanceof RuntimeException re) {
                    throw re;
                }
                throw (Error) fail; // nothing else can be thrown from above
            }
        }
    }

    @Override
    protected void _closeInput() throws IOException
    {
        if (_out != null) {
            final boolean closeTarget = _ioContext.isResourceManaged()
                    || isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET);
            if (_target != null) {
                // [dataformats-text#734]: a Writer we constructed ourselves must be closed
                //   regardless: without that its buffered content never reaches the
                //   caller's OutputStream. Caller's stream is shielded from that, and
                //   closed (or flushed) here only if it should be, as per features
                //   enabled now.
                //   (follow-up to [dataformats-text#734]): first failure is the one to report, any
                //   later ones are added as suppressed
                Throwable fail = null;
                try {
                    _out.close();
                } catch (Throwable t) {
                    fail = t;
                }
                try {
                    if (closeTarget) {
                        _target.close();
                    } else if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
                        _target.flush();
                    }
                } catch (Throwable t) {
                    if (fail == null) {
                        fail = t;
                    } else {
                        fail.addSuppressed(t);
                    }
                }
                if (fail != null) {
                    if (fail instanceof IOException ioe) {
                        throw ioe;
                    }
                    if (fail instanceof RuntimeException re) {
                        throw re;
                    }
                    throw (Error) fail;
                }
            } else if (closeTarget) {
                _out.close();
            } else if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
                // If we can't close it, we should at least flush
                _out.flush();
            }
        }
    }

    @Override
    public void flush()
    {
        // (follow-up to [dataformats-text#734]): nothing to flush once closed -- and target may
        //   have been closed along with us
        if (isClosed()) {
            return;
        }
        _flushBuffer();
        if (_out != null) {
            try {
                if (_target != null) {
                    // [dataformats-text#734]: a Writer we constructed ourselves is just
                    //   a buffer, so must be flushed regardless; caller's stream is
                    //   shielded from that, and flushed here only if it should be
                    _out.flush();
                    if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
                        _target.flush();
                    }
                } else if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
                    _out.flush();
                }
            } catch (IOException e) {
                throw _wrapIOFailure(e);
            }
        }
    }

    /*
    /**********************************************************************
    /* Implementations for methods from base class
    /**********************************************************************
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
    protected void _appendPropertyName(StringBuilder path, String name) {
        // Note that escaping needs to be applied now...
        JPropEscapes.appendKey(_basePath, name);
        // NOTE: we do NOT yet write the key; wait until we have value; just append to path
    }

    /*
    /**********************************************************************
    /* Internal methods; escaping writes
    /**********************************************************************
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

    @Override
    protected void _writeUnescapedEntry(int value) throws JacksonException
    {
        _writeRaw(_basePath);
        _writeRaw(_schema.keyValueSeparator());
        // up to 10 digits and possible minus sign
        if ((_outputTail + 11) > _outputEnd) {
            _flushBuffer();
        }
        _outputTail = NumberOutput.outputInt(value, _outputBuffer, _outputTail);
        _writeLinefeed();
    }

    @Override
    protected void _writeUnescapedEntry(long value) throws JacksonException
    {
        _writeRaw(_basePath);
        _writeRaw(_schema.keyValueSeparator());
        // up to 19 digits and possible minus sign
        if ((_outputTail + 20) > _outputEnd) {
            _flushBuffer();
        }
        _outputTail = NumberOutput.outputLong(value, _outputBuffer, _outputTail);
        _writeLinefeed();
    }

    @Override
    protected void _writeUnescapedEntry(double value) throws JacksonException
    {
        if (!isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)) {
            _writeUnescapedEntry(NumberOutput.toString(value, false));
            return;
        }
        _writeRaw(_basePath);
        _writeRaw(_schema.keyValueSeparator());
        if ((_outputTail + NumberOutput.MAX_DOUBLE_BYTES) > _outputEnd) {
            _flushBuffer();
        }
        _outputTail = NumberOutput.outputDouble(value, _outputBuffer, _outputTail);
        _writeLinefeed();
    }

    @Override
    protected void _writeUnescapedEntry(float value) throws JacksonException
    {
        if (!isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)) {
            _writeUnescapedEntry(NumberOutput.toString(value, false));
            return;
        }
        _writeRaw(_basePath);
        _writeRaw(_schema.keyValueSeparator());
        if ((_outputTail + NumberOutput.MAX_FLOAT_BYTES) > _outputEnd) {
            _flushBuffer();
        }
        _outputTail = NumberOutput.outputFloat(value, _outputBuffer, _outputTail);
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
    /**********************************************************************
    /* Internal methods; raw writes
    /**********************************************************************
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
