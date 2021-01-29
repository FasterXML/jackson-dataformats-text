package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestVersions extends ModuleTestBase
{
    private final YAMLMapper MAPPER = newObjectMapper();

    @SuppressWarnings("resource")
    public void testMapperVersions() throws IOException
    {
        // Test shared instance for funsies
        final YAMLMapper mapper = YAMLMapper.shared();

        assertVersion(mapper.tokenStreamFactory());
        JsonParser p = mapper.createParser("123");
        assertVersion(p);
        p.close();
        JsonGenerator gen = mapper.createGenerator(new ByteArrayOutputStream());
        assertVersion(gen);
    }

    @SuppressWarnings("resource")
    public void testDefaults() throws Exception
    {
        YAMLFactory f = MAPPER.tokenStreamFactory();
        assertFalse(f.canHandleBinaryNatively());
        assertFalse(f.canUseCharArrays());

        JsonParser p = MAPPER.createParser(new StringReader(""));
        assertTrue(p.canReadObjectId());
        assertTrue(p.canReadTypeId());
        p.close();

        JsonGenerator g = MAPPER.createGenerator(new StringWriter());
        assertTrue(g.canOmitProperties());
        assertTrue(g.canWriteObjectId());
        assertTrue(g.canWriteTypeId());
        // note: do not try to close it, no content, exception
    }
    
    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */
    
    private void assertVersion(Versioned vers)
    {
        assertEquals(PackageVersion.VERSION, vers.version());
    }
}

