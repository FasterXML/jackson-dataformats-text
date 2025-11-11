package com.fasterxml.jackson.dataformat.csv.ser;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectArrayNullWrite116Test extends ModuleTestBase
{
    // for [dataformats-text#116]
    @Test
    public void testWithObjectArray() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                                    .addColumn("a", CsvSchema.ColumnType.NUMBER)
                                    .addColumn("b", CsvSchema.ColumnType.NUMBER)
                                    .setUseHeader(true)
                                    .build();
        ObjectWriter writer = mapperForCsv().writer(schema);
        StringWriter out = new StringWriter();

        try (SequenceWriter sequence = writer.writeValues(out)) {
            sequence.write(new Object[]{ 1, 2 });
            sequence.write(new Object[]{ null, 2 });
            sequence.write(new Object[]{ null, null });
            sequence.write(new Object[]{ 1, null });
        }

        assertEquals("a,b\n" +
                     "1,2\n" +
                     ",2\n" +
                     ",\n" +
                     "1,\n", out.toString());
    }
}
