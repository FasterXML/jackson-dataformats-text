package com.fasterxml.jackson.dataformat.csv.failing;

import java.net.URL;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.MappingIterator;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.ColumnType;
import com.fasterxml.jackson.dataformat.csv.CsvValueDecorator;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

// [dataformats-text#442]
public class ReadBracketedArray442Test extends ModuleTestBase
{
    // [dataformats-text#442]
    @JsonPropertyOrder({"id", "title", "url", "score", "time", "comments", "author",
        "embeddings"
    })
    static class Article {
        public String id, title, author;
        public URL url;
        public int comments, score;
        public long time; // Unix time

        public double[] embeddings;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();

    // [dataformats-text#442]
    public void testArrayWithBracketsRead() throws Exception
    {
        byte[] input = readResource("/data/story-100.csv");

        // Test with schema constructed a few different ways
        _testArrayWithBracketsRead(input, _automaticSchema());
        _testArrayWithBracketsRead(input, _manualSchema(ColumnType.STRING));
        _testArrayWithBracketsRead(input, _manualSchema(ColumnType.ARRAY));
    }

    private CsvSchema _automaticSchema()
    {
        return MAPPER.schemaFor(Article.class)
                .withHeader()
                .withArrayElementSeparator(",");
    }

    private CsvSchema _manualSchema(ColumnType ct)
    {
        // second schema: manual construction
        return CsvSchema.builder()
                .addColumn("id", ColumnType.STRING)
                .addColumn("title", ColumnType.STRING)
                .addColumn("author", ColumnType.STRING)
                .addColumn("comments", ColumnType.NUMBER)
                .addColumn("score", ColumnType.NUMBER)
                .addColumn("time", ColumnType.NUMBER)
                // and then the interesting one; may mark as "String" or "Array"
                .addColumn("embedding", ct,
                        col -> col.withValueDecorator(_bracketDecorator()))
                .build();
    }

    private CsvValueDecorator _bracketDecorator() {
        return null;
    }

    private void _testArrayWithBracketsRead(byte[] input, CsvSchema schema) throws Exception
    {
        MappingIterator<Article> it = MAPPER.readerFor(Article.class)
                .with(schema)
                .readValues(input);

        Article first = it.nextValue();
        assertNotNull(first);
    }
}
