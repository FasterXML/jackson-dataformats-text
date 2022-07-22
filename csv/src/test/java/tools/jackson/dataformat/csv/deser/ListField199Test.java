package tools.jackson.dataformat.csv.deser;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectReader;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.ModuleTestBase;

public class ListField199Test extends ModuleTestBase
{
    // [dataformats-text#199]
    static class ModelString199 {
        @JsonProperty("STRINGS")
        List<String> strings;

        @JsonProperty("OTHER_FIELD")
        String otherField;
    }

    static class ModelLong199 {
        @JsonProperty("LONGS")
        List<Long> longs;

        @JsonProperty("OTHER_FIELD")
        String otherField;
    }

    enum ABC { A, B, C };

    static class ModelEnums199 {
        public EnumSet<ABC> enums;
        public String extra;
    }
    
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();

    private final CsvSchema WITH_ARRAY_SCHEMA = MAPPER.schemaWithHeader()
            .withArrayElementSeparator(",")
            .withColumnSeparator(';')
            .withEscapeChar('"');
    
    // [dataformats-text#199]
    public void testReadEmptyStringList() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(ModelString199.class)
                .with(WITH_ARRAY_SCHEMA);
        ModelString199 result;

        // First, non-empty List
        result = r.readValue("STRINGS;OTHER_FIELD\n" + 
                "stuff;other");
        assertNotNull(result);
        assertEquals("other", result.otherField);
        assertEquals(Collections.singletonList("stuff"), result.strings);

        // then empty
        result = r.readValue("STRINGS;OTHER_FIELD\n" + 
                ";Hello");
        assertNotNull(result);
        assertEquals("Hello", result.otherField);
        assertEquals(Collections.emptyList(), result.strings);
    }

    // [dataformats-text#199]
    public void testReadEmptyLongList() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(ModelLong199.class)
                .with(WITH_ARRAY_SCHEMA);
        ModelLong199 result;

        // First, non-empty List
        result = r.readValue("LONGS;OTHER_FIELD\n" + 
                "123;other");
        assertNotNull(result);
        assertEquals("other", result.otherField);
        assertNotNull(result.longs);
        assertEquals(Collections.singletonList(Long.valueOf(123)), result.longs);

        // then empty
        result = r.readValue("LONGS;OTHER_FIELD\n" + 
                ";Hello");
        assertNotNull(result);
        assertEquals("Hello", result.otherField);
        assertNotNull(result.longs);
        assertEquals(Collections.emptyList(), result.longs);
    }

    public void testReadEmptyEnumSet() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(ModelEnums199.class)
                .with(WITH_ARRAY_SCHEMA);
        ModelEnums199 result;

        // First, non-empty List
        result = r.readValue("enums;extra\n" + 
                "B;other");
        assertNotNull(result);
        assertEquals("other", result.extra);
        assertNotNull(result.enums);
        assertEquals(EnumSet.of(ABC.B), result.enums);

        // then empty
        result = r.readValue("enums;extra\n" + 
                ";stuff");
        assertNotNull(result);
        assertEquals("stuff", result.extra);
        assertNotNull(result.enums);
        assertEquals(EnumSet.noneOf(ABC.class), result.enums);
    }
}
