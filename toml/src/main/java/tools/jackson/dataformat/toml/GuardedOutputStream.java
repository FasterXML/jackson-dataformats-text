package tools.jackson.dataformat.toml;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link OutputStream} wrapper that forwards written content, but never flushes or
 * closes the underlying stream.
 *<p>
 * Needed because we wrap the caller's {@link OutputStream} in a {@code Writer} of our
 * own, which buffers content and only hands it over when flushed or closed. That
 * {@code Writer} therefore has to be flushed by {@code JsonGenerator.flush()} and
 * closed by {@code JsonGenerator.close()} regardless of
 * {@link tools.jackson.core.StreamWriteFeature#FLUSH_PASSED_TO_STREAM} and
 * {@link tools.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET} -- but doing so must
 * not flush or close the caller's stream. Whether that is to be done is decided by
 * the generator, when flushed or closed, since those features may be changed on the
 * generator after construction.
 *
 * @since 3.3
 */
final class GuardedOutputStream extends FilterOutputStream
{
    public GuardedOutputStream(OutputStream out) {
        super(out);
    }

    // NOTE: must override; `FilterOutputStream` otherwise writes one byte at a time
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() {
        // Deliberately does NOT flush the stream we wrap
    }

    @Override
    public void close() {
        // Deliberately does NOT close (nor flush) the stream we wrap
    }
}
