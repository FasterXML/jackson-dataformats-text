package com.fasterxml.jackson.dataformat.yaml.ser;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import org.junit.Assert;

import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@SuppressWarnings("resource")
public class GeneratorAutoCloseTest extends ModuleTestBase
{

    private Pojo pojo = new Pojo("bar");

    public void testGenerateWriterWithoAutoCloseTarget() throws IOException {
        CloseTrackerWriter writer = new CloseTrackerWriter();
        ObjectMapper yamlMapper = newObjectMapper();
        yamlMapper.writeValue(writer, pojo);
        Assert.assertEquals(true, writer.isClosed());
        writer.close();
    }

    public void testGenerateOutputStreamWithAutoCloseTarget() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream();
        ObjectMapper yamlMapper = newObjectMapper();
        yamlMapper.writeValue(stream, pojo);
        Assert.assertEquals(true, stream.isClosed());
        stream.close();
    }

    public void testGenerateWriterWithoutAutoCloseTarget() throws IOException {
        CloseTrackerWriter writer = new CloseTrackerWriter();
        ObjectMapper yamlMapper = mapperBuilder()
                .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
                .build();
        yamlMapper.writeValue(writer, pojo);
        Assert.assertEquals(false, writer.isClosed());
        writer.close();
    }

    public void testGenerateOutputStreamWithoutAutoCloseTarget() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream();
        ObjectMapper yamlMapper = mapperBuilder()
                .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
                .build();
        yamlMapper.writeValue(stream, pojo);
        Assert.assertEquals(false, stream.isClosed());
        stream.close();
    }

    public void testGenerateOutputStreamWithoutAutoCloseTargetOnFactory() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream();
        ObjectMapper yamlMapper = new ObjectMapper(
                YAMLFactory.builder()
                        .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
                        .build()
        );
        yamlMapper.writeValue(stream, pojo);
        Assert.assertEquals(false, stream.isClosed());
        stream.close();
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