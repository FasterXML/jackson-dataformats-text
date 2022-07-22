package tools.jackson.dataformat.csv;

import java.io.StringReader;
import java.io.StringWriter;

import tools.jackson.core.*;

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
        g.close();
    }

    public void testFactoryBuilderFastFeatures() throws Exception
    {
        CsvFactory f = CsvFactory.builder()
            .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .build();
        assertTrue(f.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        assertTrue(f.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER));
        JsonParser parser = f.createParser(ObjectReadContext.empty(), new StringReader(""));
        assertTrue(parser.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        JsonGenerator generator = f.createGenerator(ObjectWriteContext.empty(), new StringWriter());
        assertTrue(generator.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER));
    }
}
