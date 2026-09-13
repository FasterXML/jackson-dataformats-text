package tools.jackson.dataformat.toml.constraints;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.dataformat.toml.TomlFactory;
import tools.jackson.dataformat.toml.TomlMapper;
import tools.jackson.dataformat.toml.TomlMapperTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that {@link StreamReadConstraints} of {@link TomlFactory}
 * are applied by parsers it creates, regardless of {@link ObjectReadContext}
 * used: TOML parses into a tree and exposes it via databind's
 * {@code TreeTraversingParser}, which (before this) only used constraints
 * from the read context.
 *
 * @see <a href="https://github.com/FasterXML/jackson-dataformats-text/issues/430">[dataformats-text#430]</a>
 */
public class TomlTokenCountTest extends TomlMapperTestBase
{
    private final static int MAX_TOKENS = 5;

    // START_OBJECT + 5 x (PROPERTY_NAME, VALUE_NUMBER_INT) + END_OBJECT = 12 tokens
    private final static String DOC = "a = 1\nb = 2\nc = 3\nd = 4\ne = 5\n";

    private final TomlFactory LIMITED_FACTORY = TomlFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxTokenCount(MAX_TOKENS).build())
            .build();

    @Test
    public void testFactoryConstraintsExposed() throws Exception
    {
        try (JsonParser p = LIMITED_FACTORY.createParser(ObjectReadContext.empty(), DOC)) {
            assertSame(LIMITED_FACTORY.streamReadConstraints(), p.streamReadConstraints());
            assertEquals(MAX_TOKENS, p.streamReadConstraints().getMaxTokenCount());
        }
    }

    // Constraints must come from factory, not from (default) read context
    @Test
    public void testTokenCountViaFactory() throws Exception
    {
        _verifyTokenCount(() -> LIMITED_FACTORY.createParser(ObjectReadContext.empty(), DOC));
        _verifyTokenCount(() -> LIMITED_FACTORY.createParser(ObjectReadContext.empty(), new StringReader(DOC)));
        _verifyTokenCount(() -> LIMITED_FACTORY.createParser(ObjectReadContext.empty(),
                new ByteArrayInputStream(DOC.getBytes(StandardCharsets.UTF_8))));
        _verifyTokenCount(() -> LIMITED_FACTORY.createParser(ObjectReadContext.empty(),
                DOC.getBytes(StandardCharsets.UTF_8)));
        _verifyTokenCount(() -> LIMITED_FACTORY.createParser(ObjectReadContext.empty(),
                DOC.toCharArray()));
    }

    @Test
    public void testTokenCountViaMapper() throws Exception
    {
        TomlMapper mapper = newTomlMapper(LIMITED_FACTORY);
        try {
            mapper.readTree(DOC);
            fail("Should not pass");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Token count (" + (MAX_TOKENS + 1) + ")");
        }
        // But fine with default constraints
        assertEquals(5, newTomlMapper().readTree(DOC).size());
    }

    @Test
    public void testWithinLimit() throws Exception
    {
        // START_OBJECT, PROPERTY_NAME, VALUE, END_OBJECT = 4 tokens
        try (JsonParser p = LIMITED_FACTORY.createParser(ObjectReadContext.empty(), "a = 1\n")) {
            int count = 0;
            while (p.nextToken() != null) {
                ++count;
            }
            assertEquals(4, count);
        }
    }

    private interface ParserSupplier { JsonParser get() throws Exception; }

    private void _verifyTokenCount(ParserSupplier s) throws Exception
    {
        try (JsonParser p = s.get()) {
            for (int i = 0; i < MAX_TOKENS; ++i) {
                assertNotNull(p.nextToken());
            }
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.currentToken());
            try {
                p.nextToken();
                fail("Should not pass");
            } catch (StreamConstraintsException e) {
                verifyException(e, "Token count (" + (MAX_TOKENS + 1) + ")");
                verifyException(e, "exceeds the maximum allowed (" + MAX_TOKENS);
            }
        }
    }

    private static void verifyException(Throwable e, String expected) {
        String msg = String.valueOf(e.getMessage());
        assertTrue(msg.contains(expected),
                "Expected message to contain '" + expected + "', got: " + msg);
    }
}
