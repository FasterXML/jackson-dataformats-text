package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.csv.*;

// Tests for [dataformat-csv#26]
public class TestParserStrictQuoting extends ModuleTestBase
{
    @JsonPropertyOrder({"a", "b"})
    protected static class AB {
        public String a, b;

        public AB() { }
        public AB(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testStrictQuoting() throws Exception
    {
        final String NUMS = "12345 6789";
        final String LONG = NUMS + NUMS + NUMS + NUMS; // 40 chars should do it
        
        CsvMapper mapper = mapperForCsv();

        assertFalse(mapper.getFactory().isEnabled(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING));
        CsvSchema schema = mapper.schemaFor(AB.class).withoutHeader();

        final AB input = new AB("x", LONG);
        
        // with non-strict, should quote
        String csv = mapper.writer(schema).writeValueAsString(input);
        assertEquals(aposToQuotes("x,'"+LONG+"'"), csv.trim());

        // should be possible to hot-swap
        // and with strict/optimal, no quoting
        mapper.configure(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING, true);
        csv = mapper.writer(schema).writeValueAsString(input);
        assertEquals(aposToQuotes("x,"+LONG), csv.trim());
    }
}
