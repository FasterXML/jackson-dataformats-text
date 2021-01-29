package com.fasterxml.jackson.dataformat.csv;

import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public class FeaturesTest extends ModuleTestBase
{
    public void testFactoryFeatures() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvFactory f = mapper.tokenStreamFactory();
        assertFalse(f.canHandleBinaryNatively());
        assertFalse(f.canUseCharArrays());
        assertTrue(f.canUseSchema(CsvSchema.emptySchema()));

        JsonParser p = mapper.createParser("");
        assertFalse(p.canReadObjectId());
        assertFalse(p.canReadTypeId());
        p.close();

        JsonGenerator g = mapper.createGenerator(new StringWriter());
        assertFalse(g.canOmitProperties());
        assertFalse(g.canWriteObjectId());
        assertFalse(g.canWriteTypeId());
        assertTrue(g.canWriteFormattedNumbers());
        g.close();
    }
}
