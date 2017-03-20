package com.fasterxml.jackson.dataformat.csv;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.MapperFeature;

public class TestVersions extends ModuleTestBase
{
    public void testMapperVersions() throws IOException
    {
        CsvFactory f = new CsvFactory();
        assertVersion(f);
        CsvParser jp = (CsvParser) f.createParser("abc");
        assertVersion(jp);
        CsvGenerator jgen = f.createGenerator(new ByteArrayOutputStream());
        assertVersion(jgen);
        jp.close();
        jgen.close();
    }

    // Mostly to verify #11
    public void testMapperDefaults()
    {
        CsvMapper mapper = new CsvMapper();
        assertTrue(mapper.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
    }

    // Also, not strictly related to version but...

    public void testMapperCopy() 
    {
        CsvMapper mapper = new CsvMapper();
        CsvMapper copy = mapper.copy();
        // for now, not throwing exception is a happy-enough case
        assertNotNull(copy);
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

