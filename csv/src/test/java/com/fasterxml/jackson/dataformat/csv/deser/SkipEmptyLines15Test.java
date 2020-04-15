package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

import static org.junit.Assert.assertArrayEquals;

// for [dataformats-text#15]: Allow skipping of empty lines
public class SkipEmptyLines15Test extends ModuleTestBase {

    private static final String CSV_WITH_EMPTY_LINE = "1,\"xyz\"\n\ntrue,\n";
    private static final String CSV_WITH_BLANK_LINE = "1,\"xyz\"\n   \ntrue,\n";
    private static final String CSV_WITH_BLANK_LINE_AND_COMMENT = "1,\"xyz\"\n \n  #comment\n\ntrue,\n";
    private static final String CSV_WITH_FIRST_BLANK_LINE = "\n1,\"xyz\"\ntrue,\n";
    private static final String CSV_WITH_TRAILING_BLANK_LINES = "1,\"xyz\"\ntrue,\n  \n\n";

    @JsonPropertyOrder({ "age", "name", "cute" })
    protected static class Entry {
        public int age;
        public String name;
        public boolean cute;
    }

    // for [dataformats-text#15]: Allow skipping of empty lines
    public void testSkipEmptyLinesFeature() throws Exception
    {
        final String CSV = "1,\"xyz\"\n\ntrue,\n";
        
        CsvMapper mapper = mapperForCsv();

        // First, verify default behavior:

        String[][] rows = mapper
                .readerFor(String[][].class)
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValue(CSV);
        assertEquals(3, rows.length);
        String[] row;

        row = rows[0];
        assertEquals(2, row.length);
        assertEquals("1",row[0]);
        assertEquals("xyz", row[1]);

        row = rows[1];
        assertEquals(1, row.length);
        assertEquals("", row[0]);

        row = rows[2];
        assertEquals(2, row.length);
        assertEquals("true", row[0]);
        assertEquals("", row[1]);

        // when wrapped as an array, we'll get array of Lists:
        rows = mapper.readerFor(String[][].class)
                .with(CsvParser.Feature.SKIP_EMPTY_LINES)
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValue(CSV);

        assertEquals(2, rows.length);
        row = rows[0];
        assertEquals(2, row.length);
        assertEquals("1",row[0]);
        assertEquals("xyz", row[1]);

        row = rows[1];
        assertEquals(2, row.length);
        assertEquals("true", row[0]);
        assertEquals("", row[1]);
    }

    public void testCsvWithEmptyLineSkipBlankLinesFeatureDisabled() throws Exception {
        String[][] rows = mapperForCsvAsArray().readValue(CSV_WITH_EMPTY_LINE);
        // First, verify default behavior:
        assertArrayEquals(expected(
                row("1", "xyz"),
                row(""),
                row("true", "")
        ), rows);
    }

    public void testCsvWithEmptyLineSkipBlankLinesFeatureEnabled() throws Exception {
        String[][] rows = mapperForCsvAsArray()
                .with(CsvParser.Feature.SKIP_EMPTY_LINES)
                .readValue(CSV_WITH_EMPTY_LINE);
        // empty line is skipped
        assertArrayEquals(expected(
                row("1", "xyz"),
                row("true", "")
        ), rows);
    }


    public void testCsvWithBlankLineSkipBlankLinesFeatureDisabled() throws Exception {
        String[][] rows = mapperForCsvAsArray()
                .readValue(CSV_WITH_BLANK_LINE);
        // First, verify default behavior:
        assertArrayEquals(expected(
                row("1", "xyz"),
                row("   "),
                row("true", "")
        ), rows);
    }

    public void testCsvWithBlankLineSkipBlankLinesFeatureEnabled() throws Exception {
        String[][] rows = mapperForCsvAsArray()
                .with(CsvParser.Feature.SKIP_EMPTY_LINES)
                .readValue(CSV_WITH_BLANK_LINE);
        // blank line is skipped
        assertArrayEquals(expected(
                row("1", "xyz"),
                row("true", "")
        ), rows);
    }

