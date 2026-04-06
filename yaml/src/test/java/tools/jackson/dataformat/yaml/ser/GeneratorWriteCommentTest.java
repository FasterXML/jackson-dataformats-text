package tools.jackson.dataformat.yaml.ser;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLGenerator;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link YAMLGenerator#writeComment(String)}.
 *
 * @since 3.2
 */
public class GeneratorWriteCommentTest extends ModuleTestBase
{
    private final YAMLMapper MAPPER = newObjectMapper();

    @Test
    public void testSimpleBlockComment() throws Exception
    {
        StringWriter w = new StringWriter();
        YAMLGenerator gen = (YAMLGenerator) MAPPER.createGenerator(w);
        gen.writeComment(" This is a comment");
        gen.writeStartObject();
        gen.writeName("key");
        gen.writeString("value");
        gen.writeEndObject();
        gen.close();

        String yaml = w.toString();
        assertTrue(yaml.contains("# This is a comment"),
                "Expected block comment in output, got: " + yaml);
        assertTrue(yaml.contains("key: value") || yaml.contains("key: \"value\""),
                "Expected key-value pair in output, got: " + yaml);
    }

    @Test
    public void testMultilineComment() throws Exception
    {
        StringWriter w = new StringWriter();
        YAMLGenerator gen = (YAMLGenerator) MAPPER.createGenerator(w);
        gen.writeStartObject();
        gen.writeComment(" Line one\n Line two");
        gen.writeName("key");
        gen.writeString("value");
        gen.writeEndObject();
        gen.close();

        String yaml = w.toString();
        assertTrue(yaml.contains("# Line one"),
                "Expected first comment line, got: " + yaml);
        assertTrue(yaml.contains("# Line two"),
                "Expected second comment line, got: " + yaml);
    }

    @Test
    public void testMultilineCommentCR() throws Exception
    {
        StringWriter w = new StringWriter();
        YAMLGenerator gen = (YAMLGenerator) MAPPER.createGenerator(w);
        gen.writeStartObject();
        gen.writeComment(" Line one\r Line two");
        gen.writeName("key");
        gen.writeString("value");
        gen.writeEndObject();
        gen.close();

        String yaml = w.toString();
        assertTrue(yaml.contains("# Line one"),
                "Expected first comment line, got: " + yaml);
        assertTrue(yaml.contains("# Line two"),
                "Expected second comment line, got: " + yaml);
    }

    @Test
    public void testMultilineCommentCRLF() throws Exception
    {
        StringWriter w = new StringWriter();
        YAMLGenerator gen = (YAMLGenerator) MAPPER.createGenerator(w);
        gen.writeStartObject();
        gen.writeComment(" Line one\r\n Line two");
        gen.writeName("key");
        gen.writeString("value");
        gen.writeEndObject();
        gen.close();

        String yaml = w.toString();
        assertTrue(yaml.contains("# Line one"),
                "Expected first comment line, got: " + yaml);
        assertTrue(yaml.contains("# Line two"),
                "Expected second comment line, got: " + yaml);
    }

    @Test
    public void testCommentBeforeField() throws Exception
    {
        StringWriter w = new StringWriter();
        YAMLGenerator gen = (YAMLGenerator) MAPPER.createGenerator(w);
        gen.writeStartObject();
        gen.writeComment(" The hostname to connect to");
        gen.writeName("host");
        gen.writeString("localhost");
        gen.writeComment(" The port number");
        gen.writeName("port");
        gen.writeNumber(8080);
        gen.writeEndObject();
        gen.close();

        String yaml = w.toString();
        assertTrue(yaml.contains("# The hostname to connect to"),
                "Expected first comment in output, got: " + yaml);
        assertTrue(yaml.contains("# The port number"),
                "Expected second comment in output, got: " + yaml);
    }

    @Test
    public void testNullCommentEmitsBlankLine() throws Exception
    {
        StringWriter w = new StringWriter();
        YAMLGenerator gen = (YAMLGenerator) MAPPER.createGenerator(w);
        gen.writeStartObject();
        gen.writeComment(" group one");
        gen.writeName("a");
        gen.writeNumber(1);
        gen.writeComment(null);
        gen.writeComment(" group two");
        gen.writeName("b");
        gen.writeNumber(2);
        gen.writeEndObject();
        gen.close();

        String yaml = w.toString();
        // Null comment should produce a blank line separating the two groups
        assertTrue(yaml.contains("# group one"),
                "Expected first group comment, got: " + yaml);
        assertTrue(yaml.contains("# group two"),
                "Expected second group comment, got: " + yaml);
        // Verify blank line exists between the two groups
        assertTrue(yaml.contains("\n\n"),
                "Expected blank line from null comment, got: " + yaml);
    }

    @Test
    public void testHeaderComment() throws Exception
    {
        StringWriter w = new StringWriter();
        YAMLGenerator gen = (YAMLGenerator) MAPPER.createGenerator(w);
        gen.writeComment(" SPDX-License-Identifier: Apache-2.0");
        gen.writeStartObject();
        gen.writeName("name");
        gen.writeString("test");
        gen.writeEndObject();
        gen.close();

        String yaml = w.toString();
        assertTrue(yaml.contains("# SPDX-License-Identifier: Apache-2.0"),
                "Expected license header comment, got: " + yaml);
    }

    // Verify that comment written after writeName() becomes an inline comment
    @Test
    public void testInlineCommentAfterKey() throws Exception
    {
        StringWriter w = new StringWriter();
        YAMLGenerator gen = (YAMLGenerator) MAPPER.createGenerator(w);
        gen.writeStartObject();
        gen.writeName("host");
        gen.writeComment(" server hostname");
        gen.writeString("localhost");
        gen.writeEndObject();
        gen.close();

        String yaml = w.toString();
        // Inline comment should appear on the same line as the key
        assertTrue(yaml.contains("host: # server hostname"),
                "Expected inline comment after key, got: " + yaml);
    }

    // Verify that comment written before writeName() is a block comment
    @Test
    public void testBlockCommentBeforeKey() throws Exception
    {
        StringWriter w = new StringWriter();
        YAMLGenerator gen = (YAMLGenerator) MAPPER.createGenerator(w);
        gen.writeStartObject();
        gen.writeComment(" server hostname");
        gen.writeName("host");
        gen.writeString("localhost");
        gen.writeEndObject();
        gen.close();

        String yaml = w.toString();
        // Block comment should be on its own line, before the key
        String trimmed = yaml.trim();
        int commentIdx = trimmed.indexOf("# server hostname");
        int keyIdx = trimmed.indexOf("host:");
        assertTrue(commentIdx >= 0 && keyIdx > commentIdx,
                "Expected block comment before key, got: " + yaml);
    }
}
