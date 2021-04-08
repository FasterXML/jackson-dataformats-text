package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringWriter;
import java.time.*;

public class TomlGeneratorTest {
    @Test
    public void number() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeNumber(123);
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = 123\n", w.toString());
    }

    @Test
    public void bool() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeBoolean(true);
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = true\n", w.toString());
    }

    @Test
    public void floats() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeNumber(1.23);
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = 1.23\n", w.toString());
    }

    @Test
    public void stringNormal() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString("foo");
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = 'foo'\n", w.toString());
    }

    @Test
    public void stringApostrophe() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString("foo'");
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = \"foo'\"\n", w.toString());
    }

    @Test
    public void stringQuote() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString("foo\"");
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = 'foo\"'\n", w.toString());
    }

    @Test
    public void stringQuoteApostrophe() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString("foo\"'");
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = \"foo\\\"'\"\n", w.toString());
    }

    @Test
    public void stringControlCharUnicode() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString("foo\u0001");
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = \"foo\\u0001\"\n", w.toString());
    }

    @Test
    public void stringControlCharSpecial() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeString("foo\b");
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = \"foo\\b\"\n", w.toString());
    }

    @Test
    public void binary() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeBinary(new byte[] {1,2,3});
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = 'AQID'\n", w.toString());
    }

    @Test
    public void emptyObject() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeStartObject();
            generator.writeEndObject();
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = {}\n", w.toString());
    }

    @Test
    public void objectWithValues() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
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
        Assert.assertEquals("abc.foo = 1\nabc.bar = 2\n", w.toString());
    }

    @Test
    public void emptyArray() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeStartArray();
            generator.writeEndArray();
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = []\n", w.toString());
    }

    @Test
    public void arrayWithScalars() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeStartArray();
            generator.writeNumber(1);
            generator.writeNumber(2);
            generator.writeNumber(3);
            generator.writeEndArray();
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = [1, 2, 3]\n", w.toString());
    }

    @Test
    public void arrayMixed() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
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
        Assert.assertEquals("abc = [1, {foo = 1, bar = 2}]\n", w.toString());
    }

    @Test
    public void temporal() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("abc");
            generator.writeStartArray();
            generator.writeObject(LocalDate.of(2021, 3, 27));
            generator.writeObject(LocalTime.of(18, 40, 15, 123456789));
            generator.writeObject(LocalDateTime.of(2021, 3, 27, 18, 40, 15, 123456789));
            generator.writeObject(OffsetDateTime.of(2021, 3, 27, 18, 40, 15, 123456789, ZoneOffset.ofHoursMinutes(1, 23)));
            generator.writeEndArray();
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = [2021-03-27, 18:40:15.123456789, 2021-03-27T18:40:15.123456789, 2021-03-27T18:40:15.123456789+01:23]\n", w.toString());
    }

    @Test
    public void complexKey() throws IOException {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = new TomlMapper().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeFieldName("foo bar");
            generator.writeNumber(123);
            generator.writeEndObject();
        }
        Assert.assertEquals("'foo bar' = 123\n", w.toString());
    }
}