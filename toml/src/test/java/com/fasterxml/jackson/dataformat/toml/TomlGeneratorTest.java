package com.fasterxml.jackson.dataformat.toml;

import java.io.*;
import java.time.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TomlGeneratorTest extends TomlMapperTestBase {
    @Test
    public void number() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeNumber(123);
            generator.writeEndObject();
        }
        assertEquals("abc = 123\n", w.toString());
    }

    @Test
    public void bool() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeBoolean(true);
            generator.writeEndObject();
        }
        assertEquals("abc = true\n", w.toString());
    }

    @Test
    public void floats() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeNumber(1.23);
            generator.writeEndObject();
        }
        assertEquals("abc = 1.23\n", w.toString());
    }

    @Test
    public void stringNormal() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString("foo");
            generator.writeEndObject();
        }
        assertEquals("abc = 'foo'\n", w.toString());
    }

    @Test
    public void stringApostrophe() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString("foo'");
            generator.writeEndObject();
        }
        assertEquals("abc = \"foo'\"\n", w.toString());
    }

    @Test
    public void stringQuote() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString("foo\"");
            generator.writeEndObject();
        }
        assertEquals("abc = 'foo\"'\n", w.toString());
    }

    @Test
    public void stringQuoteApostrophe() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString("foo\"'");
            generator.writeEndObject();
        }
        assertEquals("abc = \"foo\\\"'\"\n", w.toString());
    }

    @Test
    public void stringControlCharUnicode() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString("foo\u0001");
            generator.writeEndObject();
        }
        assertEquals("abc = \"foo\\u0001\"\n", w.toString());
    }

    @Test
    public void stringControlCharSpecial() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString("foo\b");
            generator.writeEndObject();
        }
        assertEquals("abc = \"foo\\b\"\n", w.toString());
    }

    @Test
    public void binary() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeBinary(new byte[] {1,2,3});
            generator.writeEndObject();
        }
        assertEquals("abc = 'AQID'\n", w.toString());
    }

    @Test
    public void emptyObject() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeStartObject();
            generator.writeEndObject();
            generator.writeEndObject();
        }
        assertEquals("abc = {}\n", w.toString());
    }

    @Test
    public void objectWithValues() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeStartObject();
            generator.writeFieldName("foo");
            generator.writeNumber(1);
            generator.writeFieldName("bar");
            generator.writeNumber(2);
            generator.writeEndObject();
            generator.writeEndObject();
        }
        assertEquals("abc.foo = 1\nabc.bar = 2\n", w.toString());
    }

    @Test
    public void emptyArray() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeStartArray();
            generator.writeEndArray();
            generator.writeEndObject();
        }
        assertEquals("abc = []\n", w.toString());
    }

    @Test
    public void arrayWithScalars() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
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
    public void arrayMixed() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeStartArray();
            generator.writeNumber(1);
            generator.writeStartObject();
            generator.writeFieldName("foo");
            generator.writeNumber(1);
            generator.writeFieldName("bar");
            generator.writeNumber(2);
            generator.writeEndObject();
            generator.writeEndArray();
            generator.writeEndObject();
        }
        assertEquals("abc = [1, {foo = 1, bar = 2}]\n", w.toString());
    }

    @Test
    public void temporal() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
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
    public void complexKey() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("foo bar");
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
        g.writeFieldName("point");
        g.writeStartObject();
        g.writeFieldName("x");
        g.writeNumber(19);
        g.writeFieldName("y");
        g.writeNumber(72);
        g.writeEndObject();
        g.writeEndObject();
    }
    
    @Test
    public void nullEnabledDefault() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("foo");
            generator.writeNull();
            generator.writeEndObject();
        }
        assertEquals("foo = ''\n", w.toString());
    }

    @Test
    public void nullDisable() throws IOException {
        assertThrows(TomlStreamWriteException.class, () -> {
            StringWriter w = new StringWriter();
            try (JsonGenerator generator = newTomlMapper().enable(TomlWriteFeature.FAIL_ON_NULL_WRITE).createGenerator(w)) {
                generator.writeStartObject();
                generator.writeFieldName("foo");
                generator.writeNull();
                generator.writeEndObject();
            }
        });
    }
}
