package com.fasterxml.jackson.dataformat.yaml.ser;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import org.junit.Assert;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class GeneratorAutoCloseTest extends ModuleTestBase {

    private Pojo pojo = new Pojo("bar");

    public void testGenerateWriterWithoAutoCloseTarget() throws IOException {
        CloseTrackerWriter writer = new CloseTrackerWriter();
        ObjectMapper yamlMapper = newObjectMapper();
        yamlMapper.writeValue(writer, pojo);
        Assert.assertEquals(true, writer.isClosed());
    }

    public void testGenerateOutputStreamWithAutoCloseTarget() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream();
        ObjectMapper yamlMapper = newObjectMapper();
        yamlMapper.writeValue(stream, pojo);
        Assert.assertEquals(true, stream.isClosed());
    }

    public void testGenerateWriterWithoutAutoCloseTarget() throws IOException {
        CloseTrackerWriter writer = new CloseTrackerWriter();
        ObjectMapper yamlMapper = newObjectMapper()
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        yamlMapper.writeValue(writer, pojo);
        Assert.assertEquals(false, writer.isClosed());
    }

    public void testGenerateOutputStreamWithoutAutoCloseTarget() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream();
        ObjectMapper yamlMapper = newObjectMapper()
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        yamlMapper.writeValue(stream, pojo);
        Assert.assertEquals(false, stream.isClosed());
    }

    public void testGenerateOutputStreamWithoutAutoCloseTargetOnFactory() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream();
        ObjectMapper yamlMapper = new ObjectMapper(
                new YAMLFactory()
                        .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
        );
        yamlMapper.writeValue(stream, pojo);
        Assert.assertEquals(false, stream.isClosed());
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