package tools.jackson.dataformat.csv.deser;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.MappingIterator;

import tools.jackson.dataformat.csv.*;
import tools.jackson.dataformat.csv.CsvSchema.ColumnType;

import static org.junit.jupiter.api.Assertions.*;

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
    /* Test methods, success cases
    /**********************************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();

    private final byte[] FILE_100 = readResource("/data/story-100.csv");

    private final byte[] STRING_1 = ("id,title,url,score,time,comments,author,embeddings\n"
            + "a1,Cool title,http://foo.org,123,0,3,unknown,\"[1.0, 2.0]\"\n")
            .getBytes(StandardCharsets.UTF_8);

    // [dataformats-text#442]
    @Test
    public void testBracketsReadAutoSchema() throws Exception
    {
        final CsvSchema schema1 = _automaticSchema(true);
        final CsvSchema schema2 = _automaticSchema(false);

        _testArrayWithBracketsRead1(schema1);
        _testArrayWithBracketsRead1(schema2);
        _testArrayWithBracketsRead100(schema1);
        _testArrayWithBracketsRead100(schema2);
    }

    // [dataformats-text#442]
    @Test
    public void testBracketsManualSchemaArray() throws Exception
    {
        final CsvSchema schema = _manualSchema(ColumnType.ARRAY, true);
        _testArrayWithBracketsRead1(schema);
        _testArrayWithBracketsRead100(schema);
    }
    
    // [dataformats-text#442]
    @Test
    public void testBracketsManualSchemaString() throws Exception
    {
        final CsvSchema schema = _manualSchema(ColumnType.STRING, true);
        _testArrayWithBracketsRead1(schema);
        _testArrayWithBracketsRead100(schema);
    }

    /*
    /**********************************************************************
    /* Test methods, fail cases
    /**********************************************************************
     */

    @Test
    public void testBracketReadAutoSchemaFail() throws Exception
    {
        final CsvSchema schema = _automaticSchema(true);
        MappingIterator<Article> it = MAPPER.readerFor(Article.class)
                .with(schema)
                .readValues("id,title,url,score,time,comments,author,embeddings\n"
                        + "a1,Cool title,http://foo.org,123,0,3,unknown,\"1.0, 2.0\"\n");

        try {
            /*Article first =*/ it.nextValue();
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Decorated value of column 'embeddings' does not start with");
        }

        it = MAPPER.readerFor(Article.class)
                .with(schema)
                .readValues("id,title,url,score,time,comments,author,embeddings\n"
                        + "a1,Cool title,http://foo.org,123,0,3,unknown,\"[1.0, 2.0\"\n");

        try {
            /*Article first =*/ it.nextValue();
            fail("Should not pass");
        } catch (CsvReadException e) {
            verifyException(e, "Decorated value of column 'embeddings' does not end with");
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

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
                .setArrayElementSeparator(",")
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

    private void _testArrayWithBracketsRead1(CsvSchema schema) throws Exception
    {
        MappingIterator<Article> it = MAPPER.readerFor(Article.class)
                .with(schema)
                .readValues(STRING_1);

        Article first = it.nextValue();
        assertNotNull(first);
        assertNotNull(first.embeddings);
        assertEquals(2, first.embeddings.length);

        assertFalse(it.hasNextValue());
    }

    private void _testArrayWithBracketsRead100(CsvSchema schema) throws Exception
    {
        MappingIterator<Article> it = MAPPER.readerFor(Article.class)
                .with(schema)
                .readValues(FILE_100);

        Article first = it.nextValue();
        assertNotNull(first);
        assertNotNull(first.embeddings);
        assertEquals(1536, first.embeddings.length);

        int count = 1;

        while (it.hasNextValue()) {
            Article article = it.nextValue();
            assertNotNull(article);
            assertNotNull(article.embeddings);
            assertEquals(1536, article.embeddings.length);
            ++count;
        }

        assertEquals(100, count);
    }
}
