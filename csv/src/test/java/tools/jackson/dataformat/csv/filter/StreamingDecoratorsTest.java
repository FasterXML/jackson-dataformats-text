package tools.jackson.dataformat.csv.filter;

import java.io.*;
import java.util.Arrays;

import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.SequenceWriter;

import tools.jackson.dataformat.csv.*;
import tools.jackson.dataformat.csv.testutil.PrefixInputDecorator;
import tools.jackson.dataformat.csv.testutil.PrefixOutputDecorator;

public class StreamingDecoratorsTest extends ModuleTestBase
{
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
