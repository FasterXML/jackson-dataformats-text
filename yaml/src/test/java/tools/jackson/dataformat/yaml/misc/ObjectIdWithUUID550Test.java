package tools.jackson.dataformat.yaml.misc;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;

import tools.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

// [dataformats-text#550] YAML anchor/alias with @JsonIdentityInfo and UUIDGenerator
public class ObjectIdWithUUID550Test extends ModuleTestBase
{
    // Simpler: no-arg constructors
    @JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class)
    static class AbilitySimple {
        public String name;
        public Map<String, List<ModifierSimple>> modifiers;
    }

    static class ModifierSimple {
        public String name;
        public AbilitySimple parent;
    }

    private final ObjectMapper MAPPER = newObjectMapper();

    // First: verify round-trip works (write then read)
    @Test
    public void testRoundTripWithUUID() throws Exception
    {
        AbilitySimple ability = new AbilitySimple();
        ability.name = "TestAbility";
        ModifierSimple mod = new ModifierSimple();
        mod.name = "TestModifier";
        mod.parent = ability;
        ability.modifiers = Map.of("TestModifier", List.of(mod));

        String yaml = MAPPER.writeValueAsString(ability);

        AbilitySimple result = MAPPER.readValue(yaml, AbilitySimple.class);
        assertNotNull(result);
        assertEquals("TestAbility", result.name);
        assertNotNull(result.modifiers);
        assertSame(result, result.modifiers.get("TestModifier").get(0).parent);
    }

    // Reproduce the exact issue from #550: hand-crafted YAML with UUID anchor
    @Test
    public void testHandCraftedYAMLWithUUIDAnchor() throws Exception
    {
        String yaml = "---\n"
                + "&12345678-aaaa-bbbb-cccc-87654321abcd\n"
                + "name: TestAbility\n"
                + "modifiers:\n"
                + "  TestModifier:\n"
                + "  - name: TestModifier\n"
                + "    parent: *12345678-aaaa-bbbb-cccc-87654321abcd\n";

        AbilitySimple result = MAPPER.readValue(yaml, AbilitySimple.class);
        assertNotNull(result);
        assertEquals("TestAbility", result.name);
        assertNotNull(result.modifiers);
        assertSame(result, result.modifiers.get("TestModifier").get(0).parent);
    }
}
