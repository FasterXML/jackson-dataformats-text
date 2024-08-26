package com.fasterxml.jackson.dataformat.csv.failing;

import java.io.StringWriter;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.SequenceWriter;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.ColumnType;
import com.fasterxml.jackson.dataformat.csv.CsvValueDecorator;
import com.fasterxml.jackson.dataformat.csv.CsvValueDecorators;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

// [dataformats-text#495]
public class WriteBracketedArray495Test extends ModuleTestBase
{
 // [dataformats-text#495]
    @JsonPropertyOrder({"id", "embeddings", "title" })
    static class Article {
        public int id;
        public String title;
        public double[] embeddings;

        protected Article() { }
        public Article(int id, String title, double[] embeddings) {
            this.id = id;
            this.title = title;
            this.embeddings = embeddings;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();

    // [dataformats-text#495]
    public void testBracketsWriteAutoSchema() throws Exception
    {
        final CsvSchema schema = _automaticSchema(true);
        _testArrayWithBracketsWrite(schema);
    }

    public void testBracketsManualSchemaArray() throws Exception
    {
        final CsvSchema schema = _manualSchema(ColumnType.ARRAY, true);
        _testArrayWithBracketsWrite(schema);
    }

    public void testBracketsManualSchemaString() throws Exception
    {
        final CsvSchema schema = _manualSchema(ColumnType.STRING, true);
        _testArrayWithBracketsWrite(schema);
    }

    private CsvSchema _automaticSchema(boolean required)
    {
        return MAPPER.schemaFor(Article.class)
                .withHeader()
                .withArrayElementSeparator(",")
                .withColumn("embeddings",
                        col -> col.withValueDecorator(_bracketDecorator(required)));
    }

    private CsvSchema _manualSchema(ColumnType ct, boolean required)
    {
        return CsvSchema.builder()
                .setUseHeader(true)
                .setArrayElementSeparator(",")
                .addColumn("id", ColumnType.STRING)
                // and then the interesting one; may mark as "String" or "Array"
                .addColumn("embeddings", ct,
                        col -> col.withValueDecorator(_bracketDecorator(required)))
                .addColumn("title", ColumnType.STRING)
                .build();
    }

    private CsvValueDecorator _bracketDecorator(boolean required) {
        return required
                ? CsvValueDecorators.STRICT_BRACKETS_DECORATOR
                        : CsvValueDecorators.OPTIONAL_BRACKETS_DECORATOR;
    }

    private void _testArrayWithBracketsWrite(CsvSchema schema) throws Exception
    {
        StringWriter stringW = new StringWriter();
        SequenceWriter sw = MAPPER.writerFor(Article.class)
                .with(schema)
                .writeValues(stringW);

        sw.write(new Article(123, "Title!", new double[] { 0.5, -0.25, 2.5 }));
        sw.close();

        assertEquals("id,embeddings,title\n"
                +"123,\"[0.5,-0.25,2.5]\",\"Title!\"",
                stringW.toString().trim());
    }
}
