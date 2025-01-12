package com.fasterxml.jackson.dataformat.yaml.ser;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.yaml.*;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleGenerationTest extends ModuleTestBase
{
    private final YAMLFactory YAML_F = new YAMLFactory();

    @Test
    public void testStreamingArray() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = YAML_F.createGenerator(w);
        gen.writeStartArray();
        gen.writeNumber(3);
        gen.writeString("foobar");
        gen.writeEndArray();
        gen.close();

        String yaml = w.toString();
        // should probably parse?
        // note: 1.12 uses more compact notation; 1.10 has prefix
        yaml = trimDocMarker(yaml).trim();
        assertEquals("- 3\n- \"foobar\"", yaml);
    }

    @Test
    public void testStreamingObject() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = YAML_F.createGenerator(w);
        _writeBradDoc(gen);
        String yaml = w.toString();

        // note: 1.12 uses more compact notation; 1.10 has prefix
        yaml = trimDocMarker(yaml).trim();
        assertEquals("name: \"Brad\"\nage: 39", yaml);
        gen.close();
    }

    @Test
    public void testStreamingNested() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = YAML_F.createGenerator(w);

        gen.writeStartObject();
        gen.writeFieldName("ob");
        gen.writeStartArray();
        gen.writeString("a");
        gen.writeString("b");
        gen.writeEndArray();
        gen.writeEndObject();

        gen.close();

        String yaml = w.toString();

        // note: 1.12 uses more compact notation; 1.10 has prefix
        yaml = trimDocMarker(yaml).trim();

        BufferedReader br = new BufferedReader(new StringReader(yaml));
        assertEquals("ob:", br.readLine());

        // 27-Jan-2015, tatu: Not 100% if those items ought to (or not) be indented.
        //   SnakeYAML doesn't do that; yet some libs expect it. Strange.
        assertEquals("- \"a\"", br.readLine());
        assertEquals("- \"b\"", br.readLine());
        assertNull(br.readLine());
        br.close();
    }

    @SuppressWarnings("resource")
    @Test
    public void testStartMarker() throws Exception
    {
        YAMLFactory f = new YAMLFactory();

        // Ok, first, assume we do get the marker:
        StringWriter w = new StringWriter();
        assertTrue(f.isEnabled(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        YAMLGenerator gen = f.createGenerator(w);
        assertTrue(gen.isEnabled(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        _writeBradDoc(gen);
        String yaml = w.toString().trim();
        assertEquals("---\nname: \"Brad\"\nage: 39", yaml);

        // and then, disabling, and not any more
        f.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        assertFalse(f.isEnabled(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        w = new StringWriter();
        gen = f.createGenerator(w);
        assertFalse(gen.isEnabled(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        _writeBradDoc(gen);
        yaml = w.toString().trim();
        assertEquals("name: \"Brad\"\nage: 39", yaml);
    }

    @Test
    public void testLiteralBlockStyle() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE));

        YAMLMapper mapper = YAMLMapper.builder()
                .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true)
                .build();

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("text", "Hello\nWorld");
        String yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                     "text: |-\n  Hello\n  World", yaml);

        content.clear();
        content.put("text", "Hello World");
        yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                     "text: \"Hello World\"", yaml);
    }

    @Test
    public void testSimpleNullProperty() throws Exception
    {
        StringWriter w = new StringWriter();
        try (JsonGenerator gen = YAML_F.createGenerator(w)) {
            gen.writeStartObject();
            gen.writeFieldName("nullable");
            gen.writeNull();
            gen.writeEndObject();
        }
        // By default we'll get `null`, although tilde is a legal alternative
        assertEquals("---\n" +
                "nullable: null", w.toString().trim());
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _writeBradDoc(JsonGenerator gen) throws IOException
    {
        gen.writeStartObject();
        gen.writeStringField("name", "Brad");
        gen.writeNumberField("age", 39);
        gen.writeEndObject();
        gen.close();
    }
}
