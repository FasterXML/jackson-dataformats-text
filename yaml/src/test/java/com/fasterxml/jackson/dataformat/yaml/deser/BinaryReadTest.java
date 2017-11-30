package com.fasterxml.jackson.dataformat.yaml.deser;

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
        final String BASE64 = " R0lGODlhDAAMAIQAAP//9/X\n"
+" 17unp5WZmZgAAAOfn515eXv\n"
+" Pz7Y6OjuDg4J+fn5OTk6enp\n"
+" 56enmleECcgggoBADs=";
        final String DOC = String.format(
"---\n"
+"picture: !!binary |\n"
+"%s\n", BASE64);
        
        final JsonNode bean = MAPPER.readTree(DOC);
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
