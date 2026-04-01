package tools.jackson.dataformat.yaml.deser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLSchema;

// Tests deserialization of YAML 1.2 core schema.
public class YAML12CoreSchema623Test extends ModuleTestBase {
    private final YAMLFactory FACTORY = YAMLFactory.builder().yamlSchema(YAMLSchema.CORE).build();
    private final YAMLMapper  MAPPER  = YAMLMapper.builder(FACTORY).build();

    @Test
    public void testNull() throws Exception
    {
        Object o = MAPPER.readValue("null", Object.class);
        assertEquals(null, o);

        o = MAPPER.readValue("~", Object.class);
        assertEquals(null, o);
    }

    @Test
    public void testBoolean() throws Exception
    {
        Boolean b = MAPPER.readValue("true", Boolean.class);
        assertEquals(Boolean.TRUE, b);

        b = MAPPER.readValue("false", Boolean.class);
        assertEquals(Boolean.FALSE, b);

        b = MAPPER.readValue("True", Boolean.class);
        assertEquals(Boolean.TRUE, b);

        b = MAPPER.readValue("False", Boolean.class);
        assertEquals(Boolean.FALSE, b);

        b = MAPPER.readValue("TRUE", Boolean.class);
        assertEquals(Boolean.TRUE, b);

        b = MAPPER.readValue("FALSE", Boolean.class);
        assertEquals(Boolean.FALSE, b);
    }

    @Test
    public void testNaN() throws Exception
    {
        Float f = MAPPER.readValue(".nan", Float.class);
        assertEquals(Float.valueOf(Float.NaN), f);

        Double d = MAPPER.readValue(".NaN", Double.class);
        assertEquals(Double.valueOf(Double.NaN), d);

        Number n = MAPPER.readValue(".NAN", Number.class);
        assertEquals(Double.valueOf(Double.NaN), n);
    }

    @Test
    public void testInfinity() throws Exception
    {
        Float f = MAPPER.readValue(".inf", Float.class);
        assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), f);

        Double d = MAPPER.readValue("-.Inf", Double.class);
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), d);

        Number n = MAPPER.readValue("+.INF", Number.class);
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), n);
    }

    @Test
    public void testOctal() throws Exception
    {
        Integer i = MAPPER.readValue("0o10", Integer.class);
        assertEquals(Integer.valueOf(8), i);

        Long l = MAPPER.readValue("0o20", Long.class);
        assertEquals(Long.valueOf(16), l);

        Number n = MAPPER.readValue("0o30", Number.class);
        assertEquals(Integer.valueOf(24), n);
    }

    @Test
    public void testHexadecimal() throws Exception
    {
        Integer i = MAPPER.readValue("0x10", Integer.class);
        assertEquals(Integer.valueOf(16), i);

        Long l = MAPPER.readValue("0x20", Long.class);
        assertEquals(Long.valueOf(32), l);

        Number n = MAPPER.readValue("0x30", Number.class);
        assertEquals(Integer.valueOf(48), n);
    }
}
