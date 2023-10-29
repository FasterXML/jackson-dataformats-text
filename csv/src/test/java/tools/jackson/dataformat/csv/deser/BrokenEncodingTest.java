package tools.jackson.dataformat.csv.deser;

import java.io.ByteArrayOutputStream;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.csv.*;

public class BrokenEncodingTest extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = mapperForCsv();

    // Simple test where a Latin-1 character is encountered; first byte wrong
    public void testLatin1AsUTF8() throws Exception
    {
        String CSV = "1,2\nabc,\u00A0\n";

        CsvSchema schema = CsvSchema.builder()
                .addColumn("a")
                .addColumn("b")
                .build();
        // So: take Latin-1 bytes, but construct without specifying to lead to UTF-8 handling
        JsonParser parser = MAPPER.reader(schema)
                .createParser(CSV.getBytes("ISO-8859-1"));

        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, parser.nextToken());
        assertEquals("a", parser.currentName());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, parser.nextToken());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("2", parser.getText());
        assertToken(JsonToken.END_OBJECT, parser.nextToken());

        // problem should only be triggered now
        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, parser.nextToken());
        assertEquals("a", parser.currentName());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("abc", parser.getText());
        try {
            parser.nextToken();
            fail("Should trigger exception for invalid UTF-8 char");
        } catch (JacksonIOException e) {
            verifyException(e, "Invalid UTF-8 start byte");
            verifyException(e, "0xA0");
        }
        parser.close();
    }

    // Then a test with "middle" byte broken
    public void testBrokenUTF8MiddleByte() throws Exception
    {
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
        JsonParser parser = MAPPER.reader(schema)
                .createParser(bytes.toByteArray());

        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, parser.nextToken());
        assertEquals("a", parser.currentName());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("1", parser.getText());
        try {
            assertToken(JsonToken.PROPERTY_NAME, parser.nextToken());
            assertEquals("b", parser.currentName());
            parser.nextToken();
            fail("Should trigger exception for invalid UTF-8 char");
        } catch (JacksonIOException e) {
            verifyException(e, "Invalid UTF-8 middle byte");
            verifyException(e, "0x41");
       }
        parser.close();
    }
}
