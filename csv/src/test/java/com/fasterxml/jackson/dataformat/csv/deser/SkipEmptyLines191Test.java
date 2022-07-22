package com.fasterxml.jackson.dataformat.csv.deser;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectReader;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

// [dataformats-text#191]
// [dataformats-text#174]
public class SkipEmptyLines191Test extends ModuleTestBase {

    // [dataformats-text#174]
    @JsonPropertyOrder({ "timestamp", "random" })
    static class Row174 {
        private int timestamp;
        private String random;

        public int getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(int timestamp) {
            this.timestamp = timestamp;
        }

        public String getRandom() {
            return random;
        }

        public void setRandom(String random) {
            this.random = random;
        }

        @Override
        public String toString() {
            return "Row{timestamp=" + timestamp + ", random='" + random + "'}";
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final static CsvMapper MAPPER = new CsvMapper();

    // [dataformats-text#174]
    public void testEmptyLines174() throws Exception
    {
        final StringWriter sw = new StringWriter(50000);
        int lineCount = 0;
        final Random rnd = new Random();

        while (lineCount < 4000) {
            sw.append("\"" + System.currentTimeMillis()/1000 + "\",\"" + randomString(rnd) + "\"\n");
            ++lineCount;
        }
        final String doc = sw.toString();

        ObjectReader objectReader = MAPPER
                .readerFor(Row174.class)
                .with(CsvParser.Feature.SKIP_EMPTY_LINES)
                .with(MAPPER.schemaFor(Row174.class));

        MappingIterator<Row174> iterator = objectReader.readValues(doc);
        Row174 data = null;
        lineCount = 0;
        while (iterator.hasNext()) {
            ++lineCount;
            try {
                data = iterator.next();
            } catch (Exception e) {
                fail("Failed on row #"+lineCount+", previous row: "+data);
            }
        }
        iterator.close();
    }

    private String randomString(Random rnd) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; ++i) {
            sb.append((char) ('A' + (rnd.nextInt() & 0xF)));
        }
        return sb.toString();
    }

    // [dataformats-text#191]: IndexArrayOutOfBounds at 4000
    public void testBigCsvFile() throws Exception
    {
        final String COL_1 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        final String COL_2 = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        CsvSchema schema = CsvSchema
                .emptySchema()
                .withHeader()
                .withColumnSeparator(';')
                .withNullValue("null")
                .withComments();

        try (Reader r = new StringReader(_generate4kDoc())) {
            List<Map<String, String>> result = MAPPER
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
