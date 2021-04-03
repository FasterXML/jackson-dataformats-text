package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.ObjectWriteContext;
import com.fasterxml.jackson.core.io.IOContext;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringWriter;
import java.time.*;

public class TomlGeneratorTest {
    @Test
    public void number() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeNumber(123);
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = 123\n", w.toString());
    }

    @Test
    public void bool() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeBoolean(true);
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = true\n", w.toString());
    }

    @Test
    public void floats() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeNumber(1.23);
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = 1.23\n", w.toString());
    }

    @Test
    public void stringNormal() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeString("foo");
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = 'foo'\n", w.toString());
    }

    @Test
    public void stringApostrophe() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeString("foo'");
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = \"foo'\"\n", w.toString());
    }

    @Test
    public void stringQuote() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeString("foo\"");
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = 'foo\"'\n", w.toString());
    }

    @Test
    public void stringQuoteApostrophe() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeString("foo\"'");
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = \"foo\\\"'\"\n", w.toString());
    }

    @Test
    public void stringControlCharUnicode() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeString("foo\u0001");
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = \"foo\\u0001\"\n", w.toString());
    }

    @Test
    public void stringControlCharSpecial() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeString("foo\b");
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = \"foo\\b\"\n", w.toString());
    }

    @Test
    public void binary() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeBinary(new byte[] {1,2,3});
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = 'AQID'\n", w.toString());
    }

    @Test
    public void emptyObject() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeStartObject();
            generator.writeEndObject();
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = {}\n", w.toString());
    }

    @Test
    public void objectWithValues() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
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
        Assert.assertEquals("abc.foo = 1\nabc.bar = 2\n", w.toString());
    }

    @Test
    public void emptyArray() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
            generator.writeStartArray();
            generator.writeEndArray();
            generator.writeEndObject();
        }
        Assert.assertEquals("abc = []\n", w.toString());
    }

    @Test
    public void arrayWithScalars() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("abc");
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
    public void arrayMixed() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
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
        Assert.assertEquals("abc = [1, {foo = 1, bar = 2}]\n", w.toString());
    }

    @Test
    public void temporal() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
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
        Assert.assertEquals("abc = [2021-03-27, 18:40:15.123456789, 2021-03-27T18:40:15.123456789, 2021-03-27T18:40:15.123456789+01:23]\n", w.toString());
    }

    @Test
    public void complexKey() {
        StringWriter w = new StringWriter();
        try (JsonGenerator generator = TomlMapper.shared().createGenerator(w)) {
            generator.writeStartObject();
            generator.writeName("foo bar");
            generator.writeNumber(123);
            generator.writeEndObject();
        }
        Assert.assertEquals("'foo bar' = 123\n", w.toString());
    }
}