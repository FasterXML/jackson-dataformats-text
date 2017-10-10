package com.fasterxml.jackson.dataformat.javaprop;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestVersions extends ModuleTestBase
{
    public void testMapperVersions() throws IOException
    {
        ObjectMapper mapper = mapperForProps();
        assertVersion(mapper.tokenStreamFactory());
        JavaPropsParser p = (JavaPropsParser) mapper.createParser("abc=foo");
        assertVersion(p);
        JsonGenerator gen = mapper.createGenerator(new ByteArrayOutputStream());
        assertVersion(gen);
        p.close();
        gen.close();
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void assertVersion(Versioned vers)
    {
        assertNotNull(vers);
        assertEquals(PackageVersion.VERSION, vers.version());
    }
}
