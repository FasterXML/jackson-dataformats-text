package com.fasterxml.jackson.dataformat.javaprop;

import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectReadContext;
import com.fasterxml.jackson.core.ObjectWriteContext;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;

public class DefaultConfigsTest extends ModuleTestBase
{
    private final String ARTIFACT_ID = "jackson-dataformat-properties";

    public void testFactoryBaseConfig()
    {
        JavaPropsFactory f = new JavaPropsFactory();
        _verifyVersion(f);
        JavaPropsFactory copy = f.copy();
        assertNotSame(f, copy);
        assertEquals(JavaPropsFactory.FORMAT_NAME_JAVA_PROPERTIES, f.getFormatName());
        assertFalse(f.requiresPropertyOrdering());
        assertFalse(f.canHandleBinaryNatively());
        assertFalse(f.canUseCharArrays());
    }

    public void testGeneratorConfig() throws Exception
    {
        JavaPropsFactory f = new JavaPropsFactory();
        JsonGenerator gen = f.createGenerator(ObjectWriteContext.empty(), new StringWriter());
        _verifyVersion(gen);
        assertTrue(gen.canOmitProperties());
        assertFalse(gen.canWriteObjectId());
        assertFalse(gen.canWriteTypeId());

        gen.close();
    }

    public void testParserConfig() throws Exception
    {
        JavaPropsFactory f = new JavaPropsFactory();
        JsonParser p = f.createParser(ObjectReadContext.empty(), "#foo");
        _verifyVersion(p);
        assertFalse(p.canReadObjectId());
        assertFalse(p.canReadTypeId());
        assertFalse(p.hasTextCharacters());

        p.close();
    }

    private void _verifyVersion(Versioned v) {
        Version v2 = v.version();
        assertEquals(ARTIFACT_ID, v2.getArtifactId());
    }
}
