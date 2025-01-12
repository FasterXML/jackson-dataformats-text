package tools.jackson.dataformat.csv.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    @Test
    public void testStrictQuoting() throws Exception
    {
        final String NUMS = "12345 6789";
        final String LONG = NUMS + NUMS + NUMS + NUMS; // 40 chars should do it
        
        CsvMapper mapper = mapperForCsv();

        assertFalse(mapper.tokenStreamFactory().isEnabled(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING));
        CsvSchema schema = mapper.schemaFor(AB.class).withoutHeader();

        final AB input = new AB("x", LONG);
        
        // with non-strict, should quote
        String csv = mapper.writer(schema).writeValueAsString(input);
        assertEquals(a2q("x,'"+LONG+"'"), csv.trim());

        // should be possible to hot-swap
        // and with strict/optimal, no quoting
        csv = mapper.writer(schema)
                .with(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(input);
        assertEquals(a2q("x,"+LONG), csv.trim());
    }
}
