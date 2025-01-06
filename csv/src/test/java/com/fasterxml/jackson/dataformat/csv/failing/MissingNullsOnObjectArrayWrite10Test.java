package com.fasterxml.jackson.dataformat.csv.failing;

import java.io.StringWriter;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

public class MissingNullsOnObjectArrayWrite10Test extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    // for [dataformats-text#10]
    public void testNullsOnObjectArrayWrites2Col() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("a", CsvSchema.ColumnType.NUMBER)
                .addColumn("b", CsvSchema.ColumnType.NUMBER)
                .setUseHeader(true)
                .build();
        ObjectWriter writer = MAPPER.writer(schema);
        StringWriter out = new StringWriter();
        SequenceWriter sequence = writer.writeValues(out);

        sequence.write(new Object[]{ null, 2 });
        sequence.write(new Object[]{ null, null });
        sequence.write(new Object[]{ 1, null });

        final String csv = out.toString().trim();

        assertEquals("\"a\",\"b\"\n" +
             ",2\n" +
             ",\n" +
             "1,",
             csv);
    }
}
