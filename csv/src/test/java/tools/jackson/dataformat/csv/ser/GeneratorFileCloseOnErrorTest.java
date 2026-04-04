package tools.jackson.dataformat.csv.ser;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.ModuleTestBase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that CSV output streams are properly closed even when
 * an error occurs during generator close (see jackson-dataformats-text#109).
 *
 * @since 3.2
 */
public class GeneratorFileCloseOnErrorTest extends ModuleTestBase
{
    private final CsvMapper MAPPER = newObjectMapper();

    // [jackson-dataformats-text#109]: Output not closed on error
    @Test
    public void testOutputClosedWhenFlushFailsDuringClose() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("key")
                .build();

        FailOnDemandWriter writer = new FailOnDemandWriter();
        JsonGenerator gen = MAPPER.writer(schema).createGenerator(writer);
        gen.writeStartObject();
        gen.writeStringProperty("key", "value");
        gen.writeEndObject();

        // Arm the writer so close()'s finishRow/flush calls fail
        writer.shouldFail = true;

        try {
            gen.close();
        } catch (Exception e) {
            // Expected: writer failure during finishRow in close()
        }
        assertThat(writer.closed)
            .as("Underlying writer should be closed even when finishRow during close() fails")
            .isTrue();
    }

    @Test
    public void testOutputClosedOnSuccessfulWrite() throws Exception
    {
        FailOnDemandWriter writer = new FailOnDemandWriter();
        CsvSchema schema = MAPPER.schemaFor(Pojo.class);
        MAPPER.writer(schema).writeValue(writer, new Pojo("bar"));
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
            if (shouldFail) {
                throw new IOException("Simulated flush failure");
            }
            _delegate.flush();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            _delegate.close();
        }
    }

    static class Pojo {
        public String foo;

        public Pojo() { }
        Pojo(String foo) {
            this.foo = foo;
        }
    }
}
