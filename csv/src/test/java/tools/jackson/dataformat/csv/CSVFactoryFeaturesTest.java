package tools.jackson.dataformat.csv;

import java.io.StringReader;
import java.io.StringWriter;

import tools.jackson.core.*;

public class CSVFactoryFeaturesTest extends ModuleTestBase
{
    public void testFactoryFeatures() throws Exception
    {
        final CsvMapper mapper = mapperForCsv();

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
        g.close();
    }

    public void testFactoryFastFeatures() throws Exception
    {
        CsvFactory f = CsvFactory.builder()
                .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
                .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
                .build();
        assertTrue(f.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        assertTrue(f.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        assertTrue(f.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER));
        final CsvMapper mapper = mapperForCsv(f);
        JsonParser parser = mapper.createParser(new StringReader(""));
        assertTrue(parser.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        assertTrue(parser.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        JsonGenerator generator = mapper.createGenerator(new StringWriter());
        assertTrue(generator.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER));
    }

    public void testFactoryFastBigNumberFeature() throws Exception
    {
        CsvFactory f = CsvFactory.builder()
                .disable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)
                .build();
        assertTrue(f.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        assertFalse(f.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        final CsvMapper mapper = mapperForCsv(f);
        JsonParser parser = mapper.createParser(new StringReader(""));
        assertTrue(parser.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        assertFalse(parser.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
    }

    public void testFactoryBuilderFastFeatures() throws Exception
    {
        CsvFactory f = CsvFactory.builder()
            .disable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
            .disable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .build();
        assertFalse(f.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        assertTrue(f.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        assertFalse(f.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER));

        final CsvMapper mapper = mapperForCsv(f);
        JsonParser parser = mapper.createParser(new StringReader(""));
        assertFalse(parser.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        assertTrue(parser.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        JsonGenerator generator = mapper.createGenerator(new StringWriter());
        assertFalse(generator.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER));
    }

    public void testFactoryBuilderFastBigNumberFeature() throws Exception
    {
        CsvFactory f = CsvFactory.builder()
                .disable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)
                .build();
        assertFalse(f.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        assertTrue(f.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        final CsvMapper mapper = mapperForCsv(f);
        JsonParser parser = mapper.createParser(new StringReader(""));
        assertFalse(parser.isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
        assertTrue(parser.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
    }
}
