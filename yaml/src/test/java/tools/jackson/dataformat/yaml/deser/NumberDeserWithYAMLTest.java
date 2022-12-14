package tools.jackson.dataformat.yaml.deser;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

// Tests copied from databind "JDKNumberDeserTest": may need more clean up
// work remove useless ones, make more YAML-y (YAML parsers can typically
// parse most JSON content, but such content is not very common)
public class NumberDeserWithYAMLTest extends ModuleTestBase
{
    // [databind#2644]
    static class NodeRoot2644 {
        public String type;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes(value = {
                @JsonSubTypes.Type(value = NodeParent2644.class, name = "NodeParent")
        })
        public Node2644 node;
    }

    public static class NodeParent2644 extends Node2644 { }

    public static abstract class Node2644 {
        @JsonProperty("amount")
        BigDecimal val;

        public BigDecimal getVal() {
            return val;
        }

        public void setVal(BigDecimal val) {
            this.val = val;
        }
    }

    // [databind#2784]
    static class BigDecimalHolder2784 {
        public BigDecimal value;
    }

    static class NestedBigDecimalHolder2784 {
        @JsonUnwrapped
        public BigDecimalHolder2784 holder;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final YAMLMapper MAPPER = newObjectMapper();

    public void testNaN() throws Exception
    {
        Float result = MAPPER.readValue(" \"NaN\"", Float.class);
        assertEquals(Float.valueOf(Float.NaN), result);

        Double d = MAPPER.readValue(" \"NaN\"", Double.class);
        assertEquals(Double.valueOf(Double.NaN), d);

        Number num = MAPPER.readValue(" \"NaN\"", Number.class);
        assertEquals(Double.valueOf(Double.NaN), num);
    }

    public void testDoubleInf() throws Exception
    {
        Double result = MAPPER.readValue(" \""+Double.POSITIVE_INFINITY+"\"", Double.class);
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), result);

