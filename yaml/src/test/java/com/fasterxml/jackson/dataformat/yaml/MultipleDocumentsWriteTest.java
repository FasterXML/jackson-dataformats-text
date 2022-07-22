package com.fasterxml.jackson.dataformat.yaml;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;

import tools.jackson.databind.*;

// for [dataformats-text#163]
public class MultipleDocumentsWriteTest extends ModuleTestBase
{
    static class POJO163 {
        public int value;

        public POJO163(int v) { value = v; }
    }
    
    public void testWriteMultipleDocsBeans() throws Exception
    {
        ObjectMapper mapper = newObjectMapper();
        StringWriter w = new StringWriter();
        try (SequenceWriter seqW = mapper.writer().writeValues(w)) {
            seqW.write(new POJO163(42));
            seqW.write(new POJO163(28));
        }
        w.close();

        String yaml = w.toString().trim();
        assertEquals("---\nvalue: 42\n---\nvalue: 28", yaml);
    }

    public void testWriteMultipleDocsLists() throws Exception
    {
        ObjectMapper mapper = newObjectMapper();
        StringWriter w = new StringWriter();
        try (SequenceWriter seqW = mapper.writer().writeValues(w)) {
            seqW.write(Arrays.asList(28,12));
            seqW.write(Collections.singleton(28));
        }
        w.close();

        String yaml = w.toString().trim();
        assertEquals("---\n- 28\n- 12\n---\n- 28", yaml);
    }
}
