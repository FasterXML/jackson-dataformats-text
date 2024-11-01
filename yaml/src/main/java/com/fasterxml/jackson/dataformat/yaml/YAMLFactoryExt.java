package com.fasterxml.jackson.dataformat.yaml;

import java.io.CharArrayReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;

/**
 * A subclass of YAMLFactory with the only purpose to replace the YAMLParser by the YAMLParserExt subclass
 */
public class YAMLFactoryExt extends YAMLFactory {
    public YAMLFactoryExt() {
        super();
    }

    public YAMLFactoryExt(ObjectCodec oc) {
        super(oc);
    }

    public YAMLFactoryExt(YAMLFactory src, ObjectCodec oc) {
        super(src, oc);
    }

    protected YAMLFactoryExt(YAMLFactoryBuilder b) {
        super(b);
    }

    @Override
    public YAMLFactoryExt copy() {
        this._checkInvalidCopy(YAMLFactoryExt.class);
        return new YAMLFactoryExt(this, (ObjectCodec) null);
    }

    @Override
    protected Object readResolve() {
        return new YAMLFactoryExt(this, this._objectCodec);
    }

    @Override
    protected YAMLParser _createParser(InputStream input, IOContext ctxt) throws IOException {
        return new YAMLParserExt(ctxt, this._parserFeatures, this._yamlParserFeatures, this._loaderOptions, this._objectCodec, this._createReader(input, (JsonEncoding) null, ctxt));
    }

    @Override
    protected YAMLParser _createParser(Reader r, IOContext ctxt) throws IOException {
        return new YAMLParserExt(ctxt, this._parserFeatures, this._yamlParserFeatures, this._loaderOptions, this._objectCodec, r);
    }

    @Override
    protected YAMLParser _createParser(char[] data, int offset, int len, IOContext ctxt, boolean recyclable) throws IOException {
        return new YAMLParserExt(ctxt, this._parserFeatures, this._yamlParserFeatures, this._loaderOptions, this._objectCodec, new CharArrayReader(data, offset, len));
    }

    @Override
    protected YAMLParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        return new YAMLParserExt(ctxt, this._parserFeatures, this._yamlParserFeatures, this._loaderOptions, this._objectCodec, this._createReader(data, offset, len, (JsonEncoding) null, ctxt));
    }
}
