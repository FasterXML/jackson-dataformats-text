package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestVersions extends ModuleTestBase
{
    private final YAMLMapper MAPPER = mapperForYAML();

    public void testMapperVersions() throws IOException
    {
        assertVersion(MAPPER);
        assertVersion(MAPPER.getTokenStreamFactory());
        JsonParser p = MAPPER.createParser("123");
        assertVersion(p);
        p.close();
        JsonGenerator gen = MAPPER.createGenerator(new ByteArrayOutputStream());
        assertVersion(gen);
        gen.close();
    }

    public void testDefaults() throws Exception
    {
        YAMLFactory f = MAPPER.getTokenStreamFactory();
        assertFalse(f.canHandleBinaryNatively());
        assertFalse(f.canUseCharArrays());

        JsonParser p = MAPPER.createParser(new StringReader(""));
        assertTrue(p.canReadObjectId());
        assertTrue(p.canReadTypeId());
        p.close();

        JsonGenerator g = MAPPER.createGenerator(new StringWriter());
        assertTrue(g.canOmitFields());
        assertTrue(g.canWriteFormattedNumbers());
        assertTrue(g.canWriteObjectId());
        assertTrue(g.canWriteTypeId());
        assertFalse(g.canWriteBinaryNatively());
        g.close();
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void assertVersion(Versioned vers)
    {
        assertEquals(PackageVersion.VERSION, vers.version());
    }
}

