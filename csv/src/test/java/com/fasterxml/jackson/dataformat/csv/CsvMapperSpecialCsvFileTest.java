package com.fasterxml.jackson.dataformat.csv;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class CsvMapperSpecialCsvFileTest extends ModuleTestBase {

    private static String COL_1 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static String COL_2 = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    /**
     * tries to parse a csv file with a newline at char 4000
     */
    public void testBigCsvFile() throws Exception {
        CsvSchema schema = CsvSchema
                .emptySchema()
                .withHeader()
                .withColumnSeparator(';')
                .withNullValue("null")
                .withComments();

        InputStream csvFileStream = this.getClass().getResourceAsStream("/csv/specialCsvFile.csv");
        Reader csvFileReader = new InputStreamReader(csvFileStream, StandardCharsets.UTF_8);

        List<Map<String, String>> result = new CsvMapper()
                .readerFor(Map.class)
                .with(schema)
                .<Map<String, String>>readValues(csvFileReader)
                .readAll();

        for (Map<String, String> row : result) {
            assertEquals(row.get("COL_1"), COL_1);
            assertEquals(row.get("COL_2"), COL_2);
        }

    }
}