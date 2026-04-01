package tools.jackson.dataformat.yaml.ser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLSchema;

// Tests serialization of YAML 1.2 core schema.
public class YAML12CoreSchema623Test extends ModuleTestBase {
    private final YAMLFactory FACTORY = YAMLFactory.builder().yamlSchema(YAMLSchema.CORE).build();
    private final YAMLMapper  MAPPER  = YAMLMapper.builder(FACTORY).build();

    @Test
    public void testNull() throws Exception
    {
        Map<String, Object> map = new HashMap<>();
        map.put("key", null);
        String doc = MAPPER.writeValueAsString(map);
        doc = trimDocMarker(doc);
        assertEquals("key: null", doc);
    }

    @Test
    public void testBoolean() throws Exception
    {
        List<Boolean> list = List.of(true, false);
        String doc = MAPPER.writeValueAsString(list);
        doc = trimDocMarker(doc);
        String expected =
            "- true\n" +
            "- false";
        assertEquals(expected, doc);
    }

    @Test
    public void testNaN() throws Exception
    {
        List<Number> list = List.of(Float.NaN, Double.NaN);
        String doc = MAPPER.writeValueAsString(list);
        doc = trimDocMarker(doc);
        String expected =
            "- .nan\n" +
            "- .nan";
        assertEquals(expected, doc);
    }

    @Test
    public void testInfinity() throws Exception
    {
        List<Number> list = List.of(
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY);

        String doc = MAPPER.writeValueAsString(list);
        doc = trimDocMarker(doc);

        String expected =
            "- .inf\n"  +
            "- -.inf\n" +
            "- .inf\n"  +
            "- -.inf";
        assertEquals(expected, doc);
    }
}
