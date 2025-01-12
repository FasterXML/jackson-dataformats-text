package tools.jackson.dataformat.javaprop;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestVersions extends ModuleTestBase
{
    @Test
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
