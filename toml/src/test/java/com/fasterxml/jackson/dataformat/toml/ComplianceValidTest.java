package com.fasterxml.jackson.dataformat.toml;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ComplianceValidTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        Path folder = Paths.get("compliance", "valid");
        if (!Files.exists(folder)) {
            return Collections.emptyList();
        }
        return Files.walk(folder)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".toml"))
                .map(p -> {
                    String path = p.toString();
                    String withoutSuffix = path.substring(0, path.length() - 5);
                    Path json = Paths.get(withoutSuffix + ".json");
                    if (Files.isRegularFile(json)) {
                        return new Object[]{p, JsonMapper.shared().readTree(json)};
                    }
                    // can't parse inf values :(
                    /*
                    Path yaml = Paths.get(withoutSuffix + ".yaml");
                    if (Files.isRegularFile(yaml)) {
                        return new Object[]{p, YAMLMapper.shared().readTree(yaml)};
                    }
                    */
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private final Path path;
    private final ObjectNode expected;

    public ComplianceValidTest(Path path, ObjectNode expected) {
        this.path = path;
        this.expected = expected;
    }

    @Test
    public void test() {
        JsonNode actual = TomlMapper.builder()
                .enable(TomlReadFeature.PARSE_JAVA_TIME)
                .build().readTree(path);
        Assert.assertEquals(mapFromComplianceNode(expected), actual);
    }

    private static JsonNode mapFromComplianceNode(ObjectNode expected) {
        if (expected.has("type") && expected.has("value")) {
            JsonNode value = expected.get("value");
            switch (expected.get("type").textValue()) {
                case "string":
                    // for some reason, the compliance tests escape these values. this makes some tests fail right now
                    return JsonNodeFactory.instance.textNode(value.textValue());
                case "integer":
                    return JsonNodeFactory.instance.numberNode(new BigInteger(value.textValue()));
                case "float":
                    switch (value.textValue()) {
                        case "inf":
                            return JsonNodeFactory.instance.numberNode(Double.POSITIVE_INFINITY);
                        case "-inf":
                            return JsonNodeFactory.instance.numberNode(Double.NEGATIVE_INFINITY);
                        case "nan":
                            return JsonNodeFactory.instance.numberNode(Double.NaN);
                        default:
                            return JsonNodeFactory.instance.numberNode(new BigDecimal(value.textValue()));
                    }
                case "boolean":
                    return JsonNodeFactory.instance.booleanNode(Boolean.parseBoolean(value.textValue()));
                case "offset datetime":
                    return JsonNodeFactory.instance.pojoNode(OffsetDateTime.parse(value.textValue()));
                case "local datetime":
                case "datetime-local":
                    return JsonNodeFactory.instance.pojoNode(LocalDateTime.parse(value.textValue()));
                case "local date":
                case "date":
                    return JsonNodeFactory.instance.pojoNode(LocalDate.parse(value.textValue()));
                case "local time":
                case "time":
                    return JsonNodeFactory.instance.pojoNode(LocalTime.parse(value.textValue()));
                case "array":
                    ArrayNode array = JsonNodeFactory.instance.arrayNode();
                    for (JsonNode member : value) {
                        array.add(mapFromComplianceNode((ObjectNode) member));
                    }
                    return array;
                default:
                    throw new AssertionError(expected);
            }
        } else {
            ObjectNode object = JsonNodeFactory.instance.objectNode();
            for (Map.Entry<String, JsonNode> field : (Iterable<? extends Map.Entry<String, JsonNode>>) expected::fields) {
                object.set(field.getKey(), mapFromComplianceNode((ObjectNode) field.getValue()));
            }
            return object;
        }
    }
}
