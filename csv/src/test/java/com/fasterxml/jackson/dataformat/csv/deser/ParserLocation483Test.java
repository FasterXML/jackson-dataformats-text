package com.fasterxml.jackson.dataformat.csv.deser;

import java.util.*;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParserLocation483Test extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    // [dataformats-text#483]: Location incorrect
    @Test
    public void testAsSequence() throws Exception
    {
        try (MappingIterator<List<String>> reader = MAPPER
                .readerForListOf(String.class)
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues("name,dob\n\"string without end")) {
            reader.readAll();
        } catch (JacksonException e) {
            verifyException(e, "Missing closing quote");
            assertEquals(2, e.getLocation().getLineNr());
            // This is not always accurate but should be close:
            assertEquals(20, e.getLocation().getColumnNr());
        }
    }
}
