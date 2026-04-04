package tools.jackson.dataformat.javaprop;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that Properties output streams are properly closed even when
 * an error occurs during generator close (see jackson-dataformats-text#109).
 *
 * @since 3.2
 */
public class GeneratorFileCloseOnErrorTest extends ModuleTestBase
{
    private final JavaPropsMapper MAPPER = newPropertiesMapper();

    // [jackson-dataformats-text#109]: Output not closed on error
    @Test
    public void testOutputClosedWhenFlushFailsDuringClose() throws Exception
    {
        FailOnDemandWriter writer = new FailOnDemandWriter();
        JsonGenerator gen = MAPPER.createGenerator(writer);
        gen.writeStartObject();
        gen.writeStringProperty("key", "value");
        gen.writeEndObject();

        // Arm the writer so close()'s _flushBuffer() call fails
        writer.shouldFail = true;

        try {
            gen.close();
        } catch (Exception e) {
            // Expected: writer failure during _flushBuffer in close()
        }
        assertThat(writer.closed)
            .as("Underlying writer should be closed even when flush during close() fails")
            .isTrue();
    }

    @Test
    public void testOutputClosedOnSuccessfulWrite() throws Exception
    {
        FailOnDemandWriter writer = new FailOnDemandWriter();
        MAPPER.writeValue(writer, new Point(1, 2));
        assertThat(writer.closed)
            .as("Writer should be closed after successful write")
            .isTrue();
    }

    static class FailOnDemandWriter extends Writer {
        boolean closed;
        volatile boolean shouldFail;
        private final StringWriter _delegate = new StringWriter();

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (shouldFail) {
                throw new IOException("Simulated write failure");
            }
            _delegate.write(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            _delegate.flush();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            _delegate.close();
        }
    }
}
