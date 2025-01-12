package com.fasterxml.jackson.dataformat.javaprop;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Versioned;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestVersions extends ModuleTestBase
{
    @Test
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
