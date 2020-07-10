package com.fasterxml.jackson.dataformat.javaprop;

import java.util.Map;
import java.util.Properties;

public class PrefixTest extends ModuleTestBase
{
    private final JavaPropsMapper MAPPER = newObjectMapper();

    public void testPrefixParsing() throws Exception {
        final String INPUT = "org.o1.firstName=Bob\n"
                +"org.o1.lastName=Palmer\n"
                +"org.o2.firstName=Alice\n"
                +"org.o2.lastName=Black\n"
                +"junk=AQIDBA==\n";
        FiveMinuteUser result1 = _mapFrom(MAPPER.reader(JavaPropsSchema.emptySchema().withPrefix("org.o1")), INPUT, FiveMinuteUser.class, false);
        assertEquals("Bob", result1.firstName);
        assertEquals("Palmer", result1.lastName);
        FiveMinuteUser result2 = _mapFrom(MAPPER.reader(JavaPropsSchema.emptySchema().withPrefix("org.o2")), INPUT, FiveMinuteUser.class, false);
        assertEquals("Alice", result2.firstName);
        assertEquals("Black", result2.lastName);
    }

    public void testPrefixGeneration() throws Exception
    {
        FiveMinuteUser input = new FiveMinuteUser("Bob", "Palmer", true, Gender.MALE,
                new byte[] { 1, 2, 3, 4 });
        String output = MAPPER.writer(JavaPropsSchema.emptySchema().withPrefix("org.o1")).writeValueAsString(input);
        assertEquals("org.o1.firstName=Bob\n"
                +"org.o1.lastName=Palmer\n"
                +"org.o1.gender=MALE\n"
                +"org.o1.verified=true\n"
                +"org.o1.userImage=AQIDBA==\n"
                ,output);
        {
            Properties props = MAPPER.writeValueAsProperties(input, JavaPropsSchema.emptySchema().withPrefix("org.o1"));
            assertEquals(5, props.size());
            assertEquals("true", props.get("org.o1.verified"));
            assertEquals("MALE", props.get("org.o1.gender"));
        }
        {
            Map<String, String> map = MAPPER.writeValueAsMap(input,
                    JavaPropsSchema.emptySchema().withPrefix("org.o1"));
            assertEquals(5, map.size());
            assertEquals("true", map.get("org.o1.verified"));
            assertEquals("MALE", map.get("org.o1.gender"));
        }
    }
}
