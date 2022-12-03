package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.io.NumberInput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.UncheckedIOException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
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
                        try {
                            return new Object[]{p, new JsonMapper().readTree(json.toFile())};
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
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
    public void test() throws IOException {
        JsonNode actual = TomlMapper.builder()
                .enable(TomlReadFeature.PARSE_JAVA_TIME)
                .build().readTree(path.toFile());
        Assert.assertEquals(mapFromComplianceNode(expected), actual);
    }

    private static JsonNode mapFromComplianceNode(ObjectNode expected) {
        final JsonNodeCreator nodeF = expected;
        if (expected.has("type") && expected.has("value")) {
            JsonNode value = expected.get("value");
            switch (expected.get("type").textValue()) {
                case "string":
                    // for some reason, the compliance tests escape these values. this makes some tests fail right now
                    return nodeF.textNode(value.textValue());
                case "integer":
                    return nodeF.numberNode(NumberInput.parseBigInteger(value.textValue()));
                case "float":
                    switch (value.textValue()) {
                        case "inf":
                            return nodeF.numberNode(Double.POSITIVE_INFINITY);
                        case "-inf":
                            return nodeF.numberNode(Double.NEGATIVE_INFINITY);
                        case "nan":
                            return nodeF.numberNode(Double.NaN);
                        default:
                            return nodeF.numberNode(NumberInput.parseBigDecimal(value.textValue()));
                    }
                case "boolean":
                    return nodeF.booleanNode(Boolean.parseBoolean(value.textValue()));
                case "offset datetime":
                    return nodeF.pojoNode(OffsetDateTime.parse(value.textValue()));
                case "local datetime":
                case "datetime-local":
                    return nodeF.pojoNode(LocalDateTime.parse(value.textValue()));
                case "local date":
                case "date":
                    return nodeF.pojoNode(LocalDate.parse(value.textValue()));
                case "local time":
                case "time":
                    return nodeF.pojoNode(LocalTime.parse(value.textValue()));
                case "array":
                    ArrayNode array = nodeF.arrayNode();
                    for (JsonNode member : value) {
                        array.add(mapFromComplianceNode((ObjectNode) member));
                    }
                    return array;
                default:
                    throw new AssertionError(expected);
            }
        } else {
            ObjectNode object = expected.objectNode();
            for (Map.Entry<String, JsonNode> field : (Iterable<? extends Map.Entry<String, JsonNode>>) expected::fields) {
                object.set(field.getKey(), mapFromComplianceNode((ObjectNode) field.getValue()));
            }
            return object;
        }
    }
}
