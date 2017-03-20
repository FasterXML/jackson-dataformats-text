package com.fasterxml.jackson.dataformat.csv.deser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.csv.*;
import com.google.common.collect.Lists;

import static org.junit.Assert.assertArrayEquals;

public class BasicParserTest extends ModuleTestBase {
    @JsonPropertyOrder({"x", "y", "z"})
    public static class Point {
        public int x;
        public Integer y;
        public Integer z = 8;
    }

    final static CsvSchema SIMPLE_SCHEMA = CsvSchema.builder()
            .addColumn("firstName")
            .addColumn("lastName")
            .addColumn("gender")
            .addColumn("userImage")
            .addColumn("verified")
            .build();

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    final CsvMapper MAPPER = mapperForCsv();

    public void testSimpleExplicit() throws Exception {
        ObjectReader r = MAPPER.reader(SIMPLE_SCHEMA);
        _testSimpleExplicit(r, false);
        _testSimpleExplicit(r, true);
    }

    private void _testSimpleExplicit(ObjectReader r, boolean useBytes) throws Exception {
        r = r.forType(FiveMinuteUser.class);
        FiveMinuteUser user;
        final String INPUT = "Bob,Robertson,MALE,AQIDBAU=,false\n";
        if (useBytes) {
            user = r.readValue(INPUT);
        } else {
            user = r.readValue(INPUT.getBytes("UTF-8"));
        }
        assertEquals("Bob", user.firstName);
        assertEquals("Robertson", user.lastName);
        assertEquals(Gender.MALE, user.getGender());
        assertFalse(user.isVerified());
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, user.getUserImage());
    }

    public void testSimpleExplicitWithBOM() throws Exception {
        ObjectReader r = MAPPER.reader(SIMPLE_SCHEMA);
        r = r.forType(FiveMinuteUser.class);
        FiveMinuteUser user;

        ByteArrayOutputStream b = new ByteArrayOutputStream();

        // first, UTF-8 BOM:
        b.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        b.write("Bob,Robertson,MALE,AQIDBAU=,false\n".getBytes("UTF-8"));
        b.close();

        user = r.readValue(b.toByteArray());
        String fn = user.firstName;

        if (!fn.equals("Bob")) {
            fail("Expected 'Bob' (3), got '" + fn + "' (" + fn.length() + ")");
        }
        assertEquals("Robertson", user.lastName);
        assertEquals(Gender.MALE, user.getGender());
        assertFalse(user.isVerified());
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, user.getUserImage());
    }

    public void testSimpleWithAutoSchema() throws Exception {
        CsvSchema schema = MAPPER.schemaFor(FiveMinuteUser.class);
        // NOTE: order different from above test (as per POJO def!)
        FiveMinuteUser user = MAPPER.reader(schema).forType(FiveMinuteUser.class).readValue("Joe,Josephson,MALE,true,AwE=\n");
        assertEquals("Joe", user.firstName);
        assertEquals("Josephson", user.lastName);
        assertEquals(Gender.MALE, user.getGender());
        assertTrue(user.isVerified());
        assertArrayEquals(new byte[]{3, 1}, user.getUserImage());
    }

    /**
     * Test to verify that we can mix "untyped" access as Maps
     * with schema information...
     */
    public void testSimpleAsMaps() throws Exception {
        CsvSchema schema = MAPPER.schemaFor(FiveMinuteUser.class);
        MappingIterator<Map<?, ?>> it = MAPPER.reader(schema).forType(Map.class).readValues(
                "Joe,Smith,MALE,false,"
        );
        assertTrue(it.hasNext());
        Map<?, ?> result = it.nextValue();
        assertEquals(5, result.size());
        assertEquals("Joe", result.get("firstName"));
        assertEquals("Smith", result.get("lastName"));
        assertEquals("MALE", result.get("gender"));
        assertEquals("false", result.get("verified"));
        assertEquals("", result.get("userImage"));

        assertFalse(it.hasNextValue());
        it.close();
    }

    // Test for [Issue#10]
    public void testMapsWithLinefeeds() throws Exception {
        _testMapsWithLinefeeds(false);
        _testMapsWithLinefeeds(true);
    }

    private void _testMapsWithLinefeeds(boolean useBytes) throws Exception {
        String CSV = "A,B,C\n"
                + "data11,data12\n"
                + "data21,data22,data23\r\n"
                + "data31,\"data32 data32\ndata32 data32\",data33\n"
                + "data41,\"data42 data42\r\ndata42\",data43\n";

        CsvSchema cs = CsvSchema.emptySchema().withHeader();
        ObjectReader or = MAPPER.readerFor(HashMap.class).with(cs);

        MappingIterator<Map<String, String>> mi;

        if (useBytes) {
            mi = or.readValues(CSV.getBytes("UTF-8"));
        } else {
            mi = or.readValues(CSV);
        }

        assertTrue(mi.hasNext());
        Map<String, String> map = mi.nextValue();
        assertNotNull(map);
        assertEquals("data11", map.get("A"));
        assertEquals("data12", map.get("B"));
        assertEquals(2, map.size());

        assertTrue(mi.hasNext());
        map = mi.nextValue();
        assertNotNull(map);
        assertEquals(3, map.size());

        // then entries with linefeeds
        assertTrue(mi.hasNext());
        map = mi.nextValue();
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("data31", map.get("A"));
        assertEquals("data32 data32\ndata32 data32", map.get("B"));
        assertEquals("data33", map.get("C"));

        assertTrue(mi.hasNext());
        map = mi.nextValue();
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("data41", map.get("A"));
        assertEquals("data42 data42\r\ndata42", map.get("B"));
        assertEquals("data43", map.get("C"));

        assertFalse(mi.hasNext());
        mi.close();
    }

    // [dataformat-csv#12]
    public void testEmptyHandlingForInteger() throws Exception {
        CsvSchema schema = MAPPER.typedSchemaFor(Point.class).withoutHeader();

        // First: empty value, to be considered as null
        Point result = MAPPER.readerFor(Point.class).with(schema).readValue(",,\n");
        assertEquals(0, result.x);
        assertNull(result.y);
        assertNull(result.z);
    }

    public void testStringNullHandlingForInteger() throws Exception {
        CsvSchema schema = MAPPER.typedSchemaFor(Point.class).withoutHeader();

        // First: empty value, to be considered as null
        Point result = MAPPER.readerFor(Point.class).with(schema).readValue("null,null,null\n");
        assertEquals(0, result.x);
        assertNull(result.y);
        assertNull(result.z);
    }

    public void testLeadingZeroesForInts() throws Exception {
        CsvSchema schema = MAPPER.typedSchemaFor(Point.class).withoutHeader();
        Point result = MAPPER.readerFor(Point.class).with(schema).readValue("012,\"090\",\n");
        assertEquals(12, result.x);
        assertEquals(Integer.valueOf(90), result.y);
        assertNull(result.z);
    }

    // [dataformat-csv#41]
    public void testIncorrectDups41() throws Exception {
        final String INPUT = "\"foo\",\"bar\",\"foo\"";
        CsvSchema schema = CsvSchema.builder().addColumn("Col1").addColumn("Col2")
                .addColumn("Col3").build();

        MappingIterator<Object> iter = MAPPER.readerFor(Object.class)
                .with(schema).readValues(INPUT);

        Map<?, ?> m = (Map<?, ?>) iter.next();
        assertFalse(iter.hasNextValue());
        iter.close();

        if (m.size() != 3) {
            fail("Should have 3 entries, but got: " + m);
        }
        assertEquals("foo", m.get("Col1"));
        assertEquals("bar", m.get("Col2"));
        assertEquals("foo", m.get("Col3"));
    }

    // for [dataformat-csv#89]
    public void testColumnReordering() throws IOException {
        CsvFactory factory = new CsvFactory();
        String CSV = "b,a,c\nvb,va,vc\n";

        /* Test first column reordering, by setting the
           columns in a different order to the ones
           found in the CSV example
         */
        CsvSchema schemaWithReordering = CsvSchema.builder()
                .addColumn("a")
                .addColumn("b")
                .addColumn("c")
                .setLineSeparator('\n')
                .setUseHeader(true)         // must be set for column reordering
                .setReorderColumns(true)    // set column reordering
                .build();

        // Create a parser and ensure data is processed in the
        // right order, as per header
        CsvParser parser = factory.createParser(CSV);
        parser.setSchema(schemaWithReordering);
        assertEquals(JsonToken.START_OBJECT, parser.nextToken());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("b", parser.getCurrentName());
        assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("vb", parser.getValueAsString());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a",parser.getCurrentName());
        assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("va", parser.getValueAsString());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("c", parser.getCurrentName());
        assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("vc", parser.getValueAsString());
        assertEquals(JsonToken.END_OBJECT, parser.nextToken());
        parser.close();

        /*
            Now make a copy of the schema but set the reordering
            flag to false.  In this case the columns values are
            reported as per the schema order, not the header.
         */
        CsvSchema schemaWithoutReordering = schemaWithReordering.withColumnReordering(false);
        parser = factory.createParser(CSV);
        parser.setSchema(schemaWithoutReordering);
        assertEquals(JsonToken.START_OBJECT, parser.nextToken());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.getCurrentName());
        assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("vb", parser.getValueAsString());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("b", parser.getCurrentName());
        assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("va", parser.getValueAsString());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("c", parser.getCurrentName());
        assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("vc", parser.getValueAsString());
        assertEquals(JsonToken.END_OBJECT, parser.nextToken());
        parser.close();

        /*
            From the schema with reordering, disabling use header flag
            causes the same effect as the previous test.
         */
        CsvSchema schemaWithoutHeader = schemaWithReordering
                .withUseHeader(false)
                .withSkipFirstDataRow(true);

        parser = factory.createParser(CSV);
        parser.setSchema(schemaWithoutHeader);
        assertEquals(JsonToken.START_OBJECT, parser.nextToken());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.getCurrentName());
        assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("vb", parser.getValueAsString());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("b", parser.getCurrentName());
        assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("va", parser.getValueAsString());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("c", parser.getCurrentName());
        assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("vc", parser.getValueAsString());
        assertEquals(JsonToken.END_OBJECT, parser.nextToken());
        parser.close();

        /*
             Finally, test an empty schema, where the header is use to set
             the columns, independently of the reordering flag.
         */
        CsvSchema emptySchema = CsvSchema.builder()
                .setLineSeparator('\n')
                .setUseHeader(true)
                .build();

        parser = factory.createParser(CSV);
        parser.setSchema(emptySchema);
        assertEquals(JsonToken.START_OBJECT, parser.nextToken());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("b", parser.getCurrentName());
        assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("vb", parser.getValueAsString());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.getCurrentName());
        assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("va", parser.getValueAsString());
        assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("c", parser.getCurrentName());
        assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("vc", parser.getValueAsString());
        assertEquals(JsonToken.END_OBJECT, parser.nextToken());
        parser.close();
    }

    public void testColumnFailsOnOutOfOrder() throws IOException {
        CsvFactory factory = new CsvFactory();
        String CSV = "b,a,c\nvb,va,vc\n";

        CsvSchema schema = CsvSchema.builder()
                .addColumn("a")
                .addColumn("b")
                .addColumn("c")
                .setLineSeparator('\n')
                .setUseHeader(true)
                .setStrictHeaders(true)
                .build();

        CsvParser parser = factory.createParser(CSV);
        parser.setSchema(schema);

        try {
            parser.nextToken();
            fail("Should have failed");
        } catch (JsonProcessingException e) {
            verifyException(e, "Expected header a, actual header b");
        }
        parser.close();
    }

    public void testColumnFailsOnTooFew() throws IOException {
        CsvFactory factory = new CsvFactory();
        String CSV = "a,b\nvb,va,vc\n";

        CsvSchema schema = CsvSchema.builder()
                .addColumn("a")
                .addColumn("b")
                .addColumn("c")
                .setLineSeparator('\n')
                .setUseHeader(true)
                .setStrictHeaders(true)
                .build();

        CsvParser parser = factory.createParser(CSV);
        parser.setSchema(schema);

        try {
            parser.nextToken();
            fail("Should have failed");
        } catch (JsonProcessingException e) {
            verifyException(e, "Missing header c");
        }
        parser.close();
    }

    public void testColumnFailsOnTooMany() throws IOException {
        CsvFactory factory = new CsvFactory();
        String CSV = "a,b,c,d\nvb,va,vc\n";

        CsvSchema schema = CsvSchema.builder()
                .addColumn("a")
                .addColumn("b")
                .addColumn("c")
                .setLineSeparator('\n')
                .setUseHeader(true)
                .setStrictHeaders(true)
                .build();

        CsvParser parser = factory.createParser(CSV);
        parser.setSchema(schema);

        try {
            parser.nextToken();
            fail("Should have failed");
        } catch (JsonProcessingException e) {
            verifyException(e, "Extra header d");
        }
        parser.close();
    }

    public void testStrictColumnReturnsExpectedData() throws IOException {
        CsvSchema schema = MAPPER.schemaFor(Point.class).withHeader().withStrictHeaders(true);

        String CSV = "x,y,z\n1,2,3\n4,5,6\n7,8,9";

        final MappingIterator<Point> iter = MAPPER.readerFor(Point.class).with(schema).readValues(CSV);
        final ArrayList<Point> values = Lists.newArrayList(iter);
        assertEquals(3, values.size());
        assertEquals(1, values.get(0).x);
        assertEquals(2, values.get(0).y.intValue());
        assertEquals(3, values.get(0).z.intValue());
        assertEquals(4, values.get(1).x);
        assertEquals(5, values.get(1).y.intValue());
        assertEquals(6, values.get(1).z.intValue());
        assertEquals(7, values.get(2).x);
        assertEquals(8, values.get(2).y.intValue());
        assertEquals(9, values.get(2).z.intValue());
        iter.close();
    }
}
