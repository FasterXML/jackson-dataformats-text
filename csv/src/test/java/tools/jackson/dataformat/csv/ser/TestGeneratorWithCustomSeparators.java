package tools.jackson.dataformat.csv.ser;

import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.ModuleTestBase;

public class TestGeneratorWithCustomSeparators extends ModuleTestBase
{
    // #17
    public void testOtherSeparator() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class).withoutHeader().withColumnSeparator(';');
        FiveMinuteUser user = new FiveMinuteUser("Barbie", "Benton", false, Gender.FEMALE, null);
        String result = mapper.writer(schema).writeValueAsString(user);
        assertEquals("Barbie;Benton;FEMALE;false;\n", result);
    }

    public void testTSV() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class).withoutHeader().withColumnSeparator('\t');
        FiveMinuteUser user = new FiveMinuteUser("Barbie", "Benton", false, Gender.FEMALE, null);
        String result = mapper.writer(schema).writeValueAsString(user);
        assertEquals("Barbie\tBenton\tFEMALE\tfalse\t\n", result);
    }
}
