package tools.jackson.dataformat.toml;

import java.io.*;
import java.time.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;

public class TomlGeneratorTest extends TomlMapperTestBase {
    @Test
    public void number() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeNumber(123);
            generator.writeEndObject();
        }
        assertEquals("abc = 123\n", w.toString());
    }

    @Test
    public void bool() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeBoolean(true);
            generator.writeEndObject();
        }
        assertEquals("abc = true\n", w.toString());
    }

    @Test
    public void floats() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeNumber(1.23);
            generator.writeEndObject();
        }
        assertEquals("abc = 1.23\n", w.toString());
    }

    @Test
    public void stringNormal() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeString("foo");
            generator.writeEndObject();
        }
        assertEquals("abc = 'foo'\n", w.toString());
    }

    @Test
    public void stringApostrophe() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeString("foo'");
            generator.writeEndObject();
        }
        assertEquals("abc = \"foo'\"\n", w.toString());
    }

    @Test
    public void stringQuote() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeString("foo\"");
            generator.writeEndObject();
        }
        assertEquals("abc = 'foo\"'\n", w.toString());
    }

    @Test
    public void stringQuoteApostrophe() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeString("foo\"'");
            generator.writeEndObject();
        }
        assertEquals("abc = \"foo\\\"'\"\n", w.toString());
    }

    @Test
    public void stringControlCharUnicode() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeString("foo\u0001");
            generator.writeEndObject();
        }
        assertEquals("abc = \"foo\\u0001\"\n", w.toString());
    }

    @Test
    public void stringControlCharSpecial() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeString("foo\b");
            generator.writeEndObject();
        }
        assertEquals("abc = \"foo\\b\"\n", w.toString());
    }

    @Test
    public void binary() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeBinary(new byte[] {1,2,3});
            generator.writeEndObject();
        }
        assertEquals("abc = 'AQID'\n", w.toString());
    }

    @Test
    public void emptyObject() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeStartObject();
            generator.writeEndObject();
            generator.writeEndObject();
        }
        assertEquals("abc = {}\n", w.toString());
    }

    @Test
    public void objectWithValues() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeStartObject();
            generator.writeName("foo");
            generator.writeNumber(1);
            generator.writeName("bar");
            generator.writeNumber(2);
            generator.writeEndObject();
            generator.writeEndObject();
        }
        assertEquals("abc.foo = 1\nabc.bar = 2\n", w.toString());
    }

    @Test
    public void emptyArray() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeStartArray();
            generator.writeEndArray();
            generator.writeEndObject();
        }
        assertEquals("abc = []\n", w.toString());
    }

    @Test
    public void arrayWithScalars() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeStartArray();
            generator.writeNumber(1);
            generator.writeNumber(2);
            generator.writeNumber(3);
            generator.writeEndArray();
            generator.writeEndObject();
        }
        assertEquals("abc = [1, 2, 3]\n", w.toString());
    }

    @Test
    public void arrayMixed() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeStartArray();
            generator.writeNumber(1);
            generator.writeStartObject();
            generator.writeName("foo");
            generator.writeNumber(1);
            generator.writeName("bar");
            generator.writeNumber(2);
            generator.writeEndObject();
            generator.writeEndArray();
            generator.writeEndObject();
        }
        assertEquals("abc = [1, {foo = 1, bar = 2}]\n", w.toString());
    }

    @Test
    public void temporal() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeStartArray();
            generator.writePOJO(LocalDate.of(2021, 3, 27));
            generator.writePOJO(LocalTime.of(18, 40, 15, 123456789));
            generator.writePOJO(LocalDateTime.of(2021, 3, 27, 18, 40, 15, 123456789));
            generator.writePOJO(OffsetDateTime.of(2021, 3, 27, 18, 40, 15, 123456789, ZoneOffset.ofHoursMinutes(1, 23)));
            generator.writeEndArray();
            generator.writeEndObject();
        }
        assertEquals("abc = [2021-03-27, 18:40:15.123456789, 2021-03-27T18:40:15.123456789, 2021-03-27T18:40:15.123456789+01:23]\n", w.toString());
    }

    @Test
    public void complexKey() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("foo bar");
            generator.writeNumber(123);
            generator.writeEndObject();
        }
        assertEquals("'foo bar' = 123\n", w.toString());
    }

    // [dataformats-text#258]: byte-backed output not flushing?
    @Test
    public void nestedObjectValues() throws IOException {
        final ObjectMapper mapper = newTomlMapper();
        final String EXP_TOML = "point.x = 19\n"
                +"point.y = 72\n";

        // Try both String-/char- and byte-backed variants

        StringWriter sw = new StringWriter();
        try (JsonGenerator tomlG = mapper.createGenerator(sw)) {
            _writeNested(tomlG);
        }
        assertEquals(EXP_TOML, sw.toString());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator tomlG = mapper.createGenerator(bytes)) {
            _writeNested(tomlG);
        }
        assertEquals(EXP_TOML, bytes.toString("UTF-8"));
    }

    private void _writeNested(JsonGenerator g) throws IOException {
        g.writeStartObject();
        g.writeName("point");
        g.writeStartObject();
        g.writeName("x");
        g.writeNumber(19);
        g.writeName("y");
        g.writeNumber(72);
        g.writeEndObject();
        g.writeEndObject();
    }
    
    @Test
    public void nullEnabledDefault() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("foo");
            generator.writeNull();
            generator.writeEndObject();
        }
        assertEquals("foo = ''\n", w.toString());
    }

    @Test
    public void nullDisable() throws IOException {
        assertThrows(TomlStreamWriteException.class, () -> {
            StringWriter w = new StringWriter();
            try (JsonGenerator generator = TomlMapper.builder().enable(TomlWriteFeature.FAIL_ON_NULL_WRITE).build().createGenerator(w)) {
                generator.writeStartObject();
                generator.writeName("foo");
                generator.writeNull();
                generator.writeEndObject();
            }
        });
    }
}
