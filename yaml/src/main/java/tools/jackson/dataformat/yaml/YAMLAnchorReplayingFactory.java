package tools.jackson.dataformat.yaml;

import java.io.*;

import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.io.IOContext;

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

    public YAMLAnchorReplayingFactory(YAMLFactory src) {
        super(src);
    }

    protected YAMLAnchorReplayingFactory(YAMLAnchorReplayingFactoryBuilder b) {
        super(b);
    }

    @Override
    public YAMLAnchorReplayingFactoryBuilder rebuild() {
        return new YAMLAnchorReplayingFactoryBuilder(this);
    }

    /**
     * Main factory method to use for constructing {@link YAMLAnchorReplayingFactory} instances with
     * different configuration.
     */
    public static YAMLAnchorReplayingFactoryBuilder builder() {
        return new YAMLAnchorReplayingFactoryBuilder();
    }

    @Override
    public YAMLAnchorReplayingFactory copy() {
        return new YAMLAnchorReplayingFactory(this);
    }

    @Override
    protected Object readResolve() {
        return new YAMLAnchorReplayingFactory(this);
    }

    @Override
    protected YAMLAnchorReplayingParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt, InputStream in) {
        return new YAMLAnchorReplayingParser(readCtxt, ioCtxt,
                _getBufferRecycler(),
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                _loadSettings,
                _createReader(in, null, ioCtxt));
    }

    @Override
    protected YAMLAnchorReplayingParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt, Reader r) {
        return new YAMLAnchorReplayingParser(readCtxt, ioCtxt,
                _getBufferRecycler(),
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                _loadSettings,
                r);
    }

    @Override
    protected YAMLAnchorReplayingParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
                                       char[] data, int offset, int len,
                                       boolean recyclable) {
        return new YAMLAnchorReplayingParser(readCtxt, ioCtxt, _getBufferRecycler(),
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                _loadSettings,
                new CharArrayReader(data, offset, len));
    }

    @Override
    protected YAMLAnchorReplayingParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
                                       byte[] data, int offset, int len) {
        return new YAMLAnchorReplayingParser(readCtxt, ioCtxt, _getBufferRecycler(),
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                _loadSettings,
                _createReader(data, offset, len, null, ioCtxt));
    }
}
