package tools.jackson.dataformat.yaml.tofix;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

// for [dataformat-text#146]: cannot handle underscores in numbers
public class IntegerWithUnderscoresReadTest extends ModuleTestBase {
    static class IntegerHolder
    {
        private Integer v;

        public Integer getV()
        {
            return v;
        }

        public void setV(Integer v)
        {
            this.v = v;
        }
    }

    private final ObjectMapper MAPPER = newObjectMapper();

    @JacksonTestFailureExpected
    @Test
    public void testJsonIntegerWithUnderscores() throws Exception
    {
        IntegerHolder integerHolder = MAPPER.readValue("{\"v\": \"1_000_000\"}", IntegerHolder.class);
        assertNotNull(integerHolder);
        assertEquals(Integer.valueOf(1000000), integerHolder.getV());
    }
}
