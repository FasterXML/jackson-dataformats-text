package com.fasterxml.jackson.dataformat.javaprop;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultConfigsTest extends ModuleTestBase
{
    private final String ARTIFACT_ID = "jackson-dataformat-properties";
    
    @Test
    public void testMapperBaseConfig()
    {
        JavaPropsMapper mapper = newPropertiesMapper();
        _verifyVersion(mapper);
        JavaPropsMapper copy = mapper.copy();
        assertNotSame(mapper, copy);
    }

    @Test
    public void testFactoryBaseConfig()
    {
        JavaPropsFactory f = JavaPropsFactory.builder().build();
        _verifyVersion(f);
        JavaPropsFactory copy = f.copy();
        assertNotSame(f, copy);
        assertEquals(JavaPropsFactory.FORMAT_NAME_JAVA_PROPERTIES, f.getFormatName());
        assertFalse(f.requiresCustomCodec());
        assertFalse(f.requiresPropertyOrdering());
        assertFalse(f.canHandleBinaryNatively());
        assertFalse(f.canUseCharArrays());
    }

    @Test
    public void testGeneratorConfig() throws Exception
    {
        JavaPropsFactory f = JavaPropsFactory.builder().build();
        JsonGenerator gen = f.createGenerator(new StringWriter());
        _verifyVersion(gen);
        assertTrue(gen.canOmitFields());
        assertFalse(gen.canWriteBinaryNatively());
        assertTrue(gen.canWriteFormattedNumbers());
        assertFalse(gen.canWriteObjectId());
        assertFalse(gen.canWriteTypeId());
        assertTrue(gen.canUseSchema(JavaPropsSchema.emptySchema()));

        gen.setSchema(JavaPropsSchema.emptySchema());
        assertSame(JavaPropsSchema.emptySchema(), gen.getSchema());
        gen.close();
    }

    @Test
    public void testParserConfig() throws Exception
    {
        JavaPropsFactory f = new JavaPropsFactory();
        JsonParser p = f.createParser("#foo".getBytes("UTF-8"));
        _verifyVersion(p);
        assertFalse(p.canReadObjectId());
        assertFalse(p.canReadTypeId());
        assertFalse(p.hasTextCharacters());
        assertTrue(p.canUseSchema(JavaPropsSchema.emptySchema()));
        assertFalse(p.requiresCustomCodec());

        p.setSchema(JavaPropsSchema.emptySchema());
        assertSame(JavaPropsSchema.emptySchema(), p.getSchema());
        p.close();
    }    

    private void _verifyVersion(Versioned v) {
        Version v2 = v.version();
        assertEquals(ARTIFACT_ID, v2.getArtifactId());
    }

}
