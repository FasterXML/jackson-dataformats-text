package com.fasterxml.jackson.dataformat.csv.deser;

import java.io.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Container for some low-level tests that use parser directly;
 * needed for exercising certain methods that are difficult to
 * trigger via data-binding
 */
public class StreamingCSVReadTest extends ModuleTestBase
{
    private final CsvFactory CSV_F = new CsvFactory();

    private final CsvSchema ABC_SCHEMA = CsvSchema.builder()
            .addColumn("a")
            .addColumn("b")
            .addColumn("c")
            .setUseHeader(false)
            .build();

    protected CsvFactory csvFactory() {
        return CSV_F;
    }

    @Test
    public void testIntRead() throws Exception
    {
        _testInts(1, 59, -8);
        _testInts(10093525, -123456789, 123456789);
        _testInts(-123451, 0, -829);
        _testInts(Integer.MAX_VALUE, Integer.MIN_VALUE, 3);
    }

    @Test
    public void testLongRead() throws Exception
    {
        _testLongs(1L, -3L);
        _testLongs(1234567890L, -9876543212345679L);
        _testLongs(Long.MIN_VALUE, Long.MAX_VALUE);
    }
    
    @Test
    public void testFloatRead() throws Exception
    {
        _testDoubles(1.0, 125.375, -900.5);
        _testDoubles(10093525.125, -123456789.5, 123456789.0);
        _testDoubles(-123451.75, 0.0625, -829.5);
    }

    private void _testInts(int a, int b, int c) throws Exception {
        _testInts(false, a, b, c);
        _testInts(true, a, b, c);

        _testIntsExpected(false, a, b, c);
        _testIntsExpected(true, a, b, c);
    }

    private void _testLongs(long a, long b) throws Exception {
        _testLongs(false, a, b);
        _testLongs(true, a, b);
    }
    
    private void _testDoubles(double a, double b, double c) throws Exception {
        _testDoubles(false, a, b, c);
        _testDoubles(true, a, b, c);
    }

    private void _testInts(boolean useBytes, int a, int b, int c) throws Exception {
        CsvParser parser = _parser(String.format("%d,%d,%d\n", a, b, c), useBytes, ABC_SCHEMA);

        assertToken(JsonToken.START_OBJECT, parser.nextToken());

        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.currentName());

        StringWriter w = new StringWriter();
        assertEquals(1, parser.getText(w));
        assertEquals("a", w.toString());

        String numStr = String.valueOf(a);
        assertEquals(numStr, parser.nextTextValue());
        char[] ch = parser.getTextCharacters();
        String str2 = new String(ch, parser.getTextOffset(), parser.getTextLength());
        assertEquals(numStr, str2);
        w = new StringWriter();
        assertEquals(numStr.length(), parser.getText(w));
        assertEquals(numStr, w.toString());

        assertEquals(a, parser.getIntValue());
        assertEquals((long) a, parser.getLongValue());

        assertEquals("b", parser.nextFieldName());
        assertEquals(""+b, parser.nextTextValue());
        assertEquals((long) b, parser.getLongValue());
        assertEquals(b, parser.getIntValue());

        assertTrue(parser.nextFieldName(new SerializedString("c")));

        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals(c, parser.getIntValue());
        assertEquals((long) c, parser.getLongValue());

        assertToken(JsonToken.END_OBJECT, parser.nextToken());
        assertNull(parser.nextToken());
        
        parser.close();
    }

    private void _testIntsExpected(boolean useBytes, int a, int b, int c) throws Exception {
        try (CsvParser parser = _parser(String.format("%d,%d,%d\n", a, b, c), useBytes, ABC_SCHEMA)) {
            assertToken(JsonToken.START_OBJECT, parser.nextToken());

            assertToken(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("a", parser.currentName());

            // Reported as String BUT may be coerced
            assertToken(JsonToken.VALUE_STRING, parser.nextToken());
            assertTrue(parser.isExpectedNumberIntToken());
            assertEquals(a, parser.getIntValue());
            assertToken(JsonToken.VALUE_NUMBER_INT, parser.currentToken());

            assertEquals("b", parser.nextFieldName());
            assertToken(JsonToken.VALUE_STRING, parser.nextToken());
            assertTrue(parser.isExpectedNumberIntToken());
            assertEquals(NumberType.INT, parser.getNumberType());
            assertEquals(b, parser.getIntValue());
            assertToken(JsonToken.VALUE_NUMBER_INT, parser.currentToken());

            assertEquals("c", parser.nextFieldName());
            assertToken(JsonToken.VALUE_STRING, parser.nextToken());
            assertTrue(parser.isExpectedNumberIntToken());
            assertEquals(NumberType.INT, parser.getNumberType());
            assertEquals(c, parser.getIntValue());
            assertToken(JsonToken.VALUE_NUMBER_INT, parser.currentToken());

            assertToken(JsonToken.END_OBJECT, parser.nextToken());
            assertNull(parser.nextToken());
        }
    }

    private void _testLongs(boolean useBytes, long a, long b) throws Exception
    {
        CsvParser parser = _parser(String.format("%d,%d\n", a, b), useBytes, ABC_SCHEMA);

        assertToken(JsonToken.START_OBJECT, parser.nextToken());

        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.currentName());
        assertEquals(""+a, parser.nextTextValue());
        assertEquals(a, parser.getLongValue());

        assertEquals("b", parser.nextFieldName());
        assertEquals(""+b, parser.nextTextValue());
        assertEquals(b, parser.getLongValue());

        assertToken(JsonToken.END_OBJECT, parser.nextToken());
        assertNull(parser.nextToken());
        
        parser.close();
    }
    
    private void _testDoubles(boolean useBytes, double a, double b, double c)
            throws Exception
    {
        CsvParser parser = _parser(String.format("%s,%s,%s\n", a, b, c), useBytes, ABC_SCHEMA);

        assertToken(JsonToken.START_OBJECT, parser.nextToken());

        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.currentName());
        assertEquals(""+a, parser.nextTextValue());
        assertEquals(a, parser.getDoubleValue());
        assertEquals((float) a, parser.getFloatValue());

        assertEquals("b", parser.nextFieldName());
        assertEquals(""+b, parser.nextTextValue());
        assertEquals((float) b, parser.getFloatValue());
        assertEquals(b, parser.getDoubleValue());

        assertTrue(parser.nextFieldName(new SerializedString("c")));

        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals(c, parser.getDoubleValue());
        assertEquals((float) c, parser.getFloatValue());

        assertToken(JsonToken.END_OBJECT, parser.nextToken());
        assertNull(parser.nextToken());
        
        parser.close();
    }

    private CsvParser _parser(String csv, boolean useBytes, CsvSchema schema)
        throws IOException
    {
        CsvParser p;
        if (useBytes) {
            p = csvFactory().createParser(new ByteArrayInputStream(utf8(csv)));
        } else {
            p = csvFactory().createParser(csv);
        }
        p.setSchema(schema);
        return p;
    }
}
