package com.fasterxml.jackson.dataformat.yaml.failing;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

// for [dataformat-yaml#26]: not sure if it's an actual bug, but adding for now.
public class CollectionReadTest extends ModuleTestBase
{
    static class SetBean {
        public List<String> sets;
    }
    
    public void testSet26() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final String YAML = "---\n"
                +"sets: !!set\n"
                +"    ? a\n"
                +"    ? b\n";
        SetBean bean = mapper.readValue(YAML, SetBean.class);
        assertNotNull(bean);
        assertNotNull(bean.sets);
        assertEquals(2, bean.sets.size());
        assertEquals("b", bean.sets.get(1));
    }
}
