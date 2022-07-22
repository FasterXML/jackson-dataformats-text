package com.fasterxml.jackson.dataformat.csv.ser;

import tools.jackson.databind.ObjectWriter;

import com.fasterxml.jackson.dataformat.csv.*;

// [dataformats-text#33]
public class TestWriterWithMissingValues extends ModuleTestBase
{
    private final CsvSchema SCHEMA = new CsvSchema.Builder()
        .addColumn("timestamp", CsvSchema.ColumnType.STRING)
        .addColumn("value", CsvSchema.ColumnType.NUMBER)
        .addColumn("id", CsvSchema.ColumnType.STRING)
        .build();
    private final CsvMapper MAPPER = mapperForCsv();
    final ObjectWriter WRITER = MAPPER.writer().with(SCHEMA);

    public void testWrite_NoNulls() {
        final String csv = WRITER.writeValueAsString(
                mapOf("timestamp", "2014-03-10T23:32:47+00:00",
                        "value", 42, "id", "hello"));

        assertEquals("\"2014-03-10T23:32:47+00:00\",42,hello\n", csv);
    }

    public void testWrite_NullFirstColumn() {
        final String csv = WRITER.writeValueAsString(
                mapOf("value", 42, "id", "hello"));
        assertEquals(",42,hello\n", csv);
    }

    public void testWrite_NullSecondColumn() {
        final String csv = WRITER.writeValueAsString(
                mapOf("timestamp", "2014-03-10T23:32:47+00:00",
                        "id", "hello"));

        assertEquals("\"2014-03-10T23:32:47+00:00\",,hello\n", csv);
    }

    public void testWrite_NullThirdColumn()
    {
        assertFalse(MAPPER.tokenStreamFactory().isEnabled(CsvGenerator.Feature.OMIT_MISSING_TAIL_COLUMNS));
        String csv = MAPPER.writer(SCHEMA).writeValueAsString(
                mapOf("timestamp", "2014-03-10T23:32:47+00:00",
                        "value", 42));

        assertEquals("\"2014-03-10T23:32:47+00:00\",42,\n", csv);
        ObjectWriter w = MAPPER.writer().with(CsvGenerator.Feature.OMIT_MISSING_TAIL_COLUMNS);
        csv = w.with(SCHEMA).writeValueAsString(
                mapOf("timestamp", "2014-03-10T23:32:47+00:00",
                        "value", 42));
        assertEquals("\"2014-03-10T23:32:47+00:00\",42\n", csv);
    }
}
