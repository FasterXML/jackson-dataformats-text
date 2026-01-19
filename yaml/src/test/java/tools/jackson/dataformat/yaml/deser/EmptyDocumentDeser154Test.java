package tools.jackson.dataformat.yaml.deser;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.dataformat.yaml.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [dataformats-text#154]: Empty YAML documents (comment-only or truly empty)
 * should be deserializable to POJOs with default values
 */
public class EmptyDocumentDeser154Test extends ModuleTestBase
{
    static class DataWithDefaults {
        public boolean wireframe = false;
        public String name = "default";
        public int count = 42;
    }

    private final YAMLMapper MAPPER = newObjectMapper();
    private final YAMLMapper MAPPER_WITH_EMPTY_AS_OBJECT = YAMLMapper.builder()
            .enable(YAMLReadFeature.EMPTY_DOCUMENT_AS_EMPTY_OBJECT)
            .build();

    // First, verify the default behavior (should still fail for backwards compatibility)
    @Test
    public void testCommentOnlyYamlFailsByDefault() throws Exception
    {
        String yaml = "#wireframe: true";

        // Without the feature enabled, this should still throw MismatchedInputException
        assertThrows(MismatchedInputException.class, () -> {
            MAPPER.readValue(yaml, DataWithDefaults.class);
        });
    }

    @Test
    public void testEmptyYamlFailsByDefault() throws Exception
    {
        String yaml = "";

        // Without the feature enabled, this should still throw MismatchedInputException
        assertThrows(MismatchedInputException.class, () -> {
            MAPPER.readValue(yaml, DataWithDefaults.class);
        });
    }

    @Test
    public void testWhitespaceOnlyYamlFailsByDefault() throws Exception
    {
        String yaml = "   \n   \n";

        // Without the feature enabled, this should still throw MismatchedInputException
        assertThrows(MismatchedInputException.class, () -> {
            MAPPER.readValue(yaml, DataWithDefaults.class);
        });
    }

    // Now test with the feature enabled - should work
    @Test
    public void testCommentOnlyYamlWithFeature() throws Exception
    {
        String yaml = "#wireframe: true";
        DataWithDefaults result = MAPPER_WITH_EMPTY_AS_OBJECT.readValue(yaml, DataWithDefaults.class);

        // Should get default values
        assertNotNull(result);
        assertFalse(result.wireframe);
        assertEquals("default", result.name);
        assertEquals(42, result.count);
    }

    @Test
    public void testEmptyYamlWithFeature() throws Exception
    {
        String yaml = "";
        DataWithDefaults result = MAPPER_WITH_EMPTY_AS_OBJECT.readValue(yaml, DataWithDefaults.class);

        // Should get default values
        assertNotNull(result);
        assertFalse(result.wireframe);
        assertEquals("default", result.name);
        assertEquals(42, result.count);
    }

    @Test
    public void testWhitespaceOnlyYamlWithFeature() throws Exception
    {
        String yaml = "   \n   \n";
        DataWithDefaults result = MAPPER_WITH_EMPTY_AS_OBJECT.readValue(yaml, DataWithDefaults.class);

        // Should get default values
        assertNotNull(result);
        assertFalse(result.wireframe);
        assertEquals("default", result.name);
        assertEquals(42, result.count);
    }

    @Test
    public void testMultipleCommentsOnlyYamlWithFeature() throws Exception
    {
        String yaml = "# This is a comment\n# Another comment\n  # Indented comment";
        DataWithDefaults result = MAPPER_WITH_EMPTY_AS_OBJECT.readValue(yaml, DataWithDefaults.class);

        // Should get default values
        assertNotNull(result);
        assertFalse(result.wireframe);
        assertEquals("default", result.name);
        assertEquals(42, result.count);
    }

    // Verify that valid YAML still works (with and without feature)
    @Test
    public void testValidYamlWorks() throws Exception
    {
        String yaml = "wireframe: true";

        DataWithDefaults result1 = MAPPER.readValue(yaml, DataWithDefaults.class);
        assertTrue(result1.wireframe);
        assertEquals("default", result1.name);
        assertEquals(42, result1.count);

        DataWithDefaults result2 = MAPPER_WITH_EMPTY_AS_OBJECT.readValue(yaml, DataWithDefaults.class);
        assertTrue(result2.wireframe);
        assertEquals("default", result2.name);
        assertEquals(42, result2.count);
    }

    @Test
    public void testPartialYamlWithFeature() throws Exception
    {
        String yaml = "name: custom";
        DataWithDefaults result = MAPPER_WITH_EMPTY_AS_OBJECT.readValue(yaml, DataWithDefaults.class);

        // Should get mix of provided and default values
        assertNotNull(result);
        assertFalse(result.wireframe);
        assertEquals("custom", result.name);
        assertEquals(42, result.count);
    }
}
