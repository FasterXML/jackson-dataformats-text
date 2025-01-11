package com.fasterxml.jackson.dataformat.csv.deser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    @Test
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
    @Test
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

    @Test
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
