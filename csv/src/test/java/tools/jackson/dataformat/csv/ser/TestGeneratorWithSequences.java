package tools.jackson.dataformat.csv.ser;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGeneratorWithSequences extends ModuleTestBase
{
    @JsonPropertyOrder({"x", "y"})
    protected static class Entry {
        public int x, y;

        public Entry() { }
        public Entry(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    /**
     * Verify that we should be able to just serialize sequences as is
     * because any "array" markers are all but ignored by generator.
     */
    @Test
    public void testAsSequence() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry(1, 2));
        entries.add(new Entry(7, 8));
        CsvSchema schema = mapper.schemaFor(Entry.class)
                .withLineSeparator("\n");
        String csv = mapper.writer(schema)
            .writeValueAsString(entries);
        assertEquals("1,2\n7,8\n", csv);
    }
}
