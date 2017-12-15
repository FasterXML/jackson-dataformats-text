package com.fasterxml.jackson.dataformat.yaml.deser;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

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
}
