package tools.jackson.dataformat.yaml.constraints;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that {@link StreamReadConstraints#validateIntegerLength(int)} is
 * applied uniformly across the decimal and radix (hex / octal / binary)
 * branches that ultimately route through {@code _decodeBigInt}.
 */
public class IntegerLengthRadixTest extends ModuleTestBase
{
    private final static int MAX_NUM_LEN = 1000;

    private final YAMLMapper MAPPER = new YAMLMapper(
            YAMLFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNumberLength(MAX_NUM_LEN)
                    .build())
                .build());

    // Sanity check: decimal path is already covered by the upstream check,
    // so it should reject literals longer than the configured cap.
    @Test
    public void testDecimalIntegerLengthLimit() throws Exception
    {
        final String digits = "9".repeat(MAX_NUM_LEN + 10);
        final String doc = "n: !!int " + digits + "\n";
        try {
            MAPPER.readTree(doc);
            fail("expected StreamConstraintsException for decimal literal");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length");
            verifyException(e, "exceeds the maximum allowed");
        }
    }

    @Test
    public void testHexIntegerLengthLimit() throws Exception
    {
        final String digits = "f".repeat(MAX_NUM_LEN + 10);
        final String doc = "n: !!int 0x" + digits + "\n";
        try {
            MAPPER.readTree(doc);
            fail("expected StreamConstraintsException for hex literal");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length");
            verifyException(e, "exceeds the maximum allowed");
        }
    }

    @Test
    public void testOctalIntegerLengthLimit() throws Exception
    {
        final String digits = "7".repeat(MAX_NUM_LEN + 10);
        final String doc = "n: !!int 0o" + digits + "\n";
        try {
            MAPPER.readTree(doc);
            fail("expected StreamConstraintsException for octal literal");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length");
            verifyException(e, "exceeds the maximum allowed");
        }
    }

    @Test
    public void testBinaryIntegerLengthLimit() throws Exception
    {
        final String digits = "1".repeat(MAX_NUM_LEN + 10);
        final String doc = "n: !!int 0b" + digits + "\n";
        try {
            MAPPER.readTree(doc);
            fail("expected StreamConstraintsException for binary literal");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length");
            verifyException(e, "exceeds the maximum allowed");
        }
    }
}
