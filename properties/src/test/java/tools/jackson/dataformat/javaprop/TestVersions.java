package tools.jackson.dataformat.javaprop;

import java.io.*;

import tools.jackson.core.*;

import tools.jackson.databind.ObjectMapper;

public class TestVersions extends ModuleTestBase
{
    public void testMapperVersions() throws IOException
    {
        // Test shared instance for funsies
        ObjectMapper mapper = JavaPropsMapper.shared();
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
