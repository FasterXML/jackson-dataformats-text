package com.fasterxml.jackson.dataformat.csv;

import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.StreamWriteFeature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class CSVFactoryFeaturesTest extends ModuleTestBase
{
    @Test
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

    @Test
    public void testFactoryFastFeatures() throws Exception
    {
        CsvFactory f = new CsvFactory();
        f.enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER.mappedFeature());
        assertTrue(f.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER.mappedFeature()));
        assertFalse(f.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER.mappedFeature()));
        f.enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER.mappedFeature());
        assertTrue(f.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER.mappedFeature()));
        CsvParser parser = f.createParser(new StringReader(""));
        assertTrue(parser.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        assertFalse(parser.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        CsvGenerator generator = f.createGenerator(new StringWriter());
        assertTrue(generator.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER));
    }

    @Test
    public void testFactoryFastBigNumberFeature() throws Exception
    {
        CsvFactory f = new CsvFactory();
        f.enable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER.mappedFeature());
        assertFalse(f.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER.mappedFeature()));
        assertTrue(f.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER.mappedFeature()));
        CsvParser parser = f.createParser(new StringReader(""));
        assertFalse(parser.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        assertTrue(parser.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
    }

    @Test
    public void testFactoryBuilderFastFeatures() throws Exception
    {
        CsvFactory f = CsvFactory.builder()
            .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .build();
        assertTrue(f.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER.mappedFeature()));
        assertFalse(f.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER.mappedFeature()));
        assertTrue(f.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER.mappedFeature()));
        CsvParser parser = f.createParser(new StringReader(""));
        assertTrue(parser.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        assertFalse(parser.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        CsvGenerator generator = f.createGenerator(new StringWriter());
        assertTrue(generator.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER));
    }

    @Test
    public void testFactoryBuilderFastBigNumberFeature() throws Exception
    {
        CsvFactory f = CsvFactory.builder()
                .enable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)
                .build();
        assertTrue(f.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER.mappedFeature()));
        assertFalse(f.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER.mappedFeature()));
        CsvParser parser = f.createParser(new StringReader(""));
        assertTrue(parser.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        assertFalse(parser.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
    }
}
