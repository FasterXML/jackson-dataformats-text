package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

import static org.junit.Assert.assertArrayEquals;

// for [dataformats-text#15]: Allow skipping of empty lines
public class SkipBlankLines15Test extends ModuleTestBase {

    private static final String CSV_WITH_EMPTY_LINE = "1,\"xyz\"\n\ntrue,\n";
    private static final String CSV_WITH_BLANK_LINE = "1,\"xyz\"\n   \ntrue,\n";
    private static final String CSV_WITH_BLANK_LINE_AND_COMMENT = "1,\"xyz\"\n \n  #comment\n\ntrue,\n";
    private static final String CSV_WITH_FIRST_BLANK_LINE = "\n1,\"xyz\"\ntrue,\n";
    private static final String CSV_WITH_TRAILING_BLANK_LINES = "1,\"xyz\"\ntrue,\n  \n\n";

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
                .with(CsvParser.Feature.SKIP_BLANK_LINES)
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
                .with(CsvParser.Feature.SKIP_BLANK_LINES)
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

    public void testCsvWithBlankLineAndCommentSkipBlankLinesFeatureEnabled() throws Exception {
        String[][] rows = mapperForCsvAsArray()
                .with(CsvParser.Feature.SKIP_BLANK_LINES)
                .readValue(CSV_WITH_BLANK_LINE_AND_COMMENT);
        // blank/empty lines are skipped
        assertArrayEquals(expected(
                row("1", "xyz"),
                row("  #comment"),
                row("true", "")
        ), rows);
    }

    public void testCsvWithBlankLineAndCommentSkipBlankLinesFeatureEnabledAndAllowComments() throws Exception {
        String[][] rows = mapperForCsvAsArray()
                .with(CsvParser.Feature.SKIP_BLANK_LINES)
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
                .with(CsvParser.Feature.SKIP_BLANK_LINES)
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
                .with(CsvParser.Feature.SKIP_BLANK_LINES)
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
