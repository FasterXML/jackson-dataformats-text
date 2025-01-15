package tools.jackson.dataformat.yaml.tofix;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

// for [dataformat-text#146]: cannot handle underscores in numbers
public class LongWithUnderscoresReadTest extends ModuleTestBase
{
    static class LongHolder
    {
        private Long v;

        public Long getV()
        {
            return v;
        }

        public void setV(Long v)
        {
            this.v = v;
        }
    }

    private final ObjectMapper MAPPER = newObjectMapper();

    @JacksonTestFailureExpected
    @Test
    public void testYamlLongWithUnderscores() throws Exception
    {
        LongHolder longHolder = MAPPER.readValue("v: 1_000_000", LongHolder.class);
        assertNotNull(longHolder);
        assertEquals(LongHolder.class, longHolder.getClass());
        assertEquals(Long.valueOf(1000000), longHolder.getV());
    }

    @JacksonTestFailureExpected
    @Test
    public void testJsonLongWithUnderscores() throws Exception
    {
        LongHolder longHolder = MAPPER.readValue("{\"v\": \"1_000_000\"}", LongHolder.class);
        assertNotNull(longHolder);
        assertEquals(Long.valueOf(1000000), longHolder.getV());
    }
}
