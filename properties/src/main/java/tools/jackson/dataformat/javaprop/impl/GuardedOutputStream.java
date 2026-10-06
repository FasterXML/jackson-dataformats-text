package tools.jackson.dataformat.javaprop.impl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link OutputStream} wrapper that forwards written content, but only forwards
 * {@link #flush()} and {@link #close()} to the underlying stream if configured to
 * (a {@link #close()} that may not close it flushes it instead, if allowed).
 *<p>
 * Needed because we wrap the caller's {@link OutputStream} in a {@code Writer} of our
 * own, which buffers content and only hands it over when flushed or closed. That
 * {@code Writer} therefore has to be flushed by {@code JsonGenerator.flush()} and
 * closed by {@code JsonGenerator.close()} regardless of
 * {@link tools.jackson.core.StreamWriteFeature#FLUSH_PASSED_TO_STREAM} and
 * {@link tools.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET} -- but doing so must
 * only flush or close the caller's stream if those features say so.
 *
 * @since 3.3
 */
public final class GuardedOutputStream extends FilterOutputStream
{
    private final boolean _flushTarget;

    private final boolean _closeTarget;

    public GuardedOutputStream(OutputStream out, boolean flushTarget, boolean closeTarget) {
        super(out);
        _flushTarget = flushTarget;
        _closeTarget = closeTarget;
    }

    // NOTE: must override; `FilterOutputStream` otherwise writes one byte at a time
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        if (_flushTarget) {
            out.flush();
        }
    }

    @Override
    public void close() throws IOException {
        // Our `Writer` being closed means end of output: close the stream we wrap if
        // allowed, else at least flush it if allowed. NOTE: done explicitly rather than
        // relying on `OutputStreamWriter.close()` flushing the stream it wraps
        if (_closeTarget) {
            out.close();
        } else {
            flush();
        }
    }
}
