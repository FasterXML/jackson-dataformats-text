package com.fasterxml.jackson.dataformat.csv.failing;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

// [dataformats-text#191]
public class ParserSkipEmpty191Test extends ModuleTestBase {

    private static String COL_1 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static String COL_2 = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    // [dataformats-text#191]: IndexArrayOutOfBounds at 4000
    public void testBigCsvFile() throws Exception {
        CsvSchema schema = CsvSchema
                .emptySchema()
                .withHeader()
                .withColumnSeparator(';')
                .withNullValue("null")
                .withComments();

        try (InputStream csvFileStream = getClass().getResourceAsStream("/csv/issue-191.csv")) {
            List<Map<String, String>> result = new CsvMapper()
                    .readerFor(Map.class)
                    .with(schema)
                    .<Map<String, String>>readValues(csvFileStream)
                    .readAll();
    
            for (Map<String, String> row : result) {
                assertEquals(row.get("COL_1"), COL_1);
                assertEquals(row.get("COL_2"), COL_2);
            }
        }
    }
}