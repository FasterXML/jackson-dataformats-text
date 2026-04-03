package tools.jackson.dataformat.javaprop.constraints;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.dataformat.javaprop.JavaPropsFactory;
import tools.jackson.dataformat.javaprop.JavaPropsMapper;
import tools.jackson.dataformat.javaprop.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link StreamReadConstraints} enforcement of max name length
 * and max String value length in Properties parsing.
 *
 * @see <a href="https://github.com/FasterXML/jackson-dataformats-text/issues/636">[dataformats-text#636]</a>
 */
public class PropsReadConstraintsTest extends ModuleTestBase
{
    private final static int MAX_NAME_LEN = 100;
    private final static int MAX_STRING_LEN = 200;

    private final JavaPropsMapper NAME_LIMIT_MAPPER = new JavaPropsMapper(
            JavaPropsFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNameLength(MAX_NAME_LEN)
                    .build())
                .build());

    private final JavaPropsMapper STRING_LIMIT_MAPPER = new JavaPropsMapper(
            JavaPropsFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                    .maxStringLength(MAX_STRING_LEN)
                    .build())
                .build());

    @Test
    public void testNameTooLong() throws Exception
    {
        final String longName = "a".repeat(MAX_NAME_LEN + 50);
        Properties props = new Properties();
        props.put(longName, "value");

        try {
            NAME_LIMIT_MAPPER.readPropertiesAs(props, Object.class);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Name length");
            verifyException(e, "exceeds the maximum allowed");
        }
    }

    @Test
    public void testNameWithinLimit() throws Exception
    {
        final String name = "a".repeat(MAX_NAME_LEN);
        Properties props = new Properties();
        props.put(name, "value");

        // Should complete without exception
        NAME_LIMIT_MAPPER.readPropertiesAs(props, Object.class);
    }

    @Test
    public void testStringValueTooLong() throws Exception
    {
        final String longValue = "a".repeat(MAX_STRING_LEN + 50);
        Properties props = new Properties();
        props.put("key", longValue);

        try {
            STRING_LIMIT_MAPPER.readPropertiesAs(props, Object.class);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "String value length");
            verifyException(e, "exceeds the maximum allowed");
        }
    }

    @Test
    public void testStringValueWithinLimit() throws Exception
    {
        final String value = "a".repeat(MAX_STRING_LEN);
        Properties props = new Properties();
        props.put("key", value);

        // Should complete without exception
        STRING_LIMIT_MAPPER.readPropertiesAs(props, Object.class);
    }
}
