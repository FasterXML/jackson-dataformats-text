package com.fasterxml.jackson.dataformat.csv.ser;

import java.util.Arrays;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

public class SchemaReorderTest extends ModuleTestBase
{
    // should work ok since CsvMapper forces alphabetic ordering as default:
    static class Reordered {
        public int a, b, c, d;
    }
    
    private final CsvMapper MAPPER = new CsvMapper();

    public void testSchemaWithOrdering() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(Reordered.class);
        assertEquals(aposToQuotes("['a','b','c','d']"), schema.getColumnDesc());
        schema = schema.sortedBy("b", "c");
        assertEquals(aposToQuotes("['b','c','a','d']"), schema.getColumnDesc());

        Reordered value = new Reordered();
        value.a = 1;
        value.b = 2;
        value.c = 3;
        value.d = 4;

        schema = schema.withHeader();
        String csv = MAPPER.writer(schema).writeValueAsString(Arrays.asList(value));
        assertEquals("b,c,a,d\n2,3,1,4\n", csv);

//        _verifyLinks(schema);
    }
}
