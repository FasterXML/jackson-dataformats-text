package tools.jackson.dataformat.csv.limits;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.databind.MappingIterator;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StreamReadConstraints#validateNameLength(int)} enforcement
 * in CSV header parsing.
 *
 * @see <a href="https://github.com/FasterXML/jackson-dataformats-text/issues/634">[dataformats-text#634]</a>
 */
public class CSVNameLengthLimitsTest extends ModuleTestBase
{
    private final static int MAX_NAME_LEN = 100;

    private CsvMapper mapperWithNameLenLimit(int maxNameLen) {
        return CsvMapper.builder(
                CsvFactory.builder()
                    .streamReadConstraints(StreamReadConstraints.builder()
                        .maxNameLength(maxNameLen)
                        .build())
                    .build())
            .build();
    }

    // Test with header-from-schema path (schema has columns, useHeader, reordersColumns)
    @Test
    public void testHeaderNameTooLong_withReorder() throws Exception
    {
        final String longName = "a".repeat(MAX_NAME_LEN + 50);
        final String csv = longName + ",b\n1,2\n";

        CsvMapper mapper = mapperWithNameLenLimit(MAX_NAME_LEN);
        CsvSchema schema = CsvSchema.builder()
                .setUseHeader(true)
                .setReorderColumns(true)
                .addColumn(longName)
                .addColumn("b")
                .build();

        try {
            MappingIterator<List<String>> it = mapper.readerForListOf(String.class)
                    .with(schema)
                    .readValues(csv);
            it.readAll();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Name length");
            verifyException(e, "exceeds the maximum allowed");
        }
    }

    // Test with header-from-schema, strict headers path
    @Test
    public void testHeaderNameTooLong_strictHeaders() throws Exception
    {
        final String longName = "a".repeat(MAX_NAME_LEN + 50);
        final String csv = longName + ",b\n1,2\n";

        CsvMapper mapper = mapperWithNameLenLimit(MAX_NAME_LEN);
        CsvSchema schema = CsvSchema.builder()
                .setUseHeader(true)
                .setStrictHeaders(true)
                .addColumn(longName)
                .addColumn("b")
                .build();

        try {
            MappingIterator<List<String>> it = mapper.readerForListOf(String.class)
                    .with(schema)
                    .readValues(csv);
            it.readAll();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Name length");
            verifyException(e, "exceeds the maximum allowed");
        }
    }

    // Test with non-strict, non-reorder path (schema has columns, header skipped but validated)
    @Test
    public void testHeaderNameTooLong_noReorder() throws Exception
    {
        final String longName = "a".repeat(MAX_NAME_LEN + 50);
        final String csv = longName + ",b\n1,2\n";

        CsvMapper mapper = mapperWithNameLenLimit(MAX_NAME_LEN);
        CsvSchema schema = CsvSchema.builder()
                .setUseHeader(true)
                .addColumn(longName)
                .addColumn("b")
                .build();

        try {
            MappingIterator<List<String>> it = mapper.readerForListOf(String.class)
                    .with(schema)
                    .readValues(csv);
            it.readAll();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Name length");
            verifyException(e, "exceeds the maximum allowed");
        }
    }

    // Test that names within the limit work fine
    @Test
    public void testHeaderNameWithinLimit() throws Exception
    {
        final String name = "a".repeat(MAX_NAME_LEN);
        final String csv = name + ",b\nvalue1,value2\n";

        CsvMapper mapper = mapperWithNameLenLimit(MAX_NAME_LEN);
        CsvSchema schema = CsvSchema.builder()
                .setUseHeader(true)
                .setReorderColumns(true)
                .addColumn(name)
                .addColumn("b")
                .build();

        // Should complete without exception
        MappingIterator<Object> it = mapper.readerFor(Object.class)
                .with(schema)
                .readValues(csv);
        assertTrue(it.hasNextValue());
        it.nextValue();
        assertFalse(it.hasNextValue());
        it.close();
    }
}
