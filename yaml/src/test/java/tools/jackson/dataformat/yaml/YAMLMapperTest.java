package tools.jackson.dataformat.yaml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class YAMLMapperTest extends ModuleTestBase
{
    // for [dataformats-text#581]
    @Test
    void testFormatFeatureDefaults() {
        YAMLMapper mapper = YAMLMapper.shared();
        assertTrue(mapper.isEnabled(YAMLReadFeature.EMPTY_STRING_AS_NULL));
        assertFalse(mapper.isEnabled(YAMLWriteFeature.CANONICAL_OUTPUT));
    }
}
