package tools.jackson.dataformat.javaprop;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.ObjectMapper;

// Tests copied from databind "JDKNumberDeserTest" (only a small subset)
public class NumberDeserWithPropsTest extends ModuleTestBase
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

    private final JavaPropsMapper MAPPER = newPropertiesMapper();

    // [databind#2644]
    public void testBigDecimalSubtypes() throws Exception
    {
        ObjectMapper mapper = propertiesMapperBuilder()
                .registerSubtypes(NodeParent2644.class)
                .build();
        NodeRoot2644 root = mapper.readValue(
                "type: NodeParent\n"
                +"node.amount: 9999999999999999.99\n",
                NodeRoot2644.class
        );

        assertEquals(new BigDecimal("9999999999999999.99"), root.node.getVal());
    }

    // [databind#2784]
    public void testBigDecimalUnwrapped() throws Exception
    {
        // mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        final String DOC = "value: 5.00";
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
        final String DOC = "value: " + value;
        try {
            MAPPER.readValue(DOC, NestedBigDecimalHolder2784.class);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertTrue("unexpected message: " + e.getMessage(),
                    e.getMessage().startsWith("Number value length (1200) exceeds the maximum allowed"));
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
        final String DOC = "value: " + value;
        JavaPropsFactory factory = JavaPropsFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
                .build();
        JavaPropsMapper mapper = propertiesMapperBuilder(factory).build();
        NestedBigDecimalHolder2784 result = mapper.readValue(DOC, NestedBigDecimalHolder2784.class);
        assertEquals(new BigDecimal(value), result.holder.value);
    }
}