    public void testCsvWithBlankLineAndCommentSkipBlankLinesFeatureDisabled() throws Exception {
        String[][] rows = mapperForCsvAsArray()
                .readValue(CSV_WITH_BLANK_LINE_AND_COMMENT);
        // First, verify default behavior:
        assertArrayEquals(expected(
                row("1", "xyz"),
                row(" "),
                row("  #comment"),
                row(""),
                row("true", "")
        ), rows);
    }

    // 14-Apr-2020, tatu: Due to [dataformats-text#191], can not retain leading spaces
    //   when trimming empty lines and/or comments, so test changed for 2.11
    public void testCsvWithBlankLineAndCommentSkipBlankLinesFeatureEnabled() throws Exception {
        String[][] rows = mapperForCsvAsArray()
                .with(CsvParser.Feature.SKIP_EMPTY_LINES)
                .readValue(CSV_WITH_BLANK_LINE_AND_COMMENT);
        // blank/empty lines are skipped
        assertArrayEquals(expected(
                row("1", "xyz"),
                // As per: [dataformats-text#191]
//                row("  #comment"),
                row("#comment"),
                row("true", "")
        ), rows);
    }

    public void testCsvWithBlankLineAndCommentSkipBlankLinesFeatureEnabledAndAllowComments() throws Exception {
        String[][] rows = mapperForCsvAsArray()
                .with(CsvParser.Feature.SKIP_EMPTY_LINES)
                .with(CsvParser.Feature.ALLOW_COMMENTS)
                .readValue(CSV_WITH_BLANK_LINE_AND_COMMENT);
        // blank/empty/comment lines are skipped

        assertArrayEquals(expected(
                row("1", "xyz"),
                row("true", "")
        ), rows);
    }

    public void testCsvWithFirstBlankLineSkipBlankLinesFeatureDisabled() throws Exception {
        String[][] rows = mapperForCsvAsArray()
                .readValue(CSV_WITH_FIRST_BLANK_LINE);
        // First, verify default behavior:
        assertArrayEquals(expected(
                row(""),
                row("1", "xyz"),
                row("true", "")
        ), rows);
    }

    public void testCsvWithFirstBlankLineSkipBlankLinesFeatureEnabled() throws Exception {
        String[][] rows = mapperForCsvAsArray()
                .with(CsvParser.Feature.SKIP_EMPTY_LINES)
                .readValue(CSV_WITH_FIRST_BLANK_LINE);
        // blank line is skipped
        assertArrayEquals(expected(
                row("1", "xyz"),
                row("true", "")
        ), rows);
    }


    public void testCsvWithTrailingBlankLineSkipBlankLinesFeatureDisabled() throws Exception {
        String[][] rows = mapperForCsvAsArray()
                .readValue(CSV_WITH_TRAILING_BLANK_LINES);
        // First, verify default behavior:
        assertArrayEquals(expected(
                row("1", "xyz"),
                row("true", ""),
                row("  "),
                row("")
        ), rows);
    }

    public void testCsvWithTrailingBlankLineSkipBlankLinesFeatureEnabled() throws Exception {
        String[][] rows = mapperForCsvAsArray()
                .with(CsvParser.Feature.SKIP_EMPTY_LINES)
                .readValue(CSV_WITH_FIRST_BLANK_LINE);
        // blank lines are skipped
        assertArrayEquals(expected(
                row("1", "xyz"),
                row("true", "")
        ), rows);
    }

    private ObjectReader mapperForCsvAsArray() {
        // when wrapped as an array, we'll get array of Lists:
        return mapperForCsv()
                .readerFor(String[][].class)
                .with(CsvParser.Feature.WRAP_AS_ARRAY);
    }

    private String[][] expected(String[]... rowInputs) {
        return rowInputs;
    }

    private  String[] row(String... cellInputs) {
        return cellInputs;
    }
}
