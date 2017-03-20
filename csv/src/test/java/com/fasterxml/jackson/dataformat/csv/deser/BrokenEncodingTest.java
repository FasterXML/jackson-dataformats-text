package com.fasterxml.jackson.dataformat.csv.deser;

import java.io.ByteArrayOutputStream;
import java.io.CharConversionException;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.csv.*;

public class BrokenEncodingTest extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    // Simple test where a Latin-1 character is encountered; first byte wrong
    public void testLatin1AsUTF8() throws Exception
    {
        CsvFactory factory = new CsvFactory();
        String CSV = "1,2\nabc,\u00A0\n";

        CsvSchema schema = CsvSchema.builder()
                .addColumn("a")
                .addColumn("b")
                .build();
        // So: take Latin-1 bytes, but construct without specifying to lead to UTF-8 handling
        CsvParser parser = factory.createParser(CSV.getBytes("ISO-8859-1"));
        parser.setSchema(schema);

        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("2", parser.getText());
        assertToken(JsonToken.END_OBJECT, parser.nextToken());

        // problem should only be triggered now
        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("abc", parser.getText());
        try {
            parser.nextToken();
            fail("Should trigger exception for invalid UTF-8 char");
        } catch (CharConversionException e) {
            verifyException(e, "Invalid UTF-8 start byte");
            verifyException(e, "0xA0");
        }
        parser.close();
    }

    // Then a test with "middle" byte broken
    public void testBrokenUTF8MiddleByte() throws Exception
    {
        CsvFactory factory = new CsvFactory();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write('1');
        bytes.write(',');
        bytes.write(0xD7); // this is fine
        bytes.write(0x41); // this not
        bytes.write('\n');
        CsvSchema schema = CsvSchema.builder()
                .addColumn("a")
                .addColumn("b")
                .build();
        // So: take Latin-1 bytes, but construct without specifying to lead to UTF-8 handling
        CsvParser parser = factory.createParser(bytes.toByteArray());
        parser.setSchema(schema);

        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("1", parser.getText());
        try {
            assertToken(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("b", parser.getCurrentName());
            parser.nextToken();
            fail("Should trigger exception for invalid UTF-8 char");
        } catch (CharConversionException e) {
            verifyException(e, "Invalid UTF-8 middle byte");
            verifyException(e, "0x41");
       }
        parser.close();
    }
}
