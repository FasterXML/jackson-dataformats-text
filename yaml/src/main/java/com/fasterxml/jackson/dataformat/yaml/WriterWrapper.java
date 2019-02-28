package com.fasterxml.jackson.dataformat.yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;

import org.snakeyaml.engine.v1.api.StreamDataWriter;

public class WriterWrapper implements StreamDataWriter {
    private final Writer _writer;

    public WriterWrapper(Writer _writer) {
        this._writer = _writer;
    }

    @Override
    public void flush() {
        try {
            _writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
