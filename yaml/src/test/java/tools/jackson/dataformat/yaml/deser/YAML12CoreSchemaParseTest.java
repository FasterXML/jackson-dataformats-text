package tools.jackson.dataformat.yaml.deser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonToken;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLParser;
import tools.jackson.dataformat.yaml.YAMLReadFeature;
import tools.jackson.dataformat.yaml.YAMLSchema;

/**
 * Test YAML parsing when using the YAML 1.2 core schema (i.e. CoreScalarResolver).
 */
public class YAML12CoreSchemaParseTest extends ModuleTestBase {
    private final YAMLFactory FACTORY = YAMLFactory.builder().yamlSchema(YAMLSchema.CORE).enable(YAMLReadFeature.EMPTY_STRING_AS_NULL).build();
    private final YAMLMapper  MAPPER  = YAMLMapper.builder(FACTORY).build();

    @Test
    public void testTokens() throws Exception {
        final String YAML = """
        ---
        nulls:
        - null
        - Null
        - NULL
        - ~
        - # empty
        booleans:
        - true
        - True
        - TRUE
        - false
        - False
        - FALSE
        integers:
        - 0
        - 42
        - -1
        - 0o755
        - 0xDEADBEEF
        floats:
        - 0.0
        - 3.14159
        - 2.998e8
        - 1.0e-10
        infinities:
        - .inf
        - .Inf
        - .INF
        - +.inf
        - +.Inf
        - +.INF
        - -.inf
        - -.Inf
        - -.INF
        nans:
        - .nan
        - .NaN
        - .NAN
        strings:
        - hello, world!
        - Infinity
        - NaN
        - 'null'
        - 'false'
        - '0'
        - '-1.0'
        - '0o644'
        - '0xFF'
        """;

        YAMLParser p = (YAMLParser) MAPPER.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("nulls", p.currentName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NULL,  p.nextToken());
        assertToken(JsonToken.VALUE_NULL,  p.nextToken());
        assertToken(JsonToken.VALUE_NULL,  p.nextToken());
        assertToken(JsonToken.VALUE_NULL,  p.nextToken());
        assertToken(JsonToken.VALUE_NULL,  p.nextToken());
        assertToken(JsonToken.END_ARRAY,   p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("booleans", p.currentName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE,  p.nextToken());
        assertToken(JsonToken.VALUE_TRUE,  p.nextToken());
        assertToken(JsonToken.VALUE_TRUE,  p.nextToken());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertToken(JsonToken.END_ARRAY,   p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("integers", p.currentName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(0, p.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(42, p.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(-1, p.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(493, p.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3735928559L, p.getLongValue());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("floats", p.currentName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(0.0, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(3.14159, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(2.998e8, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(1.0e-10, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("infinities", p.currentName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(Double.POSITIVE_INFINITY, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(Double.POSITIVE_INFINITY, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(Double.POSITIVE_INFINITY, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(Double.POSITIVE_INFINITY, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(Double.POSITIVE_INFINITY, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(Double.POSITIVE_INFINITY, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(Double.NEGATIVE_INFINITY, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(Double.NEGATIVE_INFINITY, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(Double.NEGATIVE_INFINITY, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("nans", p.currentName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(Double.NaN, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(Double.NaN, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(Double.NaN, p.getDoubleValue(), 0.0);
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("strings", p.currentName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("hello, world!", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("Infinity", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("NaN", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("null", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("false", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("0", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("-1.0", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("0o644", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("0xFF", p.getString());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
    }
}
