package tools.jackson.dataformat.csv.failing;

import java.io.StringWriter;

import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.ModuleTestBase;

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
