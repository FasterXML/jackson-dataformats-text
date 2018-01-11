package com.fasterxml.jackson.dataformat.yaml.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

import java.io.StringWriter;
import java.math.BigInteger;

/**
 * Unit tests for checking functioning of the underlying
 * parser implementation.
 */
public class StreamingParseTest extends ModuleTestBase
{
    final YAMLFactory YAML_F = new YAMLFactory();

    public void testBasic() throws Exception
    {
        final String YAML =
 "string: 'text'\n"
+"bool: true\n"
+"bool2: false\n"
+"null: null\n"
+"i: 123\n"
+"d: 1.25\n"
;
        JsonParser p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("true", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals("false", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertEquals("null", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("123", p.getText());
        assertEquals(123, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals("1.25", p.getText());
        assertEquals(1.25, p.getDoubleValue());
        assertEquals(1, p.getIntValue());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        assertNull(p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    // Parsing large numbers around the transition from int->long and long->BigInteger
    public void testIntParsingWithLimits() throws Exception
    {
        String YAML;
        JsonParser p;

        // Test positive max-int
        YAML = "num: 2147483647";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(Integer.MAX_VALUE, p.getIntValue());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertEquals("2147483647", p.getText());
        p.close();

        // Test negative max-int
        YAML = "num: -2147483648";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(Integer.MIN_VALUE, p.getIntValue());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertEquals("-2147483648", p.getText());
        p.close();

        // Test positive max-int + 1
        YAML = "num: 2147483648";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(Integer.MAX_VALUE + 1L, p.getLongValue());
        assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
        assertEquals("2147483648", p.getText());
        p.close();

        // Test negative max-int - 1
        YAML = "num: -2147483649";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(Integer.MIN_VALUE - 1L, p.getLongValue());
        assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
        assertEquals("-2147483649", p.getText());
        p.close();

        // Test positive max-long
        YAML = "num: 9223372036854775807";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(Long.MAX_VALUE, p.getLongValue());
        assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
        assertEquals("9223372036854775807", p.getText());
        p.close();

        // Test negative max-long
        YAML = "num: -9223372036854775808";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(Long.MIN_VALUE, p.getLongValue());
        assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
        assertEquals("-9223372036854775808", p.getText());
        p.close();

        // Test positive max-long + 1
        YAML = "num: 9223372036854775808";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), p.getBigIntegerValue());
        assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());
        assertEquals("9223372036854775808", p.getText());
        p.close();

        // Test negative max-long - 1
        YAML = "num: -9223372036854775809";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE), p.getBigIntegerValue());
        assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());
        assertEquals("-9223372036854775809", p.getText());
        p.close();
    }

