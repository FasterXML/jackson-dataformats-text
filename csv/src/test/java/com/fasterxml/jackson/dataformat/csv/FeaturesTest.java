package com.fasterxml.jackson.dataformat.csv;

import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public class FeaturesTest extends ModuleTestBase
{
    public void testFactoryFeatures() throws Exception
    {
        CsvFactory f = new CsvFactory();
        assertFalse(f.canHandleBinaryNatively());
        assertFalse(f.canUseCharArrays());
        assertTrue(f.canUseSchema(CsvSchema.emptySchema()));

        JsonParser p = f.createParser("");
        assertFalse(p.canReadObjectId());
        assertFalse(p.canReadTypeId());
        assertTrue(p.canUseSchema(CsvSchema.emptySchema()));
        p.close();

        JsonGenerator g = f.createGenerator(new StringWriter());
        assertFalse(g.canOmitFields());
        assertFalse(g.canWriteBinaryNatively());
        assertFalse(g.canWriteObjectId());
        assertFalse(g.canWriteTypeId());
        assertTrue(g.canWriteFormattedNumbers());
        assertTrue(g.canUseSchema(CsvSchema.emptySchema()));
        g.close();
    }
}
