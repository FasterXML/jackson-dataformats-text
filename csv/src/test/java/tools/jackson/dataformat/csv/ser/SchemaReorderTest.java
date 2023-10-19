package tools.jackson.dataformat.csv.ser;

import java.util.Arrays;

import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.ModuleTestBase;

public class SchemaReorderTest extends ModuleTestBase
{
    // should work ok since CsvMapper forces alphabetic ordering as default:
    static class Reordered {
        public int a;
        public long b;
        public long c;
        public int d;
    }

    private final CsvMapper MAPPER = new CsvMapper();

    public void testSchemaWithOrdering() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(Reordered.class);
        assertEquals(a2q("['a','b','c','d']"), schema.getColumnDesc());
        schema = schema.sortedBy("b", "c");
        assertEquals(a2q("['b','c','a','d']"), schema.getColumnDesc());

        Reordered value = new Reordered();
        value.a = 1;
        value.b = Long.MIN_VALUE;
        value.c = Long.MAX_VALUE;
        value.d = 4;

        schema = schema.withHeader();
        String csv = MAPPER.writer(schema).writeValueAsString(Arrays.asList(value));
        assertEquals("b,c,a,d\n"+Long.MIN_VALUE+","+Long.MAX_VALUE+",1,4\n", csv);

//        _verifyLinks(schema);
    }
}
