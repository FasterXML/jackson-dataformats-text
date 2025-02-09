package tools.jackson.dataformat.csv.deser;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

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

    static class DeserializationIssue4917 {
        public DecimalHolder4917 decimalHolder;
        public double number;
    }

    static class DecimalHolder4917 {
        public BigDecimal value;

        private DecimalHolder4917(BigDecimal value) {
            this.value = value;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static DecimalHolder4917 of(BigDecimal value) {
            return new DecimalHolder4917(value);
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = newObjectMapper();

    // [databind#2784]
    @Test
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

    @Test
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
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException jme) {
            assertTrue(
                    jme.getMessage().startsWith("Number value length (1200) exceeds the maximum allowed"),
                    "unexpected message: " + jme.getMessage());
        }
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    // [databind#4917]
    @Test
    public void bigDecimal4917() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(DeserializationIssue4917.class).withHeader()
                .withStrictHeaders(true);
        DeserializationIssue4917 issue = MAPPER
                .readerFor(DeserializationIssue4917.class)
                .with(schema)
                .readValue("decimalHolder,number\n100.00,50\n");
        assertEquals(new BigDecimal("100.00"), issue.decimalHolder.value);
        assertEquals(50.0, issue.number);
    }
}
