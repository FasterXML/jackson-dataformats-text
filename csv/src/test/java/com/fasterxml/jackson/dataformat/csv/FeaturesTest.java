package com.fasterxml.jackson.dataformat.csv;

import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public class FeaturesTest extends ModuleTestBase
{
    public void testFactoryFeatures() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvFactory f = mapper.getTokenStreamFactory();
        assertFalse(f.canHandleBinaryNatively());
        assertFalse(f.canUseCharArrays());
        assertTrue(f.canUseSchema(CsvSchema.emptySchema()));

        JsonParser p = mapper.createParser("");
        assertFalse(p.canReadObjectId());
        assertFalse(p.canReadTypeId());
        assertTrue(p.canUseSchema(CsvSchema.emptySchema()));
        p.close();

        JsonGenerator g = mapper.createGenerator(new StringWriter());
        assertFalse(g.canOmitFields());
        assertFalse(g.canWriteBinaryNatively());
        assertFalse(g.canWriteObjectId());
        assertFalse(g.canWriteTypeId());
        assertTrue(g.canWriteFormattedNumbers());
        assertTrue(g.canUseSchema(CsvSchema.emptySchema()));
        g.close();
    }
}
