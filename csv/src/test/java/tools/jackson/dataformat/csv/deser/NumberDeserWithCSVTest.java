package tools.jackson.dataformat.csv.deser;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

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
