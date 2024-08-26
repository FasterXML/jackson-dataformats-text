package com.fasterxml.jackson.dataformat.csv.failing;

import java.net.URL;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.MappingIterator;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.ColumnType;
import com.fasterxml.jackson.dataformat.csv.CsvValueDecorator;
import com.fasterxml.jackson.dataformat.csv.CsvValueDecorators;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

// [dataformats-text#442]
public class ReadBracketedArray442Test extends ModuleTestBase
{
    // [dataformats-text#442]
    @JsonPropertyOrder({"id", "title", "url", "score", "time", "comments", "author",
        "embeddings"
    })
    static class Article {
        public String id, title;
        public URL url;
        public int score;
        public long time; // Unix time
        public int comments;
        public String author;

        public double[] embeddings;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();

    private final byte[] FILE_100 = readResource("/data/story-100.csv");

    // [dataformats-text#442]
    public void testBracketsReadAutoSchema() throws Exception
    {
        _testArrayWithBracketsRead(FILE_100, _automaticSchema(true));
    }

    // [dataformats-text#442]
    public void testBracketsManualSchemaArray() throws Exception
    {
        _testArrayWithBracketsRead(FILE_100, _manualSchema(ColumnType.ARRAY, true));
    }
    
    // [dataformats-text#442]
    public void testBracketsManualSchemaString() throws Exception
    {
        _testArrayWithBracketsRead(FILE_100, _manualSchema(ColumnType.STRING, true));
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
        // second schema: manual construction
        return CsvSchema.builder()
                .setUseHeader(true)
                .addColumn("id", ColumnType.STRING)
                .addColumn("title", ColumnType.STRING)
                .addColumn("url", ColumnType.STRING)
                .addColumn("score", ColumnType.NUMBER)
                .addColumn("time", ColumnType.NUMBER)
                .addColumn("comments", ColumnType.NUMBER)
                .addColumn("author", ColumnType.STRING)
                // and then the interesting one; may mark as "String" or "Array"
                .addColumn("embeddings", ct,
                        col -> col.withValueDecorator(_bracketDecorator(required)))
                .build();
    }

    private CsvValueDecorator _bracketDecorator(boolean required) {
        return required
                ? CsvValueDecorators.STRICT_BRACKETS_DECORATOR
                        : CsvValueDecorators.OPTIONAL_BRACKETS_DECORATOR;
    }

    private void _testArrayWithBracketsRead(byte[] input, CsvSchema schema) throws Exception
    {
        MappingIterator<Article> it = MAPPER.readerFor(Article.class)
                .with(schema)
                .readValues(input);

        Article first = it.nextValue();
        assertNotNull(first);

        int count = 1;

        while (it.hasNextValue()) {
            assertNotNull(it.nextValue());
            ++count;
        }

        assertEquals(100, count);
    }
}
