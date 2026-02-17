package tools.jackson.dataformat.yaml.ser;

import java.io.*;

import org.junit.jupiter.api.Test;

import org.snakeyaml.engine.v2.common.SpecVersion;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.dataformat.yaml.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to ensure that SnakeYAML exceptions from the emitter are properly
 * wrapped in Jackson exceptions and not leaked to callers.
 */
public class GeneratorExceptionHandlingTest extends ModuleTestBase
{
    private final YAMLMapper MAPPER = newObjectMapper();

    /**
     * This test verifies that if the underlying writer fails,
     * we get a Jackson exception, not a SnakeYAML exception.
     * The test triggers an IOException during emission which should cause
     * SnakeYAML to throw a YamlEngineException, which we then wrap.
     */
    @Test
    public void testWriterIOExceptionWrapping() throws Exception
    {
        // We need to let initial writes through for document start markers,
        // then fail to trigger YamlEngineException during actual content emission
        final int ALLOWED_INITIAL_WRITES = 5;
        
        // Create a writer that will fail during write operations
        Writer failingWriter = new Writer() {
            private int callCount = 0;
            
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                // Let a few initial writes through (document markers, etc)
                // then fail to trigger exception during content emission
                if (++callCount > ALLOWED_INITIAL_WRITES) {
                    throw new IOException("Simulated write failure");
                }
            }

            @Override
            public void flush() throws IOException {
                // Allow flush
            }

            @Override
            public void close() throws IOException {
                // Allow close
            }
        };

        try (JsonGenerator gen = MAPPER.createGenerator(failingWriter)) {
            // Try to write something that will trigger the emitter
            gen.writeStartObject();
            gen.writeStringProperty("test", "value");
            gen.writeStringProperty("test2", "value2"); 
            gen.writeStringProperty("test3", "value3"); // More writes to trigger failure
            gen.writeEndObject();
            fail("Should have thrown an exception");
        } catch (JacksonIOException e) {
            // Expected: our custom exception wrapping the underlying failure
            assertNotNull(e.getMessage());
            // Verify it has the cause
            assertNotNull(e.getCause());
        }
    }

    /**
     * Test that a {@link YamlEngineException} thrown by the SnakeYAML emitter
     * (not caused by an IOException) is caught and wrapped as a
     * {@link JacksonYAMLWriteException}.
     *<p>
     * We trigger this by configuring an unsupported YAML version (major != 1),
     * which causes the emitter to throw an {@code EmitterException}
     * (subclass of {@code YamlEngineException}) when it tries to emit the
     * document start event.
     */
    @Test
    public void testYamlEngineExceptionWrapping() throws Exception
    {
        // SpecVersion with major=2 is unsupported by the emitter
        YAMLFactory factory = YAMLFactory.builder()
                .yamlVersionToWrite(new SpecVersion(2, 0))
                .build();
        YAMLMapper mapper = YAMLMapper.builder(factory).build();

        try {
            mapper.writeValueAsString(java.util.Map.of("key", "value"));
            fail("Should have thrown an exception");
        } catch (JacksonYAMLWriteException e) {
            // Expected: SnakeYAML EmitterException wrapped as JacksonYAMLWriteException
            assertTrue(e.getMessage().contains("unsupported YAML version"),
                    "Message should mention unsupported YAML version, got: " + e.getMessage());
            assertInstanceOf(YamlEngineException.class, e.getCause());
        } catch (YamlEngineException e) {
            fail("YamlEngineException should not leak; should be wrapped as JacksonYAMLWriteException: " + e);
        }
    }
}
