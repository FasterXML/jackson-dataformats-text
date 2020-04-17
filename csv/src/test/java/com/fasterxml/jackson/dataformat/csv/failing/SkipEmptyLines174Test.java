package com.fasterxml.jackson.dataformat.csv.failing;

import java.io.*;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.csv.*;

public class SkipEmptyLines174Test extends ModuleTestBase
{
    private final static CsvMapper MAPPER = new CsvMapper();

    @JsonPropertyOrder({ "timestamp", "random" })
    static class Row {
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

    public void testEmptyLines174() throws Exception
    {
        String doc = generateCsvFile();
        ObjectReader objectReader = MAPPER
                .enable(CsvParser.Feature.SKIP_EMPTY_LINES)
                .readerFor(Row.class)
                .with(MAPPER.schemaFor(Row.class));
        int lineCount = 0;
        MappingIterator<Row> iterator = objectReader.readValues(doc);
        Row data = null;
        while (iterator.hasNext()) {
            ++lineCount;
            try {
                data = iterator.next();
            } catch (Exception e) {
//            System.out.println(lineCount + " : " + data);
                fail("Failed on row #"+lineCount+", previous row: "+data);
            }
        }
        iterator.close();
    }

    private String generateCsvFile() throws Exception {
        final StringWriter sw = new StringWriter(50000);
        int lineCount = 0;
        final Random rnd = new Random();

        while (lineCount < 4000) {
            sw.append("\"" + System.currentTimeMillis()/1000 + "\",\"" + randomString(rnd) + "\"\n");
            ++lineCount;
        }
        return sw.toString();
    }

    private String randomString(Random rnd) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; ++i) {
            sb.append((char) ('A' + (rnd.nextInt() & 0xF)));
        }
        return sb.toString();
    }
}
