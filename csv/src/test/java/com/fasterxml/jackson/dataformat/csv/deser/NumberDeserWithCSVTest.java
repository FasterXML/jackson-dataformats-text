package com.fasterxml.jackson.dataformat.csv.deser;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

// Tests copied from databind "JDKNumberDeserTest" (only a small subset)
public class NumberDeserWithCSVTest extends ModuleTestBase
{
    // [databind#2784]
    static class BigDecimalHolder2784 {
        public BigDecimal value;
    }

    static class NestedBigDecimalHolder2784 {
        @JsonUnwrapped
        public BigDecimalHolder2784 holder;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = newObjectMapper();

    // [databind#2784]
    public void testBigDecimalUnwrapped() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(NestedBigDecimalHolder2784.class).withHeader()
                .withStrictHeaders(true);
        final String DOC = "value\n5.00\n";
        NestedBigDecimalHolder2784 result = MAPPER.readerFor(NestedBigDecimalHolder2784.class)
                .with(schema)
                .readValue(DOC);
        assertEquals(new BigDecimal("5.00"), result.holder.value);
    }
}
