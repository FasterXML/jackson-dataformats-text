package tools.jackson.dataformat.csv.ser;

import tools.jackson.dataformat.csv.*;

public class GeneratorQuotingTest extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();

    // [dataformats-text#220]
    public void testQuotingOfLinefeedsStd() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");
        String csv;

        csv = MAPPER.writer(schema)
                .without(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(new IdDesc("12\n3", "Foo"));
        assertEquals("\"12\n3\",Foo\n", csv);

        csv = MAPPER.writer(schema.withEscapeChar('\\'))
                .without(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(new IdDesc("12\n3", "Foo"));
        assertEquals("\"12\n3\",Foo\n", csv);
        
        csv = MAPPER.writer(schema)
                .with(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(new IdDesc("12\n3", "Foo"));
        assertEquals("\"12\n3\",Foo\n", csv);
        csv = MAPPER.writer(schema.withEscapeChar('\\'))
                .with(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(new IdDesc("12\n3", "Foo"));
        assertEquals("\"12\n3\",Foo\n", csv);
    }

    public void testQuotingOfLinefeedsCustom() throws Exception
    {
        // '-' is bigger than max('"', ','):
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("-");
        final IdDesc value = new IdDesc("12-3", "Foo");

        // with loose(default) quoting
        String csv = MAPPER.writer(schema)
                .without(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(value);
        assertEquals("\"12-3\",Foo-", csv);

        // with loose(default) quoting and escape char
        csv = MAPPER.writer(schema.withEscapeChar('\\'))
            .without(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
            .writeValueAsString(value);
        assertEquals("\"12-3\",Foo-", csv);

        // with strict/optimal
        csv = MAPPER.writer(schema)
                .with(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(value);
        assertEquals("\"12-3\",Foo-", csv);

        // with strict/optimal and escape char
        csv = MAPPER.writer(schema.withEscapeChar('\\'))
                .with(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(value);
        assertEquals("\"12-3\",Foo-", csv);
    }
}
