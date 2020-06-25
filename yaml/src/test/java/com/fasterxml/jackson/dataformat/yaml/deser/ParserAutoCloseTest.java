package com.fasterxml.jackson.dataformat.yaml.deser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Assert;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

@SuppressWarnings("resource")
public class ParserAutoCloseTest extends ModuleTestBase
{
    private final ObjectMapper YAML_MAPPER = newObjectMapper();

    public void testParseReaderWithAutoClose() throws IOException {

        CloseTrackerReader reader = new CloseTrackerReader("foo:bar");
        YAML_MAPPER.readTree(reader);

        Assert.assertEquals(true, reader.isClosed());
    }

    public void testParseStreamWithAutoClose() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream("foo:bar");
        YAML_MAPPER.readTree(stream);

        Assert.assertEquals(true, stream.isClosed());
    }

    public void testParseReaderWithoutAutoClose() throws IOException {
        CloseTrackerReader reader = new CloseTrackerReader("foo:bar");
        YAML_MAPPER.reader()
            .without(JsonParser.Feature.AUTO_CLOSE_SOURCE)
            .readTree(reader);

        Assert.assertEquals(false, reader.isClosed());
    }

    public void testParseStreamWithoutAutoClose() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream("foo:bar");
        YAML_MAPPER.reader()
            .without(JsonParser.Feature.AUTO_CLOSE_SOURCE)
            .readTree(stream);

        Assert.assertEquals(false, stream.isClosed());
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
