package com.fasterxml.jackson.dataformat.csv.ser;

import com.fasterxml.jackson.databind.ObjectWriter;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

import com.google.common.collect.ImmutableMap;

public class TestWriterWithSomeMoreMissingValues extends ModuleTestBase {

    public void testWithAStringAndAUuid() {
        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = new CsvMapper().writer().with(schema);

        final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>();
        builder.put("string1", "hello");
        builder.put("string2", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3");

        final String csv = writer.writeValueAsString(builder.build());

        assertEquals("hello,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    public void testWithTwoStringsAndAUuid() {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = new CsvMapper().writer().with(schema);

        final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>();
        builder.put("string1", "hello");
        builder.put("string2", "world");
        builder.put("string3", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3");

        final String csv = writer.writeValueAsString(builder.build());

        assertEquals("hello,world,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    public void testWithANullAStringAndAUuid() {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = new CsvMapper().writer().with(schema);

        final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>();
        builder.put("string2", "world");
        builder.put("string3", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3");

        final String csv = writer.writeValueAsString(builder.build());

        assertEquals(",world,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    public void testWithAStringANullAndAUuid() {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = new CsvMapper().writer().with(schema);

        final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>();
        builder.put("string1", "hello");
        builder.put("string3", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3");

        final String csv = writer.writeValueAsString(builder.build());

        assertEquals("hello,,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    public void testWithThreeStringsAndAUuid() {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .addColumn("string4", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = new CsvMapper().writer().with(schema);

        final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>();
        builder.put("string1", "hello");
        builder.put("string2", "dear");
        builder.put("string3", "world");
        builder.put("string4", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3");

        final String csv = writer.writeValueAsString(builder.build());

        assertEquals("hello,dear,world,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    public void testWithANullAStringAStringAndAUuid() {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .addColumn("string4", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = new CsvMapper().writer().with(schema);

        final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>();
        builder.put("string2", "hello");
        builder.put("string3", "world");
        builder.put("string4", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3");

        final String csv = writer.writeValueAsString(builder.build());

        assertEquals(",hello,world,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    public void testWithAStringANullAStringAndAUuid() {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .addColumn("string4", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = new CsvMapper().writer().with(schema);

        final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>();
        builder.put("string1", "hello");
        builder.put("string3", "world");
        builder.put("string4", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3");

        final String csv = writer.writeValueAsString(builder.build());

        assertEquals("hello,,world,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    public void testWithTwoStringsANullAndAUuid() {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .addColumn("string4", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = new CsvMapper().writer().with(schema);

        final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>();
        builder.put("string1", "hello");
        builder.put("string2", "world");
        builder.put("string4", "2a36b911-9699-45d2-abd5-b9f2d2c9c4a3");

        final String csv = writer.writeValueAsString(builder.build());

        assertEquals("hello,world,,\"2a36b911-9699-45d2-abd5-b9f2d2c9c4a3\"\n", csv);
    }

    public void testWithTwoStringsANullAndAString() {

        final CsvSchema schema = new CsvSchema.Builder()
                .addColumn("string1", CsvSchema.ColumnType.STRING)
                .addColumn("string2", CsvSchema.ColumnType.STRING)
                .addColumn("string3", CsvSchema.ColumnType.STRING)
                .addColumn("string4", CsvSchema.ColumnType.STRING)
                .build();
        final ObjectWriter writer = new CsvMapper().writer().with(schema);

        final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>();
        builder.put("string1", "hello");
        builder.put("string2", "world");
        builder.put("string4", "again");

        final String csv = writer.writeValueAsString(builder.build());

        assertEquals("hello,world,,again\n", csv);
    }

    // [Issue#45]
    public void testWriteNullThirdColumn() {
        final CsvSchema.Builder csvSchemaBuilder = new CsvSchema.Builder();
        csvSchemaBuilder.addColumn("timestamp", CsvSchema.ColumnType.STRING);
        csvSchemaBuilder.addColumn("value", CsvSchema.ColumnType.NUMBER);
        csvSchemaBuilder.addColumn("id", CsvSchema.ColumnType.STRING);
        final CsvSchema schema = csvSchemaBuilder.build();
        final ObjectWriter writer = new CsvMapper().writer().with(schema);

        final String string = writer.writeValueAsString(
                ImmutableMap.of("timestamp", 0L, "value", 42));
        assertEquals("0,42,\n", string);
    }
}
