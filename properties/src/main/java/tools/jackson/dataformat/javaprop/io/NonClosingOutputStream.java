package tools.jackson.dataformat.javaprop.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link OutputStream} wrapper that forwards written content but keeps closing -- and,
 * optionally, flushing -- of the underlying stream to itself.
 *<p>
 * Needed because we wrap the caller's {@link OutputStream} in a {@code Writer} of our
 * own, which buffers content and only hands it over when closed or flushed. That
 * {@code Writer} therefore has to be closed even when
 * {@link tools.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET} is disabled -- but
 * closing it must not take the caller's stream with it.
 *
 * @since 3.3
 */
public final class NonClosingOutputStream extends FilterOutputStream
{
    private final boolean _flushTarget;

    public NonClosingOutputStream(OutputStream out, boolean flushTarget) {
        super(out);
        _flushTarget = flushTarget;
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
        // Deliberately does NOT close (nor flush) the stream we wrap
    }
}
