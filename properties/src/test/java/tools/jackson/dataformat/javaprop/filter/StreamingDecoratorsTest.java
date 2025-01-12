package tools.jackson.dataformat.javaprop.filter;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.dataformat.javaprop.*;
import tools.jackson.dataformat.javaprop.testutil.PrefixInputDecorator;
import tools.jackson.dataformat.javaprop.testutil.PrefixOutputDecorator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StreamingDecoratorsTest extends ModuleTestBase
{
    @SuppressWarnings("unchecked")
    @Test
    public void testInputDecorators() throws IOException
    {
        final byte[] DOC = utf8("secret=mum\n");
        final ObjectMapper mapper = propertiesMapperBuilder(
                propertiesFactoryBuilder().inputDecorator(new PrefixInputDecorator(DOC))
                .build())
                .build();
        Map<String,Object> value = mapper.readValue(utf8("value=foo\n"), Map.class);
        assertEquals(2, value.size());
        assertEquals("foo", value.get("value"));
        assertEquals("mum", value.get("secret"));

        // and then via Reader as well
        value = mapper.readValue(new StringReader("value=xyz\n"), Map.class);
        assertEquals(2, value.size());
        assertEquals("xyz", value.get("value"));
        assertEquals("mum", value.get("secret"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOutputDecorators() throws IOException
    {
        final byte[] DOC = utf8("prefix=p\n");
        final ObjectMapper mapper = propertiesMapperBuilder(
                propertiesFactoryBuilder().outputDecorator(new PrefixOutputDecorator(DOC))
                .build())
                .build();
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put("key", "value");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        mapper.writeValue(bytes, input);
        Map<String, Object> output = mapper.readValue(bytes.toByteArray(), Map.class);
        assertEquals(2, output.size());
        assertEquals("value", output.get("key"));
        assertEquals("p", output.get("prefix"));

        // and same with char-backed too
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, input);
        output = mapper.readValue(bytes.toByteArray(), Map.class);
        assertEquals(2, output.size());
        assertEquals("value", output.get("key"));
        assertEquals("p", output.get("prefix"));
    }
}
