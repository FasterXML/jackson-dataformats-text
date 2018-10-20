package com.fasterxml.jackson.dataformat.yaml.deser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.Assert.assertArrayEquals;

public class BinaryReadTest extends ModuleTestBase
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
        } catch (IOException e) {
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

    public void testReadLongBinary() throws Exception {
        final byte[] data = new byte[1000];
        new Random().nextBytes(data);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonFactory factory = MAPPER.getFactory();
        JsonGenerator gen =  factory.createGenerator(os);

        gen.writeStartObject();
        gen.writeBinaryField("data", data);
        gen.writeEndObject();
        gen.close();

        try (JsonParser parser = factory.createParser(os.toByteArray())) {
            assertEquals(JsonToken.START_OBJECT, parser.nextToken());
            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("data", parser.currentName());
            assertEquals(JsonToken.VALUE_EMBEDDED_OBJECT, parser.nextToken());
            assertArrayEquals(data, parser.getBinaryValue());
            assertEquals(JsonToken.END_OBJECT, parser.nextToken());
            assertNull(parser.nextToken());
        }
    }
}
