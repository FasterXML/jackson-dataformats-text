package com.fasterxml.jackson.dataformat.yaml.util;

import java.io.IOException;
import java.io.Reader;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

/**
 * {@link Reader} decorator that enforces
 * {@link StreamReadConstraints#getMaxDocumentLength()} by counting characters
 * read through it. Needed since actual YAML decoding is done by SnakeYAML,
 * reading content directly from a {@link Reader}, so the parser
 * itself does not see input as it is consumed.
 *<p>
 * Should only be used when constraints define a maximum document length
 * (see {@link StreamReadConstraints#hasMaxDocumentLength()}).
 *
 * @since 2.18.11
 */
public class ReadConstrainedReader extends Reader
{
    private final Reader _delegate;

    private final StreamReadConstraints _constraints;

    /**
     * Total number of characters read (or skipped) so far.
     */
    private long _charsRead;

    public ReadConstrainedReader(Reader delegate, StreamReadConstraints constraints) {
        _delegate = delegate;
        _constraints = constraints;
    }

    public long charsRead() {
        return _charsRead;
    }

    private void _count(long n) throws StreamConstraintsException {
        if (n > 0) {
            _charsRead += n;
            _constraints.validateDocumentLength(_charsRead);
        }
    }

    @Override
    public int read() throws IOException {
        int c = _delegate.read();
        if (c >= 0) {
            _count(1);
        }
        return c;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int n = _delegate.read(cbuf, off, len);
        _count(n);
        return n;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = _delegate.skip(n);
        _count(skipped);
        return skipped;
    }

    @Override
    public boolean ready() throws IOException {
        return _delegate.ready();
    }

    @Override
    public void close() throws IOException {
        _delegate.close();
    }
}
