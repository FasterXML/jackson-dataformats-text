package com.fasterxml.jackson.dataformat.csv.ser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;

import com.fasterxml.jackson.dataformat.csv.*;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.lang.String;

// [Issue#33]
public class TestWriterWithMissingValues extends ModuleTestBase
{
    private final CsvSchema SCHEMA = new CsvSchema.Builder()
        .addColumn("timestamp", CsvSchema.ColumnType.STRING)
        .addColumn("value", CsvSchema.ColumnType.NUMBER)
        .addColumn("id", CsvSchema.ColumnType.STRING)
        .build();
    final ObjectWriter WRITER = new CsvMapper().writer().with(SCHEMA);
    
    @Test
    public void testWrite_NoNulls() throws JsonProcessingException {
        final String csv = WRITER.writeValueAsString(
                ImmutableMap.of("timestamp", "2014-03-10T23:32:47+00:00",
                        "value", 42, "id", "hello"));

        assertEquals("\"2014-03-10T23:32:47+00:00\",42,hello\n", csv);
    }

    @Test
    public void testWrite_NullFirstColumn() throws JsonProcessingException {
        final String csv = WRITER.writeValueAsString(
                ImmutableMap.of("value", 42, "id", "hello"));
        assertEquals(",42,hello\n", csv);
    }

    @Test
    public void testWrite_NullSecondColumn() throws JsonProcessingException {
        final String csv = WRITER.writeValueAsString(
                ImmutableMap.of("timestamp", "2014-03-10T23:32:47+00:00",
                        "id", "hello"));

        assertEquals("\"2014-03-10T23:32:47+00:00\",,hello\n", csv);
    }

    @Test
    public void testWrite_NullThirdColumn() throws JsonProcessingException
    {
        CsvMapper mapper = new CsvMapper();
        assertFalse(mapper.getFactory().isEnabled(CsvGenerator.Feature.OMIT_MISSING_TAIL_COLUMNS));
        String csv = mapper.writer(SCHEMA).writeValueAsString(
                ImmutableMap.of("timestamp", "2014-03-10T23:32:47+00:00",
                        "value", 42));

        assertEquals("\"2014-03-10T23:32:47+00:00\",42,\n", csv);
        mapper.getFactory().enable(CsvGenerator.Feature.OMIT_MISSING_TAIL_COLUMNS);
        csv = mapper.writer(SCHEMA).writeValueAsString(
                ImmutableMap.of("timestamp", "2014-03-10T23:32:47+00:00",
                        "value", 42));
        assertEquals("\"2014-03-10T23:32:47+00:00\",42\n", csv);
    }
}
