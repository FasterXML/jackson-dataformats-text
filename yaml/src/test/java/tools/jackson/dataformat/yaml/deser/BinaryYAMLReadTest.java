package tools.jackson.dataformat.yaml.deser;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;

import tools.jackson.core.*;

import tools.jackson.databind.*;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.dataformat.yaml.ModuleTestBase;

public class BinaryYAMLReadTest extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newObjectMapper();

    public void testBinaryViaTree() throws Exception
    {
        final String BASE64 = " R0lGODlhDAAMAIQAAP//9/X1\n"
+" 7unp5WZmZgAAAOfn515eXvPz\n"
+" 7Y6OjuDg4J+fn5OTk6enp56e\n"
+" nmleECcgggoBADs=";
        final String DOC = String.format(
"---\n"
+"picture: !!binary |\n"
+"%s\n", BASE64);

        JsonNode bean = null;
        try {
            bean = MAPPER.readTree(DOC);
        } catch (JacksonException e) {
            fail("Should have decoded properly, instead got "+e);
        }
        final JsonNode picture = bean.get("picture");
        assertNotNull(picture);

        assertEquals(JsonNodeType.BINARY, picture.getNodeType());
        final byte[] gif = picture.binaryValue();
        assertNotNull(gif);
        assertEquals(65, gif.length);
        final byte[] actualFileHeader = Arrays.copyOfRange(gif, 0, 6);
        final byte[] expectedFileHeader = new byte[]{'G', 'I', 'F', '8', '9', 'a'};    
        Assert.assertArrayEquals(expectedFileHeader, actualFileHeader);
    }

    // [dataformats-text#90]
    public void testReadLongBinary() throws Exception {
        final byte[] data = new byte[1000];
        new Random(1234).nextBytes(data);

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try (JsonGenerator gen =  MAPPER.createGenerator(os)) {
            gen.writeStartObject();
            gen.writeBinaryProperty("data", data);
            gen.writeEndObject();
            gen.close();
        }

        try (JsonParser parser = MAPPER.createParser(os.toByteArray())) {
            assertEquals(JsonToken.START_OBJECT, parser.nextToken());
            assertEquals(JsonToken.PROPERTY_NAME, parser.nextToken());
            assertEquals("data", parser.currentName());
            assertEquals(JsonToken.VALUE_EMBEDDED_OBJECT, parser.nextToken());
            Assert.assertArrayEquals(data, parser.getBinaryValue());
            assertEquals(JsonToken.END_OBJECT, parser.nextToken());
            assertNull(parser.nextToken());
        }
    }
}
