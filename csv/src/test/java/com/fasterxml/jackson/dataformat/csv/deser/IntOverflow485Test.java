package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectReader;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntOverflow485Test extends ModuleTestBase
{
    // [dataformats-text#485]
    @JsonPropertyOrder({ "testInt", "testLong" })
    static class Numbers485 {
        public int intValue;
        public long longValue;
    }

    private final CsvMapper MAPPER = mapperForCsv();
    private final ObjectReader READER = MAPPER.readerWithSchemaFor(Numbers485.class);
    
    // [dataformats-text#485]

    // First test that regular parsing works
    @Test
    public void testNoOverflow485() throws Exception
    {
        Numbers485 result = READER.readValue(csv485(13, 42L));
        assertEquals(13, result.intValue);
        assertEquals(42L, result.longValue);
    }

    @Test
    public void testIntOverflow() throws Exception
    {
        try {
            Numbers485 result = READER.readValue(csv485("111111111111111111111111111111111111111111", "0"));
            fail("Should not pass; got: "+result.intValue);
        } catch (DatabindException e) { // in 2.x gets wrapped
            verifyException(e, "Numeric value");
            verifyException(e, "out of range of int");
        }
    }

    @Test
    public void testLongOverflow() throws Exception
    {
        try {
            Numbers485 result = READER.readValue(csv485("0",
                    "2222222222222222222222222222222222222222"));
            fail("Should not pass; got: "+result.longValue);
        } catch (DatabindException e) { // in 2.x gets wrapped
            verifyException(e, "Numeric value");
            verifyException(e, "out of range of long");
        }
    }

    private static String csv485(Object intValue, Object longValue) {
        return intValue+","+longValue+"\n";
    }
}
