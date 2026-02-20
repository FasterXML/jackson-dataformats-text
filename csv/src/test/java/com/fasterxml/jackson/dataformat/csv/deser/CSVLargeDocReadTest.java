package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

// Tests for StreamReadConstraints.maxDocumentLength() enforcement in CSV parser
public class CSVLargeDocReadTest extends ModuleTestBase
{
    private static final int DOC_LEN_LIMIT = 10_000;

    private CsvFactory csvFactoryWithDocLenLimit(long limit) {
        return CsvFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxDocumentLength(limit)
                        .build())
                .build();
    }

    public void testDocumentExceedingLimitFails() throws Exception
    {
        final String doc = generateCsv(DOC_LEN_LIMIT + 5_000);
        CsvFactory f = csvFactoryWithDocLenLimit(DOC_LEN_LIMIT);
        try (JsonParser p = f.createParser(doc)) {
            while (p.nextToken() != null) { }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            final String msg = e.getMessage();
            assertTrue("unexpected message: " + msg, msg.contains("Document length"));
            assertTrue("unexpected message: " + msg, msg.contains("exceeds the maximum allowed ("));
        }
    }

    public void testDocumentWithinLimitSucceeds() throws Exception
    {
        final String doc = generateCsv(1_000);
        CsvFactory f = csvFactoryWithDocLenLimit(DOC_LEN_LIMIT);
        try (JsonParser p = f.createParser(doc)) {
            while (p.nextToken() != null) { }
        }
    }

    private String generateCsv(int targetLen) {
        // "value1,value2,value3\n" = 22 chars per row
        final String row = "value1,value2,value3\n";
        final StringBuilder sb = new StringBuilder(targetLen + row.length());
        while (sb.length() < targetLen) {
            sb.append(row);
        }
        return sb.toString();
    }
}
