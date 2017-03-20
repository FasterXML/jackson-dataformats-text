package com.fasterxml.jackson.dataformat.csv.failing;

import java.io.StringWriter;

import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.dataformat.csv.*;

public class NullWriting116Test extends ModuleTestBase
{
    private final CsvMapper csv = new CsvMapper();

    // [dataformat#116]
    public void testWithObjectArray() throws Exception 
    {
        CsvSchema schema = CsvSchema.builder()
                                    .addColumn("a", CsvSchema.ColumnType.NUMBER)
                                    .addColumn("b", CsvSchema.ColumnType.NUMBER)
                                    .setUseHeader(true)
                                    .build();
        ObjectWriter writer = csv.writer(schema);
        StringWriter out = new StringWriter();
        SequenceWriter sequence = writer.writeValues(out);

        sequence.write(new Object[]{ 1, 2 });
//        sequence.write(new Object[]{ null, 2 });
        sequence.write(new Object[]{ null, null });
        sequence.write(new Object[]{ 1, null });

        sequence.close();

//System.err.println("CSV:\n"+out);
        assertEquals("a,b\n" +
                     "1,2\n" +
//                     ",2\n" +
                     ",\n" +
                     "1,\n", out.toString());
    }

}
