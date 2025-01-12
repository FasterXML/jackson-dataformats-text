package com.fasterxml.jackson.dataformat.yaml.ser;

import java.io.StringWriter;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class BinaryWriteTest extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newObjectMapper();

    @Test
    public void testBinaryViaTree() throws Exception
    {
        byte[] srcPayload = new byte[] { 1, 2, 3, 4, 5 };
        ObjectNode root = MAPPER.createObjectNode();
        root.put("payload", srcPayload);
        String doc = MAPPER.writeValueAsString(root);

        // and read back
        final JsonNode bean = MAPPER.readTree(doc);
        final JsonNode data = bean.get("payload");
        assertNotNull(data);
        assertEquals(JsonNodeType.BINARY, data.getNodeType());
        final byte[] b = data.binaryValue();
        assertArrayEquals(srcPayload, b);
    }

    @Test
    public void testWriteLongBinary() throws Exception {
        final int length = 200;
        final byte[] data = new byte[length];
        Arrays.fill(data, (byte) 1);

        StringWriter w = new StringWriter();
        
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(w)) {
            gen.writeStartObject();
            gen.writeBinaryField("array", data);
            gen.writeEndObject();
            gen.close();
        }

        String yaml = w.toString();
        assertEquals("---\n" +
                "array: !!binary |-\n" +
                "  AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n" +
                "  AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n" +
                "  AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n" +
                "  AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=\n", yaml);

    }
}
