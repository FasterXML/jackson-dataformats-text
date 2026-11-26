package com.fasterxml.jackson.dataformat.csv.deser;

import java.util.Map;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;

import com.fasterxml.jackson.dataformat.csv.*;

/**
 * Tests to verify that the maximum number of header columns
 * ({@code CsvParser.MAX_COLUMNS}, 99,999) is enforced when the header
 * line is read from input.
 */
public class MaxHeaderColumnsTest extends ModuleTestBase
{
    // Must match private constant in CsvParser
    private final static int MAX_COLUMNS = 99999;

    private final CsvMapper MAPPER = mapperForCsv();

    // Header line read into an empty schema: limit is accepted exactly
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
    public void testHeaderColumnsOverLimit() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader());
        try (MappingIterator<Map<String, Object>> it = r.readValues(_header(MAX_COLUMNS + 1) + "x\n")) {
            it.next();
            fail("Should not pass with " + (MAX_COLUMNS + 1) + " header columns");
        } catch (StreamReadException e) {
            verifyException(e, "reached maximum of " + MAX_COLUMNS + " header columns");
        }
    }

    // Same, but with INSERT_NULLS_FOR_MISSING_COLUMNS enabled (where header column
    // count determines number of null values inserted for each short row)
    public void testHeaderColumnsOverLimitWithInsertNulls() throws Exception
    {
        CsvMapper mapper = CsvMapper.builder()
                .enable(CsvParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS)
                .build();
        ObjectReader r = mapper.readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader());
        try (MappingIterator<Map<String, Object>> it = r.readValues(_header(MAX_COLUMNS + 1) + "x\n")) {
            it.next();
            fail("Should not pass with " + (MAX_COLUMNS + 1) + " header columns");
        } catch (StreamReadException e) {
            verifyException(e, "reached maximum of " + MAX_COLUMNS + " header columns");
        }
    }

    // Header line skipped (schema has columns, no reordering, non-strict headers):
    // limit applies to number of header columns skipped
    public void testSkippedHeaderColumnsOverLimit() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("c0")
                .build()
                .withHeader();
        ObjectReader r = MAPPER.readerFor(Map.class).with(schema);
        try (MappingIterator<Map<String, Object>> it = r.readValues(_header(MAX_COLUMNS + 2) + "x\n")) {
            it.next();
            fail("Should not pass with " + (MAX_COLUMNS + 2) + " header columns");
        } catch (StreamReadException e) {
            verifyException(e, "skipped " + MAX_COLUMNS + " header columns");
        }
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
