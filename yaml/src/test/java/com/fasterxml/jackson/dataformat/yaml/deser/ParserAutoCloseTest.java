package com.fasterxml.jackson.dataformat.yaml.deser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Assert;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

public class ParserAutoCloseTest extends ModuleTestBase {

    public void testParseReaderWithAutoClose() throws IOException {
        ObjectMapper yamlMapper = newObjectMapper();

        CloseTrackerReader reader = new CloseTrackerReader("foo:bar");
        yamlMapper.readTree(reader);

        Assert.assertEquals(true, reader.isClosed());
        reader.close();
    }

    public void testParseStreamWithAutoClose() throws IOException {
        ObjectMapper yamlMapper = newObjectMapper();

        CloseTrackerOutputStream stream = new CloseTrackerOutputStream("foo:bar");
        yamlMapper.readTree(stream);

        Assert.assertEquals(true, stream.isClosed());
        stream.close();
    }

    public void testParseReaderWithoutAutoClose() throws IOException {
        ObjectMapper yamlMapper = newMapperBuilder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build();

        CloseTrackerReader reader = new CloseTrackerReader("foo:bar");
        yamlMapper.readTree(reader);

        Assert.assertEquals(false, reader.isClosed());
        reader.close();
    }


    public void testParseStreamWithoutAutoClose() throws IOException {
        ObjectMapper yamlMapper = newMapperBuilder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build();

        CloseTrackerOutputStream stream = new CloseTrackerOutputStream("foo:bar");
        yamlMapper.readTree(stream);

        Assert.assertEquals(false, stream.isClosed());
        stream.close();
    }

    public static class CloseTrackerReader extends StringReader {
        private boolean closed;

        public CloseTrackerReader(String s) {
            super(s);
        }

        @Override
        public void close() {
            closed = true;
            super.close();
        }

        public boolean isClosed() {
            return closed;
        }
    }

    public static class CloseTrackerOutputStream extends ByteArrayInputStream {
        private boolean closed;

        public CloseTrackerOutputStream(String s) {
            super(s.getBytes());
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        public boolean isClosed() {
            return closed;
        }
    }
}
