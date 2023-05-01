package tools.jackson.dataformat.csv.deser;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.databind.DatabindException;
import tools.jackson.dataformat.csv.CsvFactory;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.ModuleTestBase;

// Tests copied from databind "JDKNumberDeserTest" (only a small subset)
public class NumberDeserWithCSVTest extends ModuleTestBase
{
    // [databind#2784]
    static class BigDecimalHolder2784 {
        public BigDecimal value;
    }

    static class DoubleHolder2784 {
        public Double value;
    }

    static class FloatHolder2784 {
        public Float value;
    }

    static class NestedBigDecimalHolder2784 {
        @JsonUnwrapped
        public BigDecimalHolder2784 holder;
    }

    static class NestedDoubleHolder2784 {
        @JsonUnwrapped
        public DoubleHolder2784 holder;
    }

    static class NestedFloatHolder2784 {
        @JsonUnwrapped
        public FloatHolder2784 holder;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = newObjectMapper();

    // [databind#2784]
    public void testBigDecimalUnwrapped() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(NestedBigDecimalHolder2784.class).withHeader()
                .withStrictHeaders(true);
        final String DOC = "value\n5.123\n";
        NestedBigDecimalHolder2784 result = MAPPER.readerFor(NestedBigDecimalHolder2784.class)
                .with(schema)
                .readValue(DOC);
        assertEquals(new BigDecimal("5.123"), result.holder.value);
    }

    public void testVeryBigDecimalUnwrapped() throws Exception
    {
        final int len = 1200;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(1);
        }
        final String value = sb.toString();
        CsvSchema schema = MAPPER.schemaFor(NestedBigDecimalHolder2784.class).withHeader()
                .withStrictHeaders(true);
        final String DOC = "value\n" + value + "\n";
        try {
            MAPPER.readerFor(NestedBigDecimalHolder2784.class)
                    .with(schema)
                    .readValue(DOC);
            fail("expected JsonMappingException");
        } catch (DatabindException jme) {
            assertTrue("unexpected message: " + jme.getMessage(),
                    jme.getMessage().startsWith("Number value length (1200) exceeds the maximum allowed"));
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
        CsvFactory factory = CsvFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
                .build();
        CsvMapper mapper = CsvMapper.builder(factory).build();
        CsvSchema schema = mapper.schemaFor(NestedBigDecimalHolder2784.class).withHeader()
                .withStrictHeaders(true);
        final String DOC = "value\n" + value + "\n";
        NestedBigDecimalHolder2784 result = mapper.readerFor(NestedBigDecimalHolder2784.class)
                .with(schema)
                .readValue(DOC);
        assertEquals(new BigDecimal(value), result.holder.value);
    }

    public void testDoubleUnwrapped() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(NestedDoubleHolder2784.class).withHeader()
                .withStrictHeaders(true);
        final String DOC = "value\n125.123456789\n";
        NestedDoubleHolder2784 result = MAPPER.readerFor(NestedDoubleHolder2784.class)
                .with(schema)
                .readValue(DOC);
        assertEquals(Double.parseDouble("125.123456789"), result.holder.value);
    }

    public void testFloatUnwrapped() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(NestedFloatHolder2784.class).withHeader()
                .withStrictHeaders(true);
        final String DOC = "value\n125.123\n";
        NestedFloatHolder2784 result = MAPPER.readerFor(NestedFloatHolder2784.class)
                .with(schema)
                .readValue(DOC);
        assertEquals(Float.parseFloat("125.123"), result.holder.value);
    }

    public void testFloatEdgeCase() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(NestedFloatHolder2784.class).withHeader()
                .withStrictHeaders(true);
        final String DOC = "value\n1.199999988079071\n";
        NestedFloatHolder2784 result = MAPPER.readerFor(NestedFloatHolder2784.class)
                .with(schema)
                .readValue(DOC);
        assertEquals(Float.parseFloat("1.199999988079071"), result.holder.value);
    }
}
