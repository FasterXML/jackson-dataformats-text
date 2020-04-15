package com.fasterxml.jackson.dataformat.csv.deser;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

// [dataformats-text#191]
public class SkipEmptyLines191Test extends ModuleTestBase {

    private static String COL_1 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static String COL_2 = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    // [dataformats-text#191]: IndexArrayOutOfBounds at 4000
    public void testBigCsvFile() throws Exception
    {
        CsvSchema schema = CsvSchema
                .emptySchema()
                .withHeader()
                .withColumnSeparator(';')
                .withNullValue("null")
                .withComments();

        try (Reader r = new StringReader(_generate4kDoc())) {
            List<Map<String, String>> result = new CsvMapper()
                    .readerFor(Map.class)
                    .with(schema)
                    .<Map<String, String>>readValues(r)
                    .readAll();
    
            for (Map<String, String> row : result) {
                assertEquals(row.get("COL_1"), COL_1);
                assertEquals(row.get("COL_2"), COL_2);
            }
        }
    }

    private String _generate4kDoc() {
        StringBuilder sb = new StringBuilder(5000)
.append("COL_1;COL_2\n")
.append("# csv file with a newline at char 4000 (the buffer size) to verify a bug\n")
.append("# LF has to be used for newlines to work\n")
.append("# alignment chars to have the newline as the 4000s char: ----------------\n");
        for (int i = 0; i < 40; ++i) {
            sb.append("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\n");
        }
        return sb.toString();
    }
}
