package tools.jackson.dataformat.csv.deser;

import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectReader;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that the maximum number of header columns
 * ({@code CsvParser.MAX_COLUMNS}, 99,999) is enforced when the header
 * line is read from input.
 */
public class MaxHeaderColumnsTest extends ModuleTestBase
{
    private final static int MAX_COLUMNS = MAX_CSV_HEADER_COLUMNS;

    private final CsvMapper MAPPER = mapperForCsv();

    // Header line read into an empty schema: limit is accepted exactly
    @Test
    public void testHeaderColumnsAtLimit() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader());
        try (MappingIterator<Map<String, Object>> it = r.readValues(_header(MAX_COLUMNS) + "x\n")) {
            Map<String, Object> row = it.next();
            assertEquals("x", row.get("c0"));
            assertFalse(it.hasNext());
        }
    }

    // Header line read into an empty schema: one column over the limit fails
    @Test
    public void testHeaderColumnsOverLimit() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader());
        try (MappingIterator<Map<String, Object>> it = r.readValues(_header(MAX_COLUMNS + 1) + "x\n")) {
            it.nextValue();
            fail("Should not pass with " + (MAX_COLUMNS + 1) + " header columns");
        } catch (StreamReadException e) {
            verifyException(e, "Too many header columns: maximum of " + MAX_COLUMNS + " allowed");
        }
    }

    // Same, but with INSERT_NULLS_FOR_MISSING_COLUMNS enabled (where header column
    // count determines number of null values inserted for each short row)
    @Test
    public void testHeaderColumnsOverLimitWithInsertNulls() throws Exception
    {
        CsvMapper mapper = CsvMapper.builder()
                .enable(CsvReadFeature.INSERT_NULLS_FOR_MISSING_COLUMNS)
                .build();
        ObjectReader r = mapper.readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader());
        try (MappingIterator<Map<String, Object>> it = r.readValues(_header(MAX_COLUMNS + 1) + "x\n")) {
            it.nextValue();
            fail("Should not pass with " + (MAX_COLUMNS + 1) + " header columns");
        } catch (StreamReadException e) {
            verifyException(e, "Too many header columns: maximum of " + MAX_COLUMNS + " allowed");
        }
    }

    // Header line skipped (schema has columns, no reordering, non-strict headers):
    // limit applies to number of header columns skipped; limit is accepted exactly
    @Test
    public void testSkippedHeaderColumnsAtLimit() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(Map.class).with(_singleColumnSchema());
        try (MappingIterator<Map<String, Object>> it = r.readValues(_header(MAX_COLUMNS) + "x\n")) {
            Map<String, Object> row = it.next();
            assertEquals("x", row.get("c0"));
            assertFalse(it.hasNext());
        }
    }

    // Header line skipped: one column over the limit fails
    @Test
    public void testSkippedHeaderColumnsOverLimit() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(Map.class).with(_singleColumnSchema());
        try (MappingIterator<Map<String, Object>> it = r.readValues(_header(MAX_COLUMNS + 1) + "x\n")) {
            it.nextValue();
            fail("Should not pass with " + (MAX_COLUMNS + 1) + " header columns");
        } catch (StreamReadException e) {
            verifyException(e, "Too many header columns: maximum of " + MAX_COLUMNS + " allowed");
        }
    }

    // Header within limit, short data row, INSERT_NULLS_FOR_MISSING_COLUMNS enabled:
    // without token limit, nulls are inserted for all missing columns
    @Test
    public void testInsertNullsWithoutTokenLimit() throws Exception
    {
        ObjectReader r = _insertNullsMapper(StreamReadConstraints.defaults())
                .readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader());
        try (MappingIterator<Map<String, Object>> it = r.readValues(_header(MAX_COLUMNS) + "x\n")) {
            Map<String, Object> row = it.next();
            assertEquals(MAX_COLUMNS, row.size());
            assertEquals("x", row.get("c0"));
            assertTrue(row.containsKey("c" + (MAX_COLUMNS - 1)));
            assertNull(row.get("c" + (MAX_COLUMNS - 1)));
            assertFalse(it.hasNext());
        }
    }

    // Same, but with StreamReadConstraints.maxTokenCount set: tokens for inserted
    // nulls count against the limit (as recommended by Feature Javadoc)
    @Test
    public void testInsertNullsWithTokenLimit() throws Exception
    {
        final long maxTokens = 1000L;
        ObjectReader r = _insertNullsMapper(StreamReadConstraints.builder()
                        .maxTokenCount(maxTokens).build())
                .readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader());
        try (MappingIterator<Map<String, Object>> it = r.readValues(_header(MAX_COLUMNS) + "x\n")) {
            it.nextValue();
            fail("Should not pass with maxTokenCount of " + maxTokens);
        } catch (StreamConstraintsException e) {
            verifyException(e, "Token count");
            verifyException(e, String.valueOf(maxTokens));
        }
    }

    private static CsvSchema _singleColumnSchema() {
        return CsvSchema.builder()
                .addColumn("c0")
                .build()
                .withHeader();
    }

    private static CsvMapper _insertNullsMapper(StreamReadConstraints constraints) {
        return CsvMapper.builder(CsvFactory.builder()
                        .streamReadConstraints(constraints)
                        .build())
                .enable(CsvReadFeature.INSERT_NULLS_FOR_MISSING_COLUMNS)
                .build();
    }

    private static String _header(int columns) {
        StringBuilder sb = new StringBuilder(columns * 7);
        for (int i = 0; i < columns; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('c').append(i);
        }
        return sb.append('\n').toString();
    }
}
