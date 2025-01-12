package tools.jackson.dataformat.yaml.ser;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;

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
        writer.close();
    }

    @Test
    public void testGenerateOutputStreamWithAutoCloseTarget() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream();
        ObjectMapper yamlMapper = newObjectMapper();
        yamlMapper.writeValue(stream, pojo);
        assertEquals(true, stream.isClosed());
        stream.close();
    }

    @Test
    public void testGenerateWriterWithoutAutoCloseTarget() throws IOException {
        CloseTrackerWriter writer = new CloseTrackerWriter();
        ObjectMapper yamlMapper = mapperBuilder()
                .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
                .build();
        yamlMapper.writeValue(writer, pojo);
        assertEquals(false, writer.isClosed());
        writer.close();
    }

    @Test
    public void testGenerateOutputStreamWithoutAutoCloseTarget() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream();
        ObjectMapper yamlMapper = mapperBuilder()
                .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
                .build();
        yamlMapper.writeValue(stream, pojo);
        assertEquals(false, stream.isClosed());
        stream.close();
    }

    @Test
    public void testGenerateOutputStreamWithoutAutoCloseTargetOnFactory() throws IOException {
        CloseTrackerOutputStream stream = new CloseTrackerOutputStream();
        ObjectMapper yamlMapper = new ObjectMapper(
                YAMLFactory.builder()
                        .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
                        .build()
        );
        yamlMapper.writeValue(stream, pojo);
        assertEquals(false, stream.isClosed());
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
