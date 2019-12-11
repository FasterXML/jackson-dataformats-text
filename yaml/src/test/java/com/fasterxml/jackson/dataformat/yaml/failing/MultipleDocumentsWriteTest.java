package com.fasterxml.jackson.dataformat.yaml.failing;

import java.io.StringWriter;
import java.util.Collections;

import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

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

        String yaml = w.toString();

        // !!! TODO: actual expected multi-doc contents:
        assertEquals("foo", yaml);
    }

    public void testWriteMultipleDocsLists() throws Exception
    {
        ObjectMapper mapper = newObjectMapper();
        StringWriter w = new StringWriter();
        try (SequenceWriter seqW = mapper.writer().writeValues(w)) {
            seqW.write(Collections.singleton(42));
            seqW.write(Collections.singleton(28));
        }
        w.close();

        String yaml = w.toString();

        // !!! TODO: actual expected multi-doc contents:
        assertEquals("foo", yaml);
    }
}
