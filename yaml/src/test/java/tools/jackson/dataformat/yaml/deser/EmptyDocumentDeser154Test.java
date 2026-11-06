package tools.jackson.dataformat.yaml.deser;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MappingIterator;
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

    // END_OBJECT must remain the current token, within proper context, until next call
    @Test
    public void testTokenStreamWithFeature() throws Exception
    {
        _verifyTokenStream(MAPPER_WITH_EMPTY_AS_OBJECT);
    }

    @Test
    public void testTokenStreamWithFeatureAnchorReplaying() throws Exception
    {
        _verifyTokenStream(mapperBuilder(new YAMLAnchorReplayingFactory())
                .enable(YAMLReadFeature.EMPTY_DOCUMENT_AS_EMPTY_OBJECT)
                .build());
    }

    private void _verifyTokenStream(YAMLMapper mapper) throws Exception
    {
        try (JsonParser p = mapper.createParser("# just a comment")) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertTrue(p.streamReadContext().inObject());

            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.currentToken());
            assertTrue(p.streamReadContext().inRoot());
            assertFalse(p.isClosed());

            assertNull(p.nextToken());
            assertNull(p.currentToken());
            assertTrue(p.isClosed());

            assertNull(p.nextToken());
        }
    }

    @Test
    public void testReadValuesWithFeature() throws Exception
    {
        try (MappingIterator<DataWithDefaults> it = MAPPER_WITH_EMPTY_AS_OBJECT
                .readerFor(DataWithDefaults.class)
                .readValues("# just a comment")) {
            List<DataWithDefaults> all = it.readAll();
            assertEquals(1, all.size());
            assertEquals(42, all.get(0).count);
        }
    }

    @Test
    public void testReadTreeWithFeature() throws Exception
    {
        JsonNode root = MAPPER_WITH_EMPTY_AS_OBJECT.readTree("");
        assertTrue(root.isObject());
        assertTrue(root.isEmpty());
    }

    @Test
    public void testTokenClearingDisabledWithFeature() throws Exception
    {
        YAMLMapper mapper = YAMLMapper.builder()
                .enable(YAMLReadFeature.EMPTY_DOCUMENT_AS_EMPTY_OBJECT)
                .disable(StreamReadFeature.CLEAR_CURRENT_TOKEN_ON_CLOSE)
                .build();
        assertEquals(42, mapper.readValue("", DataWithDefaults.class).count);
        _verifyTokenStream(mapper);
    }

    // Alias is content too: document must not be considered empty
    @Test
    public void testAliasOnlyDocumentNotEmpty() throws Exception
    {
        assertEquals("a", MAPPER_WITH_EMPTY_AS_OBJECT.readValue("*a", String.class));

        try (JsonParser p = MAPPER_WITH_EMPTY_AS_OBJECT.createParser("*a\n---\n*b")) {
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("a", p.getString());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("b", p.getString());
            assertNull(p.nextToken());
        }
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