    // Testing addition of underscores
    public void testIntParsingUnderscoresSm() throws Exception
    {
        // First, couple of simple small values
        try (JsonParser p = YAML_F.createParser("num: 10_345")) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("num", p.getCurrentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(10345, p.getIntValue());
            assertEquals(JsonParser.NumberType.INT, p.getNumberType());
            assertEquals("10_345", p.getText());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }

        try (JsonParser p = YAML_F.createParser("num: -11_222")) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("num", p.getCurrentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(-11222, p.getIntValue());
            assertEquals(JsonParser.NumberType.INT, p.getNumberType());
            assertEquals("-11_222", p.getText());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }

        try (JsonParser p = YAML_F.createParser("num: +8_192")) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("num", p.getCurrentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(8192, p.getIntValue());
            assertEquals(JsonParser.NumberType.INT, p.getNumberType());
            assertEquals("+8_192", p.getText());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }

        String YAML;
        JsonParser p;

        // Test positive max-int
        YAML = "num: 2_147_483_647";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(Integer.MAX_VALUE, p.getIntValue());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertEquals("2_147_483_647", p.getText());
        p.close();

        // Test negative max-int
        YAML = "num: -2_147_483_648";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(Integer.MIN_VALUE, p.getIntValue());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertEquals("-2_147_483_648", p.getText());
        p.close();

        // Test positive max-int + 1
        YAML = "num: 2_147_483_648";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(Integer.MAX_VALUE + 1L, p.getLongValue());
        assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
        assertEquals("2_147_483_648", p.getText());
        p.close();

        // Test negative max-int - 1
        YAML = "num: -2_147_483_649";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(Integer.MIN_VALUE - 1L, p.getLongValue());
        assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
        assertEquals("-2_147_483_649", p.getText());
        p.close();

        // Test positive max-long
        YAML = "num: 9_223_372_036_854_775_807";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(Long.MAX_VALUE, p.getLongValue());
        assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
        assertEquals("9_223_372_036_854_775_807", p.getText());
        p.close();

        // Test negative max-long
        YAML = "num: -9_223_372_036_854_775_808";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(Long.MIN_VALUE, p.getLongValue());
        assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
        assertEquals("-9_223_372_036_854_775_808", p.getText());
        p.close();

        // Test positive max-long + 1
        YAML = "num: 9_223372036854775_808";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), p.getBigIntegerValue());
        assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());
        assertEquals("9_223372036854775_808", p.getText());
        p.close();

        // Test negative max-long - 1
        YAML = "num: -92233_72036_85477_5809";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE), p.getBigIntegerValue());
        assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());
        assertEquals("-92233_72036_85477_5809", p.getText());
        p.close();
    }

    // [cbor#4]: accidental recognition as double, with multiple dots
    public void testDoubleParsing() throws Exception
    {
        // First, test out valid use case.
        String YAML;

        YAML = "num: +1_000.25"; // note underscores; legal in YAML apparently
        JsonParser p = YAML_F.createParser(YAML);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.getCurrentName());

        StringWriter w = new StringWriter();
        assertEquals(3, p.getText(w));
        assertEquals("num", w.toString());
        
        // should be considered a String...
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(1000.25, p.getDoubleValue());
        // let's retain exact representation text however:
        assertEquals("+1_000.25", p.getText());
        p.close();
        
        // and then non-number that may be mistaken
        
        final String IP = "10.12.45.127";
        YAML = "ip: "+IP+"\n";
        p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("ip", p.getCurrentName());
        // should be considered a String...
        assertToken(JsonToken.VALUE_STRING, p.nextToken());

        w = new StringWriter();
        assertEquals(IP.length(), p.getText(w));
        assertEquals(IP, w.toString());
        
        assertEquals(IP, p.getText());
        p.close();
    }

    // [Issue#7]
    // looks like colons in content can be problematic, if unquoted
    public void testColons() throws Exception
    {
        // First, test out valid use case. NOTE: spaces matter!
        String YAML = "section:\n"
                    +"  text: foo:bar\n";
        JsonParser p = YAML_F.createParser(YAML);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("section", p.getCurrentName());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("text", p.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("foo:bar", p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());

        p.close();
    }
    
    /**
     * How should YAML Anchors be exposed?
     */
    public void testAnchorParsing() throws Exception
    {
        // silly doc, just to expose an id (anchor) and ref to it
        final String YAML = "---\n"
                +"parent: &id1\n"
                +"    name: Bob\n"
                +"child: &id2\n"
                +"    name: Bill\n"
                +"    parentRef: *id1"
                ;
        YAMLParser yp = YAML_F.createParser(YAML);

        assertToken(JsonToken.START_OBJECT, yp.nextToken());
        assertFalse(yp.isCurrentAlias());
        assertNull(yp.getObjectId());

        assertToken(JsonToken.FIELD_NAME, yp.nextToken());
        assertEquals("parent", yp.getCurrentName());
        assertFalse(yp.isCurrentAlias());
        assertNull(yp.getObjectId());

        assertToken(JsonToken.START_OBJECT, yp.nextToken());
        assertFalse(yp.isCurrentAlias());
        assertEquals("id1", yp.getObjectId());
        assertToken(JsonToken.FIELD_NAME, yp.nextToken());
        assertEquals("name", yp.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, yp.nextToken());
        assertEquals("Bob", yp.getText());
        assertFalse(yp.isCurrentAlias());
        assertToken(JsonToken.END_OBJECT, yp.nextToken());

        assertToken(JsonToken.FIELD_NAME, yp.nextToken());
        assertEquals("child", yp.getCurrentName());
        assertFalse(yp.isCurrentAlias());
        assertToken(JsonToken.START_OBJECT, yp.nextToken());
        assertFalse(yp.isCurrentAlias());
        assertEquals("id2", yp.getObjectId());
        assertToken(JsonToken.FIELD_NAME, yp.nextToken());
        assertEquals("name", yp.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, yp.nextToken());
        assertEquals("Bill", yp.getText());
        assertToken(JsonToken.FIELD_NAME, yp.nextToken());
        assertEquals("parentRef", yp.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, yp.nextToken());
        assertEquals("id1", yp.getText());
        assertTrue(yp.isCurrentAlias());
        assertToken(JsonToken.END_OBJECT, yp.nextToken());

        assertToken(JsonToken.END_OBJECT, yp.nextToken());
        
        assertNull(yp.nextToken());
        yp.close();
    }

    // [Issue#10]
    // Scalars should not be parsed when not in the plain flow style.
    public void testQuotedStyles() throws Exception
    {
        String YAML = "strings: [\"true\", 'false']";
        JsonParser p = YAML_F.createParser(YAML);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("strings", p.getCurrentName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("true", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("false", p.getText());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());

        p.close();
    }

    // Scalars should be parsed when in the plain flow style.
    public void testUnquotedStyles() throws Exception
    {
        String YAML = "booleans: [true, false]";
        JsonParser p = YAML_F.createParser(YAML);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("booleans", p.getCurrentName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());

        p.close();
    }

    public void testObjectWithNumbers() throws Exception
    {
        String YAML = "---\n"
+"content:\n"
+"  uri: \"http://javaone.com/keynote.mpg\"\n"
+"  title: \"Javaone Keynote\"\n"
+"  width: 640\n"
+"  height: 480\n"
+"  persons:\n"
+"  - \"Foo Bar\"\n"
+"  - \"Max Power\"\n"
;

        JsonParser p = YAML_F.createParser(YAML);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("content", p.getCurrentName());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("uri", p.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("title", p.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("width", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(640, p.getIntValue());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("height", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(480, p.getIntValue());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("persons", p.getCurrentName());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("Foo Bar", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("Max Power", p.getText());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());

        p.close();
    }

    public void testNulls() throws Exception
    {
        String YAML = "nulls: [!!null \"null\" ]";
        JsonParser p = YAML_F.createParser(YAML);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("nulls", p.getCurrentName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());

        p.close();
    }

    public void testTildeNulls() throws Exception
    {
        String YAML = "nulls: [~ ]";
        JsonParser p = YAML_F.createParser(YAML);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("nulls", p.getCurrentName());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());

        p.close();
    }

    // for [dataformat-yaml#69]
    public void testTimeLikeValues() throws Exception
    {
          final String YAML = "value: 3:00\n";
          JsonParser p = YAML_F.createParser(YAML);

          assertToken(JsonToken.START_OBJECT, p.nextToken());
          assertToken(JsonToken.FIELD_NAME, p.nextToken());
          assertEquals("value", p.getCurrentName());
          assertToken(JsonToken.VALUE_STRING, p.nextToken());
          assertEquals("3:00", p.getText());
          assertToken(JsonToken.END_OBJECT, p.nextToken());
          assertNull(p.nextToken());
          p.close();
    }
}
