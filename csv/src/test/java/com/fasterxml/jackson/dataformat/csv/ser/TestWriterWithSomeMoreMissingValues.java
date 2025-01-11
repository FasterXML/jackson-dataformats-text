package com.fasterxml.jackson.dataformat.csv.ser;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectWriter;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class TestWriterWithSomeMoreMissingValues extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();
    
    @Test
    public void testWithAStringAndAUuid() throws Exception
    {
        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = MAPPER.writer().with(schema);

        final Map<String, Object> map = mapOf(
                "string1", "hello",
                "string2", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3"
        );

        final String csv = writer.writeValueAsString(map);

        assertEquals("hello,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    @Test
    public void testWithTwoStringsAndAUuid() throws Exception
    {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = MAPPER.writer().with(schema);

        final Map<String, Object> map = mapOf(
                "string1", "hello",
                "string2", "world",
                "string3", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3"
        );

        final String csv = writer.writeValueAsString(map);

        assertEquals("hello,world,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    @Test
    public void testWithANullAStringAndAUuid() throws Exception
    {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = MAPPER.writer().with(schema);

        final Map<String, Object> map = mapOf(
                "string2", "world",
                "string3", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3"
        );

        final String csv = writer.writeValueAsString(map);

        assertEquals(",world,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    @Test
    public void testWithAStringANullAndAUuid() throws Exception
    {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = MAPPER.writer().with(schema);

        final Map<String, Object> map = mapOf(
                "string1", "hello",
                "string3", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3"
        );

        final String csv = writer.writeValueAsString(map);

        assertEquals("hello,,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    @Test
    public void testWithThreeStringsAndAUuid() throws Exception
    {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .addColumn("string4", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = MAPPER.writer().with(schema);

        final Map<String, Object> map = mapOf(
            "string1", "hello",
            "string2", "dear",
            "string3", "world",
            "string4", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3"
        );

        final String csv = writer.writeValueAsString(map);

        assertEquals("hello,dear,world,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    @Test
    public void testWithANullAStringAStringAndAUuid() throws Exception
    {
        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .addColumn("string4", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = MAPPER.writer().with(schema);

        final Map<String, Object> map = mapOf(
                "string2", "hello",
                "string3", "world",
                "string4", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3"
        );
        final String csv = writer.writeValueAsString(map);

        assertEquals(",hello,world,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    @Test
    public void testWithAStringANullAStringAndAUuid() throws Exception
    {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .addColumn("string4", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = MAPPER.writer().with(schema);

        final Map<String, Object> map = mapOf(
                "string1", "hello",
                "string3", "world",
                "string4", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3"
        );

        final String csv = writer.writeValueAsString(map);

        assertEquals("hello,,world,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    @Test
    public void testWithTwoStringsANullAndAUuid() throws Exception
    {
        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .addColumn("string4", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = MAPPER.writer().with(schema);

        final Map<String, Object> map = mapOf(
                "string1", "hello",
                "string2", "world",
                "string4", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3"
        );

        final String csv = writer.writeValueAsString(map);

        assertEquals("hello,world,,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    @Test
    public void testWithTwoStringsANullAndAString() throws Exception
    {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .addColumn("string4", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = MAPPER.writer().with(schema);

        final Map<String, String> map = new LinkedHashMap<>();
        map.put("string1", "hello");
        map.put("string2", "world");
        map.put("string4", "again");

        final String csv = writer.writeValueAsString(map);

        assertEquals("hello,world,,again\n", csv);
    }

    // [Issue#45]
    @Test
    public void testWriteNullThirdColumn() throws Exception
    {
        final CsvSchema.Builder csvSchemaBuilder = new CsvSchema.Builder();
        csvSchemaBuilder.addColumn("timestamp", CsvSchema.ColumnType.STRING);
        csvSchemaBuilder.addColumn("value", CsvSchema.ColumnType.NUMBER);
        csvSchemaBuilder.addColumn("id", CsvSchema.ColumnType.STRING);
        final CsvSchema schema = csvSchemaBuilder.build();
        final ObjectWriter writer = MAPPER.writer().with(schema);

        final String string = writer.writeValueAsString(
                mapOf("timestamp", 0L, "value", 42));
        assertEquals("0,42,\n", string);
    }
}
