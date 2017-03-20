package com.fasterxml.jackson.dataformat.csv.ser;

import java.io.*;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

public class TestGeneratorNoSchema extends ModuleTestBase
{
    private final CsvSchema SCHEMA = CsvSchema.emptySchema()
            .withoutHeader()
            .withEscapeChar('\\')
            .withQuoteChar('"')
            .withColumnSeparator(';')
            .withLineSeparator("\r\n")
            ;

    private final CsvMapper MAPPER = mapperForCsv();

    public void testUntypedAsSequenceStreaming() throws Exception
    {
        StringWriter sw = new StringWriter();
        CsvGenerator gen = MAPPER.getFactory().createGenerator(sw);
        gen.setSchema(SCHEMA);

        assertEquals(0, gen.getOutputBuffered());
        
        gen.writeStartArray();
        gen.writeString("foo");

        // this will be buffered because we output in correct order, so:
        assertEquals(3, gen.getOutputBuffered());
        
        gen.writeNumber(1234567890L);
        gen.writeBoolean(true);
        gen.writeEndArray();

        gen.writeStartArray();
        gen.writeString("bar");
        gen.writeNumber(-1250000000000L);
        gen.writeBoolean(false);
        gen.writeEndArray();

        gen.close();
        assertEquals(0, gen.getOutputBuffered());
        
        String csv = sw.toString();

        assertEquals("foo;1234567890;true\r\n"
                +"bar;-1250000000000;false\r\n",
                csv);
    }

    public void testUntypedAsSequenceDatabind() throws Exception
    {
        ObjectWriter writer = MAPPER.writer(SCHEMA);

        String csv = writer.writeValueAsString(new Object[] {
                new Object[] { "foo", 13, true },
                new Object[] { "bar", 28, false }
        });
        assertEquals("foo;13;true\r\n"
                +"bar;28;false\r\n",
                csv);
    }
}
