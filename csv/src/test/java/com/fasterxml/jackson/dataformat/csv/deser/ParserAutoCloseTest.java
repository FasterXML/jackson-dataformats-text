package com.fasterxml.jackson.dataformat.csv.deser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParserAutoCloseTest extends ModuleTestBase
{

    @Test
    public void testParseReaderWithAutoClose() throws IOException {
        ObjectMapper mapper = mapperForCsv();

        CloseTrackerReader reader = new CloseTrackerReader("foo,bar");
        mapper.readTree(reader);

        assertEquals(true, reader.isClosed());
        reader.close();
    }

    @Test
    public void testParseStreamWithAutoClose() throws IOException {
        ObjectMapper mapper = mapperForCsv();

        CloseTrackerOutputStream stream = new CloseTrackerOutputStream("foo,bar");
        mapper.readTree(stream);

        assertEquals(true, stream.isClosed());
        stream.close();
    }

    @Test
    public void testParseReaderWithoutAutoClose() throws IOException {
        ObjectMapper mapper = mapperBuilder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build();

        CloseTrackerReader reader = new CloseTrackerReader("foo,bar");
        mapper.readTree(reader);

        assertEquals(false, reader.isClosed());
        reader.close();
    }


    @Test
    public void testParseStreamWithoutAutoClose() throws IOException {
        ObjectMapper mapper = mapperBuilder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build();

        CloseTrackerOutputStream stream = new CloseTrackerOutputStream("foo,bar");
        mapper.readTree(stream);

        assertEquals(false, stream.isClosed());
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
            super(s.getBytes(StandardCharsets.UTF_8));
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
