package tools.jackson.dataformat.csv.failing;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.exc.StreamReadException;

import tools.jackson.databind.ObjectReader;

import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.ModuleTestBase;

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
    public void testNoOverflow485() throws Exception
    {
        Numbers485 result = READER.readValue(csv485(13, 42L));
        assertEquals(13, result.intValue);
        assertEquals(42L, result.longValue);
    }

    public void testIntOverflow() throws Exception
    {
        try {
            Numbers485 result = READER.readValue(csv485("111111111111111111111111111111111111111111", "0"));
            fail("Should not pass; got: "+result.intValue);
        } catch (StreamReadException e) {
            verifyException(e, "Numeric value");
            verifyException(e, "out of range of int");
        }
    }

    public void testLongOverflow() throws Exception
    {
        try {
            Numbers485 result = READER.readValue(csv485("0",
                    "2222222222222222222222222222222222222222"));
            fail("Should not pass; got: "+result.longValue);
        } catch (StreamReadException e) {
            verifyException(e, "Numeric value");
            verifyException(e, "out of range of long");
        }
    }

    private static String csv485(Object intValue, Object longValue) {
        return intValue+","+longValue+"\n";
    }
}
