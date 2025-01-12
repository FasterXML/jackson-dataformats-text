package tools.jackson.dataformat.csv.ser;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.SequenceWriter;

import tools.jackson.dataformat.csv.*;
import tools.jackson.dataformat.csv.CsvSchema.ColumnType;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [dataformats-text#495]
public class WriteBracketedArray495Test extends ModuleTestBase
{
 // [dataformats-text#495]: 
    @JsonPropertyOrder({"id", "embeddings", "title", "extra" })
    static class Article {
        public int id;
        public String title;
        public double[] embeddings;
        public int extra;

        protected Article() { }
        public Article(int id, String title, int extra, double[] embeddings) {
            this.id = id;
            this.title = title;
            this.extra = extra;
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
    @Test
    public void testBracketsWriteAutoSchema() throws Exception
    {
        final CsvSchema schema = _automaticSchema();
        _testArrayWithBracketsWrite(schema);
    }

    @Test
    public void testBracketsManualSchemaArray() throws Exception
    {
        final CsvSchema schema = _manualSchema(ColumnType.ARRAY);
        _testArrayWithBracketsWrite(schema);
    }

    @Test
    public void testBracketsManualSchemaString() throws Exception
    {
        final CsvSchema schema = _manualSchema(ColumnType.STRING);
        _testArrayWithBracketsWrite(schema);
    }

    private CsvSchema _automaticSchema()
    {
        return MAPPER.schemaFor(Article.class)
                .withHeader()
                .withArrayElementSeparator(",")
                .withColumn("embeddings",
                        col -> col.withValueDecorator(_bracketDecorator()))
                .withColumn("title",
                        col -> col.withValueDecorator(_parenthesisDecorator()))
                .withColumn("extra",
                        col -> col.withValueDecorator(_curlyDecorator()));
    }

    private CsvSchema _manualSchema(ColumnType ct)
    {
        return CsvSchema.builder()
                .setUseHeader(true)
                .setArrayElementSeparator(",")
                .addColumn("id", ColumnType.NUMBER)
                // and then the interesting one; may mark as "String" or "Array"
                .addColumn("embeddings", ct,
                        col -> col.withValueDecorator(_bracketDecorator()))
                .addColumn("title", ColumnType.STRING,
                        col -> col.withValueDecorator(_parenthesisDecorator()))
                .addColumn("extra", ColumnType.NUMBER,
                        col -> col.withValueDecorator(_curlyDecorator()))
                .build();
    }

    private CsvValueDecorator _bracketDecorator() {
        return CsvValueDecorators.STRICT_BRACKETS_DECORATOR;
    }

    private CsvValueDecorator _parenthesisDecorator() {
        return CsvValueDecorators.requiredPrefixSuffixDecorator("(", ")");
    }

    private CsvValueDecorator _curlyDecorator() {
        return CsvValueDecorators.requiredPrefixSuffixDecorator("{", "}");
    }

    private void _testArrayWithBracketsWrite(CsvSchema schema) throws Exception
    {
        StringWriter stringW = new StringWriter();
        SequenceWriter sw = MAPPER.writerFor(Article.class)
                .with(schema)
                .writeValues(stringW);

        sw.write(new Article(123, "Title!", 42, new double[] { 0.5, -0.25, 2.5 }));
        sw.close();

        assertEquals("id,embeddings,title,extra\n"
                +"123,\"[0.5,-0.25,2.5]\",\"(Title!)\",{42}",
                stringW.toString().trim());
    }
}
