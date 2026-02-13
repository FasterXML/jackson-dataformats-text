package tools.jackson.dataformat.yaml.ser;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
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
    public void testWriterFailureWrapping() throws Exception
    {
        // We need to let initial writes through for document start markers,
        // then fail to trigger YamlEngineException during actual content emission
        final int ALLOWED_INITIAL_WRITES = 5;
        
        // Create a writer that will fail during write operations
        Writer failingWriter = new Writer() {
            private int callCount = 0;
            
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                callCount++;
                // Let a few initial writes through (document markers, etc)
                // then fail to trigger exception during content emission
                if (callCount > ALLOWED_INITIAL_WRITES) {
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

        JsonGenerator gen = null;
        try {
            gen = MAPPER.createGenerator(failingWriter);
            // Try to write something that will trigger the emitter
            gen.writeStartObject();
            gen.writeStringProperty("test", "value");
            gen.writeStringProperty("test2", "value2"); 
            gen.writeStringProperty("test3", "value3"); // More writes to trigger failure
            gen.writeEndObject();
            gen.close();
            fail("Should have thrown an exception");
        } catch (JacksonYAMLWriteException e) {
            // Expected: our custom exception wrapping the underlying failure
            assertNotNull(e.getMessage());
            // Verify it has the cause
            assertNotNull(e.getCause());
        } catch (org.snakeyaml.engine.v2.exceptions.YamlEngineException e) {
            fail("Should not leak SnakeYAML exception: " + e);
        } catch (Exception e) {
            // Other Jackson exceptions are acceptable (e.g., if caught at a different layer)
            // but not SnakeYAML exceptions
            assertFalse(e.getClass().getName().contains("snakeyaml"),
                    "Should not leak SnakeYAML exception: " + e);
        } finally {
            if (gen != null) {
                try {
                    gen.close();
                } catch (Exception e) {
                    // Ignore close exceptions in cleanup
                }
            }
        }
    }

    /**
     * Basic sanity test that normal generation still works.
     */
    @Test
    public void testNormalGenerationWorks() throws Exception
    {
        StringWriter w = new StringWriter();
        try (JsonGenerator gen = MAPPER.createGenerator(w)) {
            gen.writeStartObject();
            gen.writeStringProperty("name", "test");
            gen.writeNumberProperty("value", 42);
            gen.writeEndObject();
        }

        String yaml = w.toString();
        assertNotNull(yaml);
        assertTrue(yaml.contains("name"));
        assertTrue(yaml.contains("test"));
        assertTrue(yaml.contains("value"));
        assertTrue(yaml.contains("42"));
    }
}
