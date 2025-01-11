package com.fasterxml.jackson.dataformat.csv;

import java.io.*;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectReader;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class NullReader122Test extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    // for [dataformats-text#122]: passing `null` Reader leads to infinite loop
    @Test
    public void testEmptyStream() throws Exception {
        CsvSchema columns = CsvSchema.emptySchema().withHeader().withColumnSeparator(';');
        ObjectReader r = MAPPER.readerFor(Map.class).with(columns);
        try {
            /*Object ob =*/ r.readValue((Reader) null);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Argument \"r\" is null");
        }
    }
}
