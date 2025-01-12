package tools.jackson.dataformat.javaprop;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class SimpleGenerationTest extends ModuleTestBase
{
    private final JavaPropsMapper MAPPER = newPropertiesMapper();

    @Test
    public void testSimpleEmployee() throws Exception
    {
        FiveMinuteUser input = new FiveMinuteUser("Bob", "Palmer", true, Gender.MALE,
                new byte[] { 1, 2, 3, 4 });
        String output = MAPPER.writeValueAsString(input);
        assertEquals("firstName=Bob\n"
                +"lastName=Palmer\n"
                +"gender=MALE\n"
                +"verified=true\n"
                +"userImage=AQIDBA==\n"
                ,output);
        {
            Properties props = MAPPER.writeValueAsProperties(input);
            assertEquals(5, props.size());
            assertEquals("true", props.get("verified"));
            assertEquals("MALE", props.get("gender"));
        }
        {
            Map<String, String> map = MAPPER.writeValueAsMap(input);
            assertEquals(5, map.size());
            assertEquals("true", map.get("verified"));
            assertEquals("MALE", map.get("gender"));
        }
    }

    @Test
    public void testSimpleRectangle() throws Exception
    {
        Rectangle input = new Rectangle(new Point(1, -2), new Point(5, 10));
        String output = MAPPER.writeValueAsString(input);
        assertEquals("topLeft.x=1\n"
                +"topLeft.y=-2\n"
                +"bottomRight.x=5\n"
                +"bottomRight.y=10\n"
                ,output);
        {
            Properties props = MAPPER.writeValueAsProperties(input);
            assertEquals(4, props.size());
            assertEquals("5", props.get("bottomRight.x"));
        }
        {
            Map<String, String> map = MAPPER.writeValueAsMap(input);
            assertEquals(4, map.size());
            assertEquals("5", map.get("bottomRight.x"));
        }
    }

    @Test
    public void testRectangleWithCustomKeyValueSeparator() throws Exception
    {
        JavaPropsSchema schema = JavaPropsSchema.emptySchema()
                .withKeyValueSeparator(": ");
        Rectangle input = new Rectangle(new Point(1, -2), new Point(5, 10));
        String output = MAPPER.writer(schema).writeValueAsString(input);
        assertEquals("topLeft.x: 1\n"
                +"topLeft.y: -2\n"
                +"bottomRight.x: 5\n"
                +"bottomRight.y: 10\n"
                ,output);
        {
            Properties props = MAPPER.writeValueAsProperties(input, schema);
            assertEquals(4, props.size());
            assertEquals("5", props.get("bottomRight.x"));
        }
        {
            Map<String, String> map = MAPPER.writeValueAsMap(input, schema);
            assertEquals(4, map.size());
            assertEquals("5", map.get("bottomRight.x"));
        }
    }

    @Test
    public void testRectangleWithHeader() throws Exception
    {
        final String HEADER = "# SUPER IMPORTANT!\n";
        JavaPropsSchema schema = JavaPropsSchema.emptySchema()
                .withHeader(HEADER);
        Rectangle input = new Rectangle(new Point(1, -2), new Point(5, 10));
        String output = MAPPER.writer(schema)
                .writeValueAsString(input);
        assertEquals(HEADER
                +"topLeft.x=1\n"
                +"topLeft.y=-2\n"
                +"bottomRight.x=5\n"
                +"bottomRight.y=10\n"
                ,output);
    }

    @Test
    public void testRectangleWithIndent() throws Exception
    {
        JavaPropsSchema schema = JavaPropsSchema.emptySchema()
                .withLineIndentation("  ");
        Rectangle input = new Rectangle(new Point(1, -2), new Point(5, 10));
        String output = MAPPER.writer(schema)
                .writeValueAsString(input);
        assertEquals("  topLeft.x=1\n"
                +"  topLeft.y=-2\n"
                +"  bottomRight.x=5\n"
                +"  bottomRight.y=10\n"
                ,output);
    }
}
