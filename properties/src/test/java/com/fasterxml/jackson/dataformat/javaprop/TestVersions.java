package com.fasterxml.jackson.dataformat.javaprop;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestVersions extends ModuleTestBase
{
    public void testMapperVersions() throws IOException
    {
        JavaPropsFactory f = new JavaPropsFactory();
        assertVersion(f);
        JavaPropsParser p = (JavaPropsParser) f.createParser("abc=foo");
        assertVersion(p);
        JsonGenerator gen = f.createGenerator(new ByteArrayOutputStream());
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
