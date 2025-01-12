package com.fasterxml.jackson.dataformat.yaml.ser;

import java.io.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("resource")
public class GeneratorAutoCloseTest extends ModuleTestBase
{

    private Pojo pojo = new Pojo("bar");

    @Test
    public void testGenerateWriterWithoAutoCloseTarget() throws IOException {
        CloseTrackerWriter writer = new CloseTrackerWriter();
        ObjectMapper yamlMapper = newObjectMapper();
        yamlMapper.writeValue(writer, pojo);
        assertEquals(true, writer.isClosed());
    }

    @Test
    public void testGenerateOutputStreamWithAutoCloseTarget() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream();
        ObjectMapper yamlMapper = newObjectMapper();
        yamlMapper.writeValue(stream, pojo);
        assertEquals(true, stream.isClosed());
    }

    @Test
    public void testGenerateWriterWithoutAutoCloseTarget() throws IOException {
        CloseTrackerWriter writer = new CloseTrackerWriter();
        ObjectMapper yamlMapper = newObjectMapper()
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        yamlMapper.writeValue(writer, pojo);
        assertEquals(false, writer.isClosed());
    }

    @Test
    public void testGenerateOutputStreamWithoutAutoCloseTarget() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream();
        ObjectMapper yamlMapper = newObjectMapper()
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        yamlMapper.writeValue(stream, pojo);
        assertEquals(false, stream.isClosed());
    }

    @Test
    public void testGenerateOutputStreamWithoutAutoCloseTargetOnFactory() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream();
        ObjectMapper yamlMapper = new ObjectMapper(
                new YAMLFactory()
                        .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
        );
        yamlMapper.writeValue(stream, pojo);
        assertEquals(false, stream.isClosed());
    }


    static class CloseTrackerOutputStream extends OutputStream {
        private boolean closed;

        @Override
        public void write(int b) throws IOException {

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

    static class CloseTrackerWriter extends StringWriter {
        private boolean closed;


        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        public boolean isClosed() {
            return closed;
        }
    }

    static class Pojo {

        public final String foo;

        Pojo(final String foo) {
            this.foo = foo;
        }
    }
}