package tools.jackson.dataformat.yaml.deser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLReadFeature;
import tools.jackson.dataformat.yaml.YAMLSchema;

/**
 * Tests for {@link YAMLReadFeature#PARSE_OCTAL_NUMBERS} (issue #276).
 * Uses CORE schema since implicit octal (0-prefixed digits like {@code 0444})
 * is only resolved as {@code Tag.INT} by the CORE schema resolver.
 */
public class ParseOctalNumbers276Test extends ModuleTestBase
{
    // CORE schema with default features (octal enabled)
    private final YAMLMapper CORE_MAPPER = YAMLMapper.builder(
            YAMLFactory.builder()
                    .yamlSchema(YAMLSchema.CORE)
                    .build())
            .build();

    // CORE schema with octal disabled
    private final YAMLMapper CORE_NO_OCTAL_MAPPER = YAMLMapper.builder(
            YAMLFactory.builder()
                    .yamlSchema(YAMLSchema.CORE)
                    .disable(YAMLReadFeature.PARSE_OCTAL_NUMBERS)
                    .build())
            .build();

    // Default behavior: implicit octal interpreted as octal
    @Test
    public void testImplicitOctalEnabledByDefault() throws Exception
    {
        // 0444 octal = 292 decimal
        assertEquals(Integer.valueOf(292), CORE_MAPPER.readValue("0444", Integer.class));
    }

    // With feature disabled: implicit octal treated as decimal
    @Test
    public void testImplicitOctalDisabled() throws Exception
    {
        assertEquals(Integer.valueOf(444), CORE_NO_OCTAL_MAPPER.readValue("0444", Integer.class));
    }

    // Explicit 0o prefix should always be octal regardless of feature
    @Test
    public void testExplicitOctalAlwaysWorks() throws Exception
    {
        // 0o444 octal = 292 decimal
        assertEquals(Integer.valueOf(292), CORE_NO_OCTAL_MAPPER.readValue("0o444", Integer.class));
        assertEquals(Integer.valueOf(292), CORE_MAPPER.readValue("0o444", Integer.class));
    }

    // Negative implicit octal
    @Test
    public void testNegativeImplicitOctal() throws Exception
    {
        // -0444 octal = -292 decimal (default)
        assertEquals(Integer.valueOf(-292), CORE_MAPPER.readValue("-0444", Integer.class));
        // -0444 decimal = -444 (disabled)
        assertEquals(Integer.valueOf(-444), CORE_NO_OCTAL_MAPPER.readValue("-0444", Integer.class));
    }

    // Plain zero should always work
    @Test
    public void testZeroUnaffected() throws Exception
    {
        assertEquals(Integer.valueOf(0), CORE_NO_OCTAL_MAPPER.readValue("0", Integer.class));
        assertEquals(Integer.valueOf(0), CORE_MAPPER.readValue("0", Integer.class));
    }

    // Tree model test (matches the original issue report use case)
    @Test
    public void testTreeModelWithOctalDisabled() throws Exception
    {
        String yaml = "defaultMode: 0444";
        JsonNode node = CORE_NO_OCTAL_MAPPER.readTree(yaml);
        assertEquals(444, node.get("defaultMode").intValue());
    }

    @Test
    public void testTreeModelWithOctalEnabled() throws Exception
    {
        String yaml = "defaultMode: 0444";
        JsonNode node = CORE_MAPPER.readTree(yaml);
        assertEquals(292, node.get("defaultMode").intValue());
    }
}