        result = MAPPER.readValue(" \""+Double.NEGATIVE_INFINITY+"\"", Double.class);
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), result);
    }

    // 01-Mar-2017, tatu: This is bit tricky... in some ways, mapping to "empty value"
    //    would be best; but due to legacy reasons becomes `null` at this point
    public void testEmptyAsNumber() throws Exception
    {
        assertNull(MAPPER.readValue(quote(""), Byte.class));
        assertNull(MAPPER.readValue(quote(""), Short.class));
        assertNull(MAPPER.readValue(quote(""), Character.class));
        assertNull(MAPPER.readValue(quote(""), Integer.class));
        assertNull(MAPPER.readValue(quote(""), Long.class));
        assertNull(MAPPER.readValue(quote(""), Float.class));
        assertNull(MAPPER.readValue(quote(""), Double.class));

        assertNull(MAPPER.readValue(quote(""), BigInteger.class));
        assertNull(MAPPER.readValue(quote(""), BigDecimal.class));
    }

    public void testTextualNullAsNumber() throws Exception
    {
        final String NULL_JSON = quote("null");
        assertNull(MAPPER.readValue(NULL_JSON, Byte.class));
        assertNull(MAPPER.readValue(NULL_JSON, Short.class));
        // Character is bit special, can't do:
//        assertNull(MAPPER.readValue(JSON, Character.class));
        assertNull(MAPPER.readValue(NULL_JSON, Integer.class));
        assertNull(MAPPER.readValue(NULL_JSON, Long.class));
        assertNull(MAPPER.readValue(NULL_JSON, Float.class));
        assertNull(MAPPER.readValue(NULL_JSON, Double.class));

        assertEquals(Byte.valueOf((byte) 0), MAPPER.readValue(NULL_JSON, Byte.TYPE));
        assertEquals(Short.valueOf((short) 0), MAPPER.readValue(NULL_JSON, Short.TYPE));

        //        assertEquals(Character.valueOf((char) 0), MAPPER.readValue(JSON, Character.TYPE));
        assertEquals(Integer.valueOf(0), MAPPER.readValue(NULL_JSON, Integer.TYPE));
        assertEquals(Long.valueOf(0L), MAPPER.readValue(NULL_JSON, Long.TYPE));
        assertEquals(Float.valueOf(0f), MAPPER.readValue(NULL_JSON, Float.TYPE));
        assertEquals(Double.valueOf(0d), MAPPER.readValue(NULL_JSON, Double.TYPE));
        
        assertNull(MAPPER.readValue(NULL_JSON, BigInteger.class));
        assertNull(MAPPER.readValue(NULL_JSON, BigDecimal.class));

        // Also: verify failure for at least some
        try {
            MAPPER.readerFor(Integer.TYPE).with(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .readValue(NULL_JSON);
            fail("Should not have passed");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce String \"null\"");
        }

        ObjectMapper noCoerceMapper = mapperBuilder()
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .build();
        try {
            noCoerceMapper.readValue(NULL_JSON, Integer.TYPE);
            fail("Should not have passed");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce String value");
        }
    }

    // [databind#852]
    public void testScientificNotationAsStringForNumber() throws Exception
    {
        Object ob = MAPPER.readValue("\"3E-8\"", Number.class);
        assertEquals(Double.class, ob.getClass());
        ob = MAPPER.readValue("\"3e-8\"", Number.class);
        assertEquals(Double.class, ob.getClass());
        ob = MAPPER.readValue("\"300000000\"", Number.class);
        assertEquals(Integer.class, ob.getClass());
        ob = MAPPER.readValue("\"123456789012\"", Number.class);
        assertEquals(Long.class, ob.getClass());
    }

    public void testIntAsNumber() throws Exception
    {
        // Even if declared as 'generic' type, should return using most
        // efficient type... here, Integer
        Number result = MAPPER.readValue(" 123 ", Number.class);
        assertEquals(Integer.valueOf(123), result);
    }

    public void testLongAsNumber() throws Exception
    {
        // And beyond int range, should get long
        long exp = 1234567890123L;
        Number result = MAPPER.readValue(String.valueOf(exp), Number.class);
        assertEquals(Long.valueOf(exp), result);
    }

    public void testBigIntAsNumber() throws Exception
    {
        // and after long, BigInteger
        BigInteger biggie = new BigInteger("1234567890123456789012345678901234567890");
        Number result = MAPPER.readValue(biggie.toString(), Number.class);
        assertEquals(BigInteger.class, biggie.getClass());
        assertEquals(biggie, result);
    }

    public void testIntTypeOverride() throws Exception
    {
        // Slight twist; as per [JACKSON-100], can also request binding
        // to BigInteger even if value would fit in Integer
        ObjectReader r = MAPPER.reader(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

        BigInteger exp = BigInteger.valueOf(123L);

        // first test as any Number
        Number result = r.forType(Number.class).readValue(" 123 ");
        assertEquals(BigInteger.class, result.getClass());
        assertEquals(exp, result);

        // then as any Object
        /*Object value =*/ r.forType(Object.class).readValue("123");
        assertEquals(BigInteger.class, result.getClass());
        assertEquals(exp, result);

        // and as JsonNode
        JsonNode node = r.readTree("  123");
        assertTrue(node.isBigInteger());
        assertEquals(123, node.asInt());
    }

    public void testDoubleAsNumber() throws Exception
    {
        Number result = MAPPER.readValue(new StringReader(" 1.0 "), Number.class);
        assertEquals(Double.valueOf(1.0), result);
    }

    public void testFpTypeOverrideSimple() throws Exception
    {
        ObjectReader r = MAPPER.reader(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        BigDecimal dec = new BigDecimal("0.1");

        // First test generic stand-alone Number
        Number result = r.forType(Number.class).readValue(dec.toString());
        assertEquals(BigDecimal.class, result.getClass());
        assertEquals(dec, result);

        // Then plain old Object
        Object value = r.forType(Object.class).readValue(dec.toString());
        assertEquals(BigDecimal.class, result.getClass());
        assertEquals(dec, value);

        JsonNode node = r.readTree(dec.toString());
        assertTrue(node.isBigDecimal());
        assertEquals(dec.doubleValue(), node.asDouble());
    }

    public void testFpTypeOverrideStructured() throws Exception
    {
        ObjectReader r = MAPPER.reader(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        BigDecimal dec = new BigDecimal("-19.37");
        // List element types
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) r.forType(List.class).readValue("[ "+dec.toString()+" ]");
        assertEquals(1, list.size());
        Object val = list.get(0);
        assertEquals(BigDecimal.class, val.getClass());
        assertEquals(dec, val);

        // and a map
        Map<?,?> map = r.forType(Map.class).readValue("a: "+dec.toString()+"\n");
        assertEquals(1, map.size());
        val = map.get("a");
        assertEquals(BigDecimal.class, val.getClass());
        assertEquals(dec, val);
    }

    // [databind#504]
    public void testForceIntsToLongs() throws Exception
    {
        ObjectReader r = MAPPER.reader(DeserializationFeature.USE_LONG_FOR_INTS);

        Object ob = r.forType(Object.class).readValue("42");
        assertEquals(Long.class, ob.getClass());
        assertEquals(Long.valueOf(42L), ob);

        Number n = r.forType(Number.class).readValue("42");
        assertEquals(Long.class, n.getClass());
        assertEquals(Long.valueOf(42L), n);

        // and one more: should get proper node as well
        JsonNode node = r.readTree("42");
        if (!node.isLong()) {
            fail("Expected LongNode, got: "+node.getClass().getName());
        }
        assertEquals(42, node.asInt());
    }

    // [databind#2644]
    public void testBigDecimalSubtypes() throws Exception
    {
        ObjectMapper mapper = mapperBuilder()
                .registerSubtypes(NodeParent2644.class)
                .build();
        NodeRoot2644 root = mapper.readValue(
                "type: \"NodeParent\"\n"
                +"node:\n"
                +"  amount: 9999999999999999.99\n",
                NodeRoot2644.class
        );

        assertEquals(new BigDecimal("9999999999999999.99"), root.node.getVal());
    }

    // [databind#2784]
    public void testBigDecimalUnwrapped() throws Exception
    {
        // mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        final String DOC = "value: 5.00\n";
        NestedBigDecimalHolder2784 result = MAPPER.readValue(DOC, NestedBigDecimalHolder2784.class);
        assertEquals(new BigDecimal("5.00"), result.holder.value);
    }

    public void testVeryBigDecimalUnwrapped() throws Exception
    {
        final int len = 1200;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(1);
        }
        final String value = sb.toString();
        final String DOC = "value: " + value + "\n";
        try {
            MAPPER.readValue(DOC, NestedBigDecimalHolder2784.class);
            fail("expected DatabindException");
        } catch (DatabindException jme) {
            assertTrue("unexpected message: " + jme.getMessage(),
                    jme.getMessage().startsWith("Number length (1200) exceeds the maximum length (1000)"));
        }
    }

    public void testVeryBigDecimalUnwrappedWithNumLenUnlimited() throws Exception
    {
        final int len = 1200;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(1);
        }
        final String value = sb.toString();
        final String DOC = "value: " + value + "\n";
        YAMLFactory factory = streamFactoryBuilder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
                .build();
        NestedBigDecimalHolder2784 result = mapperBuilder(factory).build()
                .readValue(DOC, NestedBigDecimalHolder2784.class);
        assertEquals(new BigDecimal(value), result.holder.value);
    }
}
