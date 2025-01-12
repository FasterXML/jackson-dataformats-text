package com.fasterxml.jackson.dataformat.csv.ser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestGeneratorWithCustomSeparators extends ModuleTestBase
{
    // #17
    @Test
    public void testOtherSeparator() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class).withoutHeader().withColumnSeparator(';');
        FiveMinuteUser user = new FiveMinuteUser("Barbie", "Benton", false, Gender.FEMALE, null);
        String result = mapper.writer(schema).writeValueAsString(user);
        assertEquals("Barbie;Benton;FEMALE;false;\n", result);
    }

    @Test
    public void testTSV() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class).withoutHeader().withColumnSeparator('\t');
        FiveMinuteUser user = new FiveMinuteUser("Barbie", "Benton", false, Gender.FEMALE, null);
        String result = mapper.writer(schema).writeValueAsString(user);
        assertEquals("Barbie\tBenton\tFEMALE\tfalse\t\n", result);
    }
}
