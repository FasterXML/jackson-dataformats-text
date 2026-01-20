package tools.jackson.dataformat.csv.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectReader;
import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@code CsvReadFeature.NULL_VALUE_UNQUOTED_AS_NULL}
 * (see [dataformats-text#601])
 */
public class NullValueUnquotedAsNullTest extends ModuleTestBase
{
    @JsonPropertyOrder({"firstName", "middleName", "lastName"})
    static class TestUser {
        public String firstName, middleName, lastName;
    }

    private final CsvMapper MAPPER = mapperForCsv();

    // Default behavior: both quoted and unquoted null values become null
    @Test
    public void testDefaultBothQuotedAndUnquotedAsNull() throws Exception {
        CsvSchema schema = MAPPER.schemaFor(TestUser.class).withNullValue("N/A");
        ObjectReader reader = MAPPER.readerFor(TestUser.class).with(schema);

        // Unquoted N/A -> null
        TestUser user1 = reader.readValue("Grace,N/A,Hopper");
        assertEquals("Grace", user1.firstName);
        assertNull(user1.middleName);
        assertEquals("Hopper", user1.lastName);

        // Quoted "N/A" -> also null (default behavior)
        TestUser user2 = reader.readValue("Grace,\"N/A\",Hopper");
        assertEquals("Grace", user2.firstName);
        assertNull(user2.middleName);
        assertEquals("Hopper", user2.lastName);
    }

    // With feature enabled: only unquoted null values become null
    @Test
    public void testUnquotedNullValueAsNull() throws Exception {
        CsvSchema schema = MAPPER.schemaFor(TestUser.class).withNullValue("N/A");
        ObjectReader reader = MAPPER.readerFor(TestUser.class)
                .with(schema)
                .with(CsvReadFeature.ONLY_UNQUOTED_NULL_VALUES_AS_NULL);

        // Unquoted N/A -> null
        TestUser user1 = reader.readValue("Grace,N/A,Hopper");
        assertEquals("Grace", user1.firstName);
        assertNull(user1.middleName);
        assertEquals("Hopper", user1.lastName);

        // Quoted "N/A" -> remains as string "N/A"
        TestUser user2 = reader.readValue("Grace,\"N/A\",Hopper");
        assertEquals("Grace", user2.firstName);
        assertEquals("N/A", user2.middleName);
        assertEquals("Hopper", user2.lastName);
    }

    // Test with array binding (non-POJO)
    @Test
    public void testUnquotedNullValueAsNullWithArrays() throws Exception {
        CsvSchema schema = CsvSchema.emptySchema().withNullValue("null");
        ObjectReader reader = MAPPER.reader()
                .with(schema)
                .with(CsvReadFeature.ONLY_UNQUOTED_NULL_VALUES_AS_NULL)
                .with(CsvReadFeature.WRAP_AS_ARRAY);

        // Unquoted null -> null
        try (MappingIterator<String[]> it = reader.forType(String[].class)
                .readValues("a,null,b")) {
            String[] arr = it.next();
            assertEquals(3, arr.length);
            assertEquals("a", arr[0]);
            assertNull(arr[1]);
            assertEquals("b", arr[2]);
        }

        // Quoted "null" -> string "null"
        try (MappingIterator<String[]> it = reader.forType(String[].class)
                .readValues("a,\"null\",b")) {
            String[] arr = it.next();
            assertEquals(3, arr.length);
            assertEquals("a", arr[0]);
            assertEquals("null", arr[1]);
            assertEquals("b", arr[2]);
        }
    }

    // Test with Object[] binding
    @Test
    public void testUnquotedNullValueAsNullWithObjectArrays() throws Exception {
        CsvSchema schema = CsvSchema.emptySchema().withNullValue("NULL");
        ObjectReader reader = MAPPER.reader()
                .with(schema)
                .with(CsvReadFeature.ONLY_UNQUOTED_NULL_VALUES_AS_NULL)
                .with(CsvReadFeature.WRAP_AS_ARRAY);

        // Unquoted NULL -> null
        try (MappingIterator<Object[]> it = reader.forType(Object[].class)
                .readValues("first,NULL,last")) {
            Object[] arr = it.next();
            assertEquals(3, arr.length);
            assertEquals("first", arr[0]);
            assertNull(arr[1]);
            assertEquals("last", arr[2]);
        }

        // Quoted "NULL" -> string "NULL"
        try (MappingIterator<Object[]> it = reader.forType(Object[].class)
                .readValues("first,\"NULL\",last")) {
            Object[] arr = it.next();
            assertEquals(3, arr.length);
            assertEquals("first", arr[0]);
            assertEquals("NULL", arr[1]);
            assertEquals("last", arr[2]);
        }
    }

    // Test feature disabled has no effect (same as default)
    @Test
    public void testFeatureDisabledSameAsDefault() throws Exception {
        CsvSchema schema = MAPPER.schemaFor(TestUser.class).withNullValue("N/A");
        ObjectReader reader = MAPPER.readerFor(TestUser.class)
                .with(schema)
                .without(CsvReadFeature.ONLY_UNQUOTED_NULL_VALUES_AS_NULL);

        // Both quoted and unquoted should become null
        TestUser user1 = reader.readValue("Grace,N/A,Hopper");
        assertNull(user1.middleName);

        TestUser user2 = reader.readValue("Grace,\"N/A\",Hopper");
        assertNull(user2.middleName);
    }
}
