package com.fasterxml.jackson.dataformat.csv.filter;

import java.io.*;
import java.util.Arrays;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.*;
import com.fasterxml.jackson.dataformat.csv.testutil.PrefixInputDecorator;
import com.fasterxml.jackson.dataformat.csv.testutil.PrefixOutputDecorator;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StreamingDecoratorsTest extends ModuleTestBase
{
    @Test
    public void testInputDecorators() throws IOException
    {
        final byte[] DOC = utf8("foo,");
        final CsvMapper mapper = mapperBuilder(
                streamFactoryBuilder().inputDecorator(new PrefixInputDecorator(DOC))
                        .enable(CsvParser.Feature.WRAP_AS_ARRAY)
                .build())
                .build();
        try (MappingIterator<String[]> r = mapper.readerFor(String[].class)
                .readValues(utf8("bar"))) {
            assertTrue(r.hasNext());
            String[] row = r.nextValue();
            assertEquals(2, row.length);
            assertEquals("foo", row[0]);
            assertEquals("bar", row[1]);
            assertFalse(r.hasNext());
        }

        // and then via Reader as well
        try (MappingIterator<String[]> r = mapper.readerFor(String[].class)
                .readValues("zipfoo")) {
            assertTrue(r.hasNext());
            String[] row = r.nextValue();
            assertEquals(2, row.length);
            assertEquals("foo", row[0]);
            assertEquals("zipfoo", row[1]);
            assertFalse(r.hasNext());
        }
    }

    @Test
    public void testOutputDecorators() throws IOException
    {
        final byte[] DOC = utf8("a,b\n");
        final CsvMapper mapper = mapperBuilder(
                streamFactoryBuilder().outputDecorator(new PrefixOutputDecorator(DOC))
                        .enable(CsvParser.Feature.WRAP_AS_ARRAY)
                .build())
                .build();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (SequenceWriter w = mapper.writer().writeValues(bytes)) {
            w.write(Arrays.asList("first", "second"));
        }
        assertEquals("a,b\nfirst,second", bytes.toString("UTF-8").trim());

        // and same with char-backed too
        StringWriter sw = new StringWriter();
        try (SequenceWriter w = mapper.writer().writeValues(sw)) {
            w.write(Arrays.asList("x", "yz"));
        }
        assertEquals("a,b\nx,yz", sw.toString().trim());
    }
}
