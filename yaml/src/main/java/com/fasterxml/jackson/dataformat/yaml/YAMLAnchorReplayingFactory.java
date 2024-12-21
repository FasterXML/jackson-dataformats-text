package com.fasterxml.jackson.dataformat.yaml;

import java.io.CharArrayReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;

/**
 * A subclass of YAMLFactory with the only purpose to replace the YAMLParser by
 * the YAMLAnchorReplayingParser subclass.
 *
 * @since 2.19
 */
public class YAMLAnchorReplayingFactory extends YAMLFactory {
    private static final long serialVersionUID = 1L;

    public YAMLAnchorReplayingFactory() {
        super();
    }

    public YAMLAnchorReplayingFactory(ObjectCodec oc) {
        super(oc);
    }

    public YAMLAnchorReplayingFactory(YAMLFactory src, ObjectCodec oc) {
        super(src, oc);
    }

    protected YAMLAnchorReplayingFactory(YAMLFactoryBuilder b) {
        super(b);
    }

    @Override
    public YAMLAnchorReplayingFactory copy() {
        _checkInvalidCopy(YAMLAnchorReplayingFactory.class);
        return new YAMLAnchorReplayingFactory(this, (ObjectCodec) null);
    }

    @Override
    protected Object readResolve() {
        return new YAMLAnchorReplayingFactory(this, _objectCodec);
    }

    @Override
    protected YAMLParser _createParser(InputStream input, IOContext ctxt) throws IOException {
        return new YAMLAnchorReplayingParser(ctxt, _parserFeatures, _yamlParserFeatures,
                _loaderOptions, _objectCodec,
                _createReader(input, (JsonEncoding) null, ctxt));
    }

    @Override
    protected YAMLParser _createParser(Reader r, IOContext ctxt) throws IOException {
        return new YAMLAnchorReplayingParser(ctxt, _parserFeatures, _yamlParserFeatures,
                _loaderOptions, _objectCodec, r);
    }

    @Override
    protected YAMLParser _createParser(char[] data, int offset, int len, IOContext ctxt, boolean recyclable) throws IOException {
        return new YAMLAnchorReplayingParser(ctxt, _parserFeatures, _yamlParserFeatures,
                _loaderOptions, _objectCodec, new CharArrayReader(data, offset, len));
    }

    @Override
    protected YAMLParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        return new YAMLAnchorReplayingParser(ctxt, _parserFeatures, _yamlParserFeatures,
                _loaderOptions, _objectCodec, _createReader(data, offset, len, (JsonEncoding) null, ctxt));
    }
}
