package tools.jackson.dataformat.csv.deser;

import java.io.FilterReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.SerializedString;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

public class ColumnTypeReadTest extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    private final CsvSchema SCHEMA = CsvSchema.builder()
            .addColumn("name")
            .addColumn("count", CsvSchema.ColumnType.NUMBER)
            .addColumn("ratio", CsvSchema.ColumnType.NUMBER)
            .addColumn("active", CsvSchema.ColumnType.BOOLEAN)
            .addColumn("note", CsvSchema.ColumnType.STRING_OR_LITERAL)
            .build();

    @Test
    public void testSchemaTypesInParserTokensAndAccessors() throws Exception
    {
        try (JsonParser p = MAPPER.reader(SCHEMA).createParser("item,25,1.5,true,false\n")) {
            assertEquals(JsonToken.START_OBJECT, p.nextToken());
            assertEquals("name", p.nextName());
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("item", p.getString());
            assertTrue(p.nextName(new SerializedString("count")));
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(25, p.getIntValue());
            assertEquals("ratio", p.nextName());
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(1.5, p.getDoubleValue());
            assertEquals("active", p.nextName());
            assertEquals(JsonToken.VALUE_TRUE, p.nextToken());
            assertEquals("note", p.nextName());
            assertEquals(JsonToken.VALUE_FALSE, p.nextToken());
        }
    }

    @Test
    public void testTypedValuesForMapAndTree() throws Exception
    {
        Map<?, ?> row = MAPPER.readerFor(Map.class).with(SCHEMA)
                .readValue("item,25,1.5,true,false\n");
        assertEquals("item", row.get("name"));
        assertEquals(25, row.get("count"));
        assertEquals(1.5, ((Number) row.get("ratio")).doubleValue());
        assertEquals(true, row.get("active"));
        assertEquals(false, row.get("note"));

        JsonNode tree = MAPPER.readerFor(JsonNode.class).with(SCHEMA)
                .readValue("item,25,1.5,true,false\n");
        assertTrue(tree.get("count").isNumber());
        assertEquals(25, tree.get("count").intValue());
        assertTrue(tree.get("ratio").isNumber());
        assertEquals(1.5, tree.get("ratio").doubleValue());
        assertTrue(tree.get("active").isBoolean());
        assertTrue(tree.get("note").isBoolean());
    }

    @Test
    public void testTypedNullsAndPermissiveFallback() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("number", CsvSchema.ColumnType.NUMBER)
                .addColumn("either", CsvSchema.ColumnType.NUMBER_OR_STRING)
                .addColumn("literal", CsvSchema.ColumnType.STRING_OR_LITERAL)
                .addColumn("flag", CsvSchema.ColumnType.BOOLEAN)
                .build();
        Map<?, ?> row = MAPPER.readerFor(Map.class).with(schema)
                .readValue(",not-a-number,null,\n");
        assertNull(row.get("number"));
        assertEquals("not-a-number", row.get("either"));
        assertNull(row.get("literal"));
        assertNull(row.get("flag"));
    }

    @Test
    public void testNumberOrStringAndDefaultStringTypes() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("untyped")
                .addColumn("either", CsvSchema.ColumnType.NUMBER_OR_STRING)
                .addColumn("literal", CsvSchema.ColumnType.NUMBER_OR_STRING)
                .addColumn("leadingZero", CsvSchema.ColumnType.NUMBER_OR_STRING)
                .build();
        Map<?, ?> row = MAPPER.readerFor(Map.class).with(schema)
                .readValue("25,1e2,true,012\n");
        assertEquals("25", row.get("untyped"));
        assertEquals(100.0, ((Number) row.get("either")).doubleValue());
        assertEquals(true, row.get("literal"));
        assertEquals("012", row.get("leadingZero"));
    }

    @Test
    public void testTrimmedNumericBufferAndBigValues() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addNumberColumn("small")
                .addNumberColumn("large")
                .addNumberColumn("precise")
                .build();
        String large = "123456789012345678901234567890";
        String precise = "0.10000000000000000001";
        try (JsonParser p = MAPPER.reader(schema).createParser("\" 25 \"," + large + "," + precise + "\n")) {
            assertEquals(JsonToken.START_OBJECT, p.nextToken());
            assertEquals("small", p.nextName());
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals("25", p.getString());
            assertEquals(2, p.getStringLength());
            assertEquals(JsonParser.NumberType.INT, p.getNumberType());
            assertEquals(25, p.getIntValue());
            assertEquals("large", p.nextName());
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());
            assertEquals(new BigInteger(large), p.getBigIntegerValue());
            assertEquals("precise", p.nextName());
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(new BigDecimal(precise), p.getDecimalValue());
        }
    }

    @Test
    public void testNumericValuesAcrossShortReaderChunks() throws Exception
    {
        CsvSchema schema = CsvSchema.builder().addNumberColumn("value").build();
        String digits = "1" + "0".repeat(299);
        FilterReader shortReader = new FilterReader(new StringReader(digits + "\n")) {
            @Override
            public int read(char[] buffer, int offset, int length) throws java.io.IOException {
                return super.read(buffer, offset, Math.min(length, 3));
            }
        };
        try (JsonParser p = MAPPER.reader(schema).createParser(shortReader)) {
            assertEquals(JsonToken.START_OBJECT, p.nextToken());
            assertEquals("value", p.nextName());
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(new BigInteger(digits), p.getBigIntegerValue());
        }
    }

    @Test
    public void testArrayColumnElementsRemainStrings() throws Exception
    {
        CsvSchema schema = CsvSchema.builder().addArrayColumn("items", "|").build();
        try (JsonParser p = MAPPER.reader(schema).createParser("1|true|null\n")) {
            assertEquals(JsonToken.START_OBJECT, p.nextToken());
            assertEquals("items", p.nextName());
            assertEquals(JsonToken.START_ARRAY, p.nextToken());
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("1", p.getString());
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("true", p.getString());
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("null", p.getString());
            assertEquals(JsonToken.END_ARRAY, p.nextToken());
        }
    }

    @Test
    public void testNumberOrStringUsesJsonNumberSyntax() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("value", CsvSchema.ColumnType.NUMBER_OR_STRING).build();
        for (String value : new String[] {"0", "-0", "-25", "0.25", "1e-2", "-2E+3"}) {
            JsonNode row = MAPPER.readerFor(JsonNode.class).with(schema).readValue(value + "\n");
            assertTrue(row.get("value").isNumber(), value);
        }
        for (String value : new String[] {"012", "+1", ".5", "1.", "1e", "1e+", "-", "trueish"}) {
            JsonNode row = MAPPER.readerFor(JsonNode.class).with(schema).readValue(value + "\n");
            assertTrue(row.get("value").isString(), value);
            assertEquals(value, row.get("value").stringValue());
        }
    }

    @Test
    public void testTypedHeaderRowsAndConfiguredNull() throws Exception
    {
        CsvSchema schema = CsvSchema.builder().addNumberColumn("count")
                .addBooleanColumn("flag").addColumn("note").build()
                .withHeader().withNullValue("NA");
        try (MappingIterator<Map<String, Object>> rows = MAPPER.readerFor(Map.class).with(schema)
                .readValues("count,flag,note\n25,true,25\nNA,false,null\n")) {
            Map<String, Object> first = rows.next();
            assertEquals(25, first.get("count"));
            assertEquals(true, first.get("flag"));
            assertEquals("25", first.get("note"));
            Map<String, Object> second = rows.next();
            assertNull(second.get("count"));
            assertEquals(false, second.get("flag"));
            assertEquals("null", second.get("note"));
            assertFalse(rows.hasNext());
        }
    }

    @Test
    public void testInvalidStrictTypedValuesFail() throws Exception
    {
        CsvSchema schema = CsvSchema.builder().addColumn("flag", CsvSchema.ColumnType.BOOLEAN).build();
        try (JsonParser p = MAPPER.reader(schema).createParser("perhaps\n")) {
            assertEquals(JsonToken.START_OBJECT, p.nextToken());
            assertEquals("flag", p.nextName());
            assertThrows(StreamReadException.class, p::nextToken);
        }
        schema = CsvSchema.builder().addNumberColumn("amount").build();
        try (JsonParser p = MAPPER.reader(schema).createParser("12x\n")) {
            assertEquals(JsonToken.START_OBJECT, p.nextToken());
            assertEquals("amount", p.nextName());
            assertThrows(StreamReadException.class, p::nextToken);
        }
    }
}
