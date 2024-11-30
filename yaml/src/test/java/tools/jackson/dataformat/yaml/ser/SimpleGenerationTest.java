package tools.jackson.dataformat.yaml.ser;

import java.io.*;
import java.util.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.dataformat.yaml.*;

public class SimpleGenerationTest extends ModuleTestBase
{
    private final YAMLMapper MAPPER = newObjectMapper();

    public void testStreamingArray() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = MAPPER.createGenerator(w);
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

    public void testStreamingObject() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = MAPPER.createGenerator(w);
        _writeBradDoc(gen);
        String yaml = w.toString();

        // note: 1.12 uses more compact notation; 1.10 has prefix
        yaml = trimDocMarker(yaml).trim();
        assertEquals("name: \"Brad\"\nage: 39", yaml);
        gen.close();
    }

    public void testStreamingNested() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = MAPPER.createGenerator(w);

        gen.writeStartObject();
        gen.writeName("ob");
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
    public void testStartMarker() throws Exception
    {
        // Ok, first, assume we do get the marker:
        StringWriter w = new StringWriter();
        ObjectWriter ow = MAPPER.writer();

        assertTrue(MAPPER.tokenStreamFactory().isEnabled(YAMLWriteFeature.WRITE_DOC_START_MARKER));
        YAMLGenerator gen = (YAMLGenerator) MAPPER.createGenerator(w);
        assertTrue(gen.isEnabled(YAMLWriteFeature.WRITE_DOC_START_MARKER));
        _writeBradDoc(gen);
        String yaml = w.toString().trim();
        assertEquals("---\nname: \"Brad\"\nage: 39", yaml);

        // and then, disabling, and not any more
        ow = ow.without(YAMLWriteFeature.WRITE_DOC_START_MARKER);
        w = new StringWriter();
        gen = (YAMLGenerator)ow.createGenerator(w);
        assertFalse(gen.isEnabled(YAMLWriteFeature.WRITE_DOC_START_MARKER));
        _writeBradDoc(gen);
        yaml = w.toString().trim();
        assertEquals("name: \"Brad\"\nage: 39", yaml);
    }

    public void testLiteralBlockStyle() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLWriteFeature.LITERAL_BLOCK_STYLE));

        f = f.rebuild().enable(YAMLWriteFeature.LITERAL_BLOCK_STYLE)
                .build();
        assertTrue(f.isEnabled(YAMLWriteFeature.LITERAL_BLOCK_STYLE));

        YAMLMapper mapper = new YAMLMapper(f);
        assertTrue(mapper.tokenStreamFactory().isEnabled(YAMLWriteFeature.LITERAL_BLOCK_STYLE));

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

    public void testSimpleNullProperty() throws Exception
    {
        StringWriter w = new StringWriter();
        try (JsonGenerator gen = MAPPER.createGenerator(w)) {
            gen.writeStartObject();
            gen.writeName("nullable");
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

    private void _writeBradDoc(JsonGenerator g) throws IOException
    {
        g.writeStartObject();
        g.writeStringProperty("name", "Brad");
        g.writeNumberProperty("age", 39);
        g.writeEndObject();
        g.close();
    }
}
