package com.fasterxml.jackson.dataformat.javaprop;

import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.io.JPropWriteContext;

public class SimpleStreamingTest extends ModuleTestBase
{
    private final ObjectMapper MAPPER = mapperForProps();

    private final JavaPropsFactory F = new JavaPropsFactory();

    public void testParsing() throws Exception
    {
        JsonParser p = F.createParser("foo = bar");
        Object src = p.getInputSource();
        assertTrue(src instanceof Reader);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertNull(p.getEmbeddedObject());
        assertNotNull(p.getCurrentLocation()); // N/A
        assertNotNull(p.getTokenLocation()); // N/A
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("foo", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        StringWriter sw = new StringWriter();
        assertEquals(3, p.getText(sw));
        assertEquals("bar", sw.toString());
        p.close();
        assertTrue(p.isClosed());

        // one more thing, verify handling of non-binary
        p = F.createParser("foo = bar");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        try {
            p.getBinaryValue();
            fail("Should not pass");
        } catch (JsonProcessingException e) {
            verifyException(e, "can not access as binary");
        }
        try {
            p.getDoubleValue();
            fail("Should not pass");
        } catch (JsonProcessingException e) {
            verifyException(e, "can not use numeric");
        }
        p.close();
    }

    public void testStreamingGeneration() throws Exception
    {
        StringWriter strw = new StringWriter();
        JsonGenerator gen = F.createGenerator(strw);

        Object target = gen.getOutputTarget();
        assertTrue(target instanceof Writer);
        
        gen.writeStartObject();
        gen.writeBooleanField("flagTrue", true);
        gen.writeBooleanField("flagFalse", false);
        gen.writeNullField("null");
        gen.writeNumberField("long", 10L);
        gen.writeNumberField("int", 10);
        gen.writeNumberField("double", 0.25);
        gen.writeNumberField("float", 0.5f);
        gen.writeNumberField("decimal", BigDecimal.valueOf(0.125));
        gen.writeFieldName(new SerializedString("bigInt"));
        gen.writeNumber(BigInteger.valueOf(123));
        gen.writeFieldName("numString");
        gen.writeNumber("123.0");
        gen.writeFieldName("charString");
        gen.writeString(new char[] { 'a', 'b', 'c' }, 1, 2);

        gen.writeFieldName("arr");
        gen.writeStartArray();

        JsonStreamContext ctxt = gen.getOutputContext();
        String path = ctxt.toString();
        assertTrue(ctxt instanceof JPropWriteContext);
        // Note: this context gives full path, unlike many others
        assertEquals("/arr/0", path);
        
        gen.writeEndArray();

        gen.writeEndObject();
        assertFalse(gen.isClosed());
        gen.flush();
        gen.close();

        String props = strw.toString();

        // Plus read back for fun
        Map<?,?> stuff = MAPPER.readValue(props, Map.class);
        assertEquals(11, stuff.size());
        assertEquals("10", stuff.get("long"));
    }

    public void testStreamingGenerationRaw() throws Exception
    {
        StringWriter strw = new StringWriter();
        JsonGenerator gen = F.createGenerator(strw);

        String COMMENT = "# comment!\n";
        gen.writeRaw(COMMENT);
        gen.writeRaw(new SerializedString(COMMENT));
        gen.writeRaw(COMMENT, 0, COMMENT.length());
        gen.writeRaw('#');
        gen.writeRaw('\n');

        gen.writeStartObject();
        gen.writeBooleanField("enabled", true);
        gen.writeEndObject();
        
        gen.close();

        assertEquals(COMMENT + COMMENT + COMMENT
                + "#\nenabled=true\n", strw.toString());

        // Plus read back for fun
        Map<?,?> stuff = MAPPER.readValue(strw.toString(), Map.class);
        assertEquals(1, stuff.size());
        assertEquals("true", stuff.get("enabled"));
    }        

    public void testStreamingLongRaw() throws Exception
    {
        StringWriter strw = new StringWriter();
        JsonGenerator gen = F.createGenerator(strw);

        StringBuilder sb = new StringBuilder();
        sb.append("# ");
        for (int i = 0; i < 12000; ++i) {
            sb.append('a');
        }
        gen.writeRaw(sb.toString());
        gen.close();

        assertEquals(sb.toString(), strw.toString());
    }        
}
