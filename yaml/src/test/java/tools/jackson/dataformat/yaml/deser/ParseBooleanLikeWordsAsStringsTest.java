package tools.jackson.dataformat.yaml.deser;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;

// NOTE: Jackson 3.0 behavior differs from 2.x due to changes in the
// underlying "snakeyaml-engine" behavior
public class ParseBooleanLikeWordsAsStringsTest extends ModuleTestBase
{
    
    final String YAML =
            "one: Yes\n" +
            "two: No\n" +
            "three: Off\n" +
            "four: On\n" +
            "five: true\n" +
            "six: false\n" +
            "seven: Y\n" +
            "eight: N\n";

    public void testParseBooleanLikeWordsAsStringDefault() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        ObjectMapper mapper = new ObjectMapper(f);

        JsonNode root = mapper.readTree(YAML);
        assertEquals(root.get("one").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("one").textValue(), "Yes");

        assertEquals(root.get("two").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("two").textValue(), "No");

        assertEquals(root.get("three").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("three").textValue(), "Off");

        assertEquals(root.get("four").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("four").textValue(), "On");

        assertEquals(root.get("five").getNodeType(), JsonNodeType.BOOLEAN);
        assertTrue(root.get("five").booleanValue());

        assertEquals(root.get("six").getNodeType(), JsonNodeType.BOOLEAN);
        assertFalse(root.get("six").booleanValue());

        assertEquals(root.get("seven").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("seven").textValue(), "Y");

        assertEquals(root.get("eight").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("eight").textValue(), "N");
    }
}
