package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestVersions extends ModuleTestBase
{
    @SuppressWarnings("resource")
    public void testMapperVersions() throws IOException
    {
        YAMLFactory f = new YAMLFactory();
        assertVersion(f);
        YAMLParser jp = (YAMLParser) f.createParser("123");
        assertVersion(jp);
        jp.close();
        YAMLGenerator jgen = (YAMLGenerator) f.createGenerator(new ByteArrayOutputStream());
        assertVersion(jgen);
    }

    public void testDefaults() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        assertFalse(f.canHandleBinaryNatively());
        assertFalse(f.canUseCharArrays());

        JsonParser p = f.createParser(new StringReader(""));
        assertTrue(p.canReadObjectId());
        assertTrue(p.canReadTypeId());
        p.close();

        @SuppressWarnings("resource")
        JsonGenerator g = f.createGenerator(new StringWriter());
        assertTrue(g.canOmitFields());
        assertTrue(g.canWriteFormattedNumbers());
        assertTrue(g.canWriteObjectId());
        assertTrue(g.canWriteTypeId());
        assertFalse(g.canWriteBinaryNatively());
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

