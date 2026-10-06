package com.fasterxml.jackson.dataformat.toml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // `writeString(char[],int,int)` used to read `text[offset + len]` instead
    // of `text[offset + i]` when escaping was needed (wrong chars written, or
    // ArrayIndexOutOfBoundsException)
    @Test
    public void stringFromCharArrayNeedingEscapes() throws IOException {
        // value has a quote and a newline: needs "basic string" with escapes
        String value = "say \"hi\"\nbye";
        char[] padded = ("<<" + value).toCharArray(); // no trailing padding: index bug would overflow
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString(padded, 2, value.length());
            generator.writeEndObject();
        }
        assertEquals("abc = \"say \\\"hi\\\"\\nbye\"\n", w.toString());
        // and must match String variant, and read back
        StringWriter w2 = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w2)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString(value);
            generator.writeEndObject();
        }
        assertEquals(w2.toString(), w.toString());
        assertEquals(value, newTomlMapper().readTree(w.toString()).get("abc").textValue());
    }

    // [#726]: long value (exceeds output buffer) with escapes, sliced from the middle
    // of a larger array, so both index-bound directions are exercised
    @Test
    public void longStringFromCharArrayNeedingEscapes() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000; ++i) {
            sb.append("line \"").append(i).append("\"\n");
        }
        final String value = sb.toString();
        char[] padded = ("<<<" + value + ">>>").toCharArray();
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString(padded, 3, value.length());
            generator.writeEndObject();
        }
        StringWriter w2 = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w2)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString(value);
            generator.writeEndObject();
        }
        assertEquals(w2.toString(), w.toString());
        assertEquals(value, newTomlMapper().readTree(w.toString()).get("abc").textValue());
    }

    @Test
    public void nullStringFromCharArray() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString((char[]) null, 0, 0);
            generator.writeFieldName("def");
            generator.writeNull();
            generator.writeEndObject();
        }
        assertEquals("abc = \'\'\ndef = \'\'\n", w.toString());
    }

    // Invalid String values must fail before anything (key, separator) is written,
    // leaving generator usable
    @Test
    public void unsupportedCharsInStringValue() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            TomlStreamWriteException e = assertThrows(TomlStreamWriteException.class,
                    () -> generator.writeString(new char[] { '\uD800', 'x' }, 0, 2));
            assertTrue(e.getMessage().startsWith("String value contains unsupported characters"),
                    e.getMessage());
            e = assertThrows(TomlStreamWriteException.class,
                    () -> generator.writeString("\uD800x"));
            assertTrue(e.getMessage().startsWith("String value contains unsupported characters"),
                    e.getMessage());
            generator.writeString("ok");
            generator.writeEndObject();
        }
        assertEquals("abc = 'ok'\n", w.toString());
    }

    @Test
    public void unsupportedCharsInInlineKey() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeStartArray();
            generator.writeStartObject();
            TomlStreamWriteException e = assertThrows(TomlStreamWriteException.class,
                    () -> generator.writeFieldName("\uD800x"));
            assertTrue(e.getMessage().startsWith("Key contains unsupported characters"),
                    e.getMessage());
        }
    }

    @Test
    public void invalidCharArrayRange() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            final char[] chars = "ab\"c".toCharArray();
            StreamWriteException e = assertThrows(StreamWriteException.class,
                    () -> generator.writeString(chars, 2, 10));
            assertTrue(e.getMessage().contains("Invalid 'offset'"), e.getMessage());
            assertThrows(StreamWriteException.class,
                    () -> generator.writeString(chars, 0, -1));
            // nothing written, generator still usable
            generator.writeString(chars, 2, 2);
            generator.writeEndObject();
        }
        assertEquals("abc = '\"c'\n", w.toString());
    }

    @Test
    public void invalidBinaryRange() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            final byte[] data = new byte[] { 1, 2 };
            // used to silently pad with zero bytes
            assertThrows(StreamWriteException.class,
                    () -> generator.writeBinary(data, 1, 5));
            assertThrows(StreamWriteException.class,
                    () -> generator.writeBinary(data, -1, 1));
            generator.writeBinary(data, 1, 1);
            generator.writeEndObject();
        }
        assertEquals("abc = 'Ag=='\n", w.toString());
    }

    // `writeRawValue()` variants did not call `writeValueEnd()`: no line feed after value
    @Test
    public void rawValue() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("a");
            generator.writeRawValue("1");
            generator.writeFieldName("b");
            generator.writeRawValue("<<2>>", 2, 1);
            generator.writeFieldName("c");
            generator.writeRawValue("<<3>>".toCharArray(), 2, 1);
            generator.writeFieldName("d");
            generator.writeRawValue(new SerializedString("4"));
            generator.writeFieldName("e");
            generator.writeStartArray();
            generator.writeRawValue("5");
            generator.writeRawValue("6");
            generator.writeEndArray();
            generator.writeEndObject();
        }
        assertEquals("a = 1\nb = 2\nc = 3\nd = 4\ne = [5, 6]\n", w.toString());
        assertThrows(StreamWriteException.class,
                () -> newTomlMapper().createGenerator(new StringWriter()).writeRawValue("abc", 2, 5));
    }

    @Test
    public void invalidRawRange() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            assertThrows(StreamWriteException.class,
                    () -> generator.writeRaw("abc", 2, 5));
            assertThrows(StreamWriteException.class,
                    () -> generator.writeRaw("abc".toCharArray(), 2, 5));
            assertThrows(StreamWriteException.class,
                    () -> generator.writeRaw("abc".toCharArray(), -1, 1));
            generator.writeRaw("abc", 1, 2);
            generator.writeRaw("xyz".toCharArray(), 0, 1);
        }
        assertEquals("bcx", w.toString());
    }

    // `writeUTF8String()` used to call `writeValueEnd()` twice, producing
    // an extra empty line after the value
    @Test
    public void utf8String() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            final byte[] utf8 = "<<x\"y>>".getBytes(StandardCharsets.UTF_8);
            generator.writeUTF8String(utf8, 2, 3);
            generator.writeFieldName("def");
            generator.writeNumber(1);
            generator.writeEndObject();
        }
        assertEquals("abc = 'x\"y'\ndef = 1\n", w.toString());
    }

    // `writeNumber(short)` used to call `writeValueEnd()` twice, producing
    // an extra empty line after every short value
    @Test
    public void shortNumber() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeNumber((short) 7);
            generator.writeFieldName("def");
            generator.writeNumber((short) -8);
            generator.writeEndObject();
        }
        assertEquals("abc = 7\ndef = -8\n", w.toString());
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

    // [dataformats-text#696]: non-finite floats must be written as the TOML
    // tokens `nan` / `inf` / `-inf` (not Java's `NaN` / `Infinity`) so the
    // writer's own output can be parsed back.
    @Test
    public void nonFiniteDoubles() throws IOException {
        assertEquals("abc = nan\n", _writeDouble(Double.NaN));
        assertEquals("abc = inf\n", _writeDouble(Double.POSITIVE_INFINITY));
        assertEquals("abc = -inf\n", _writeDouble(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void nonFiniteFloats() throws IOException {
        assertEquals("abc = nan\n", _writeFloat(Float.NaN));
        assertEquals("abc = inf\n", _writeFloat(Float.POSITIVE_INFINITY));
        assertEquals("abc = -inf\n", _writeFloat(Float.NEGATIVE_INFINITY));
    }

    // Written non-finite values must round-trip through the same mapper.
    @Test
    public void nonFiniteRoundTrip() throws IOException {
        TomlMapper mapper = newTomlMapper();
        for (double d : new double[] {
                Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            String toml = mapper.writeValueAsString(java.util.Collections.singletonMap("x", d));
            JsonNode node = mapper.readTree(toml).get("x");
            if (Double.isNaN(d)) {
                assertTrue(Double.isNaN(node.doubleValue()), "expected NaN from: " + toml);
            } else {
                assertEquals(d, node.doubleValue(), 0.0, "round-trip failed for: " + toml);
            }
        }
    }

    private String _writeDouble(double d) throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeNumber(d);
            generator.writeEndObject();
        }
        return w.toString();
    }

    private String _writeFloat(float f) throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = newTomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeNumber(f);
            generator.writeEndObject();
        }
        return w.toString();
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
