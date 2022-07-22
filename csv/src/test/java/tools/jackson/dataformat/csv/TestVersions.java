package tools.jackson.dataformat.csv;

import java.io.*;

import tools.jackson.core.*;

import tools.jackson.databind.MapperFeature;

public class TestVersions extends ModuleTestBase
{
    public void testMapperVersions() throws IOException
    {
        // Test shared instance for funsies
        CsvMapper mapper = CsvMapper.shared();
        assertVersion(mapper.tokenStreamFactory());
        JsonParser p = mapper.createParser("abc");
        assertVersion(p);
        p.close();
        JsonGenerator g = mapper.createGenerator(new ByteArrayOutputStream());
        assertVersion(g);
        g.close();
    }

    // Mostly to verify #11
    public void testMapperDefaults()
    {
        CsvMapper mapper = new CsvMapper();
        assertTrue(mapper.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
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

