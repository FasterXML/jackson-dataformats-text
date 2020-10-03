package com.fasterxml.jackson.dataformat.yaml.ser;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker;

@SuppressWarnings("serial")
public class CustomStringQuoting229Test extends ModuleTestBase
{
    static class CustomChecker extends StringQuotingChecker
    {
        @Override
        public boolean needToQuoteName(String name) {
            return name.isEmpty() || "specialKey".equals(name);
        }

        @Override
        public boolean needToQuoteValue(String value) {
            return value.isEmpty() || "specialValue".equals(value);
        }
    }

    private final ObjectMapper MINIMIZING_MAPPER = YAMLMapper.builder()
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .build();

    private final YAMLMapper CUSTOM_MAPPER = YAMLMapper.builder(
            YAMLFactory.builder()
                .stringQuotingChecker(new CustomChecker())
                .build())
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .build();

    public void testNameQuotingDefault() throws Exception
    {
        // First, default quoting
        assertEquals("key: value",
                _asYaml(MINIMIZING_MAPPER, Collections.singletonMap("key", "value")));

        // 03-Oct-2020, tatu: output here is... weird. Not sure what to make of it
/*       
        assertEquals("\"\": value",
                _asYaml(MINIMIZING_MAPPER, Collections.singletonMap("", "value")));
                */
        assertEquals("\"true\": value",
                _asYaml(MINIMIZING_MAPPER, Collections.singletonMap("true", "value")));
        assertEquals("\"123\": value",
                _asYaml(MINIMIZING_MAPPER, Collections.singletonMap("123", "value")));

        assertEquals("specialKey: value",
                _asYaml(MINIMIZING_MAPPER, Collections.singletonMap("specialKey", "value")));
    }

    public void testNameQuotingCustom() throws Exception
    {
        // Then with custom rules
        assertEquals("key: value",
                _asYaml(CUSTOM_MAPPER, Collections.singletonMap("key", "value")));
        // 03-Oct-2020, tatu: output here is... weird. Not sure what to make of it
/*        assertEquals("\"\": value",
                _asYaml(CUSTOM_MAPPER, Collections.singletonMap("", "value")));
*/
        assertEquals("true: value",
                _asYaml(CUSTOM_MAPPER, Collections.singletonMap("true", "value")));
        assertEquals("123: value",
                _asYaml(CUSTOM_MAPPER, Collections.singletonMap("123", "value")));

        assertEquals("\"specialKey\": value",
                _asYaml(CUSTOM_MAPPER, Collections.singletonMap("specialKey", "value")));
    }

    public void testValueQuotingDefault() throws Exception
    {
        // First, default quoting
        assertEquals("key: value",
                _asYaml(MINIMIZING_MAPPER, Collections.singletonMap("key", "value")));
        assertEquals("key: \"\"",
                _asYaml(MINIMIZING_MAPPER, Collections.singletonMap("key", "")));

        assertEquals("key: \"true\"",
                _asYaml(MINIMIZING_MAPPER, Collections.singletonMap("key", "true")));
        // Currently (2.12.0) number-looking Strings not (yet?) quoted
        assertEquals("key: 123",
                _asYaml(MINIMIZING_MAPPER, Collections.singletonMap("key", "123")));

        assertEquals("key: specialValue",
                _asYaml(MINIMIZING_MAPPER, Collections.singletonMap("key", "specialValue")));
    }

    public void testValueQuotingCustom() throws Exception
    {
        // Then with custom rules
        assertEquals("key: value",
                _asYaml(CUSTOM_MAPPER, Collections.singletonMap("key", "value")));
        assertEquals("key: \"\"",
                _asYaml(CUSTOM_MAPPER, Collections.singletonMap("key", "")));

        assertEquals("key: true",
                _asYaml(CUSTOM_MAPPER, Collections.singletonMap("key", "true")));
        assertEquals("key: 123",
                _asYaml(CUSTOM_MAPPER, Collections.singletonMap("key", "123")));

        assertEquals("key: \"specialValue\"",
                _asYaml(CUSTOM_MAPPER, Collections.singletonMap("key", "specialValue")));
    }

    private String _asYaml(ObjectMapper mapper, Object value) throws Exception
    {
        String yaml = mapper.writeValueAsString(value).trim();
        if (yaml.startsWith("---")) {
            yaml = yaml.substring(3).trim();
        }
        return yaml;
    }
}
