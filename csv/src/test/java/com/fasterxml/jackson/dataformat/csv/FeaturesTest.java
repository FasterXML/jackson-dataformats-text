package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.StreamWriteFeature;

import java.io.StringReader;
import java.io.StringWriter;

public class FeaturesTest extends ModuleTestBase
{
    public void testFactoryFeatures() throws Exception
    {
        CsvFactory f = new CsvFactory();
        assertFalse(f.canHandleBinaryNatively());
        assertFalse(f.canUseCharArrays());
        assertTrue(f.canUseSchema(CsvSchema.emptySchema()));

        CsvParser p = f.createParser("");
        assertFalse(p.canReadObjectId());
        assertFalse(p.canReadTypeId());
        assertTrue(p.canUseSchema(CsvSchema.emptySchema()));
        p.close();

        CsvGenerator g = f.createGenerator(new StringWriter());
        assertFalse(g.canOmitFields());
        assertFalse(g.canWriteBinaryNatively());
        assertFalse(g.canWriteObjectId());
        assertFalse(g.canWriteTypeId());
        assertTrue(g.canWriteFormattedNumbers());
        assertTrue(g.canUseSchema(CsvSchema.emptySchema()));
        g.close();
    }

    public void testFactoryFastFeatures() throws Exception
    {
        CsvFactory f = new CsvFactory();
        f.enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER.mappedFeature());
        assertTrue(f.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER.mappedFeature()));
        f.enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER.mappedFeature());
        assertTrue(f.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER.mappedFeature()));
        CsvParser parser = f.createParser(new StringReader(""));
        assertTrue(parser.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        CsvGenerator generator = f.createGenerator(new StringWriter());
        assertTrue(generator.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER));
    }

    public void testFactoryBuilderFastFeatures() throws Exception
    {
        CsvFactory f = CsvFactory.builder()
            .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .build();
        assertTrue(f.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER.mappedFeature()));
        assertTrue(f.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER.mappedFeature()));
        CsvParser parser = f.createParser(new StringReader(""));
        assertTrue(parser.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        CsvGenerator generator = f.createGenerator(new StringWriter());
        assertTrue(generator.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER));
    }
}
