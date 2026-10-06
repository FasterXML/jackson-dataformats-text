package tools.jackson.dataformat.yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;

import org.snakeyaml.engine.v2.api.StreamDataWriter;

public class WriterWrapper implements StreamDataWriter {
    private final Writer _writer;

    public WriterWrapper(Writer _writer) {
        this._writer = _writer;
    }

    /**
     * Deliberately does NOT flush the underlying {@link Writer}: SnakeYAML flushes
     * its output at the end of each document (and of the stream), but whether
     * the target gets flushed is for {@link YAMLGenerator} to decide, as per
     * {@link tools.jackson.core.StreamWriteFeature#FLUSH_PASSED_TO_STREAM}
     * [dataformats-text#749].
     */
    @Override
    public void flush() {
        // nothing to do
    }

    @Override
    public void write(String str) {
        try {
            _writer.write(str);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void write(String str, int off, int len) {
        try {
            _writer.write(str, off, len);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
