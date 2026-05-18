package tools.jackson.dataformat.toml;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.core.io.NumberInput;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeCreator;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ComplianceValidTest extends TomlMapperTestBase
{
    private static final ObjectMapper TOML_MAPPER = TomlMapper.builder()
            .enable(TomlReadFeature.PARSE_JAVA_TIME)
            .build();
    private static final ObjectMapper JSON_MAPPER = JsonMapper.shared();

    @ParameterizedTest
    @MethodSource("data")
    public void tomlTestValidCorpus(Path toml, Path json) throws Exception {
        assumeTrue(toml != null, "Set -Dtoml.corpus.dir=/path/to/toml-test to run TOML corpus tests");

        JsonNode expected = mapFromComplianceNode(JSON_MAPPER.readTree(json.toFile()));
        JsonNode actual = TOML_MAPPER.readTree(toml.toFile());
        assertTrue(semanticallyEquals(expected, actual),
                "expected=" + expected + " actual=" + actual);
    }

    public static Stream<Arguments> data() throws Exception {
        Path corpusRoot = corpusRoot();
        if (corpusRoot == null) {
            return Stream.of(Arguments.of(null, null));
        }
        return corpusFiles(corpusRoot, "valid/").stream()
                .map(toml -> Arguments.of(toml, expectedJson(toml)))
                .filter(args -> Files.isRegularFile((Path) args.get()[1]));
    }

    static Path corpusRoot() {
        String root = System.getProperty("toml.corpus.dir");
        if (root == null || root.isBlank()) {
            return null;
        }
        return Paths.get(root);
    }

    static List<Path> corpusFiles(Path corpusRoot, String prefix) throws IOException {
        Path testsRoot = corpusRoot.resolve("tests");
        Path fileList = testsRoot.resolve(System.getProperty("toml.corpus.fileList", "files-toml-1.0.0"));
        List<Path> files = new ArrayList<>();
        for (String line : Files.readAllLines(fileList)) {
            if (line.isBlank() || line.startsWith("#") || !line.endsWith(".toml") || !line.startsWith(prefix)) {
                continue;
            }
            files.add(testsRoot.resolve(line));
        }
        return files;
    }

    private static Path expectedJson(Path toml) {
        String fileName = toml.getFileName().toString();
        return toml.resolveSibling(fileName.substring(0, fileName.length() - 5) + ".json");
    }

    private static JsonNode mapFromComplianceNode(JsonNode expected) {
        final JsonNodeCreator nodeF = JsonMapper.shared().createObjectNode();
        if (expected.isObject()) {
            ObjectNode expectedObject = (ObjectNode) expected;
            if (expectedObject.has("type") && expectedObject.has("value")) {
                JsonNode value = expectedObject.get("value");
                switch (expectedObject.get("type").stringValue()) {
                    case "string":
                        return nodeF.stringNode(value.stringValue());
                    case "integer":
                        return nodeF.numberNode(NumberInput.parseBigInteger(value.stringValue(), false));
                    case "float":
                        switch (value.stringValue()) {
                            case "inf":
                                return nodeF.numberNode(Double.POSITIVE_INFINITY);
                            case "-inf":
                                return nodeF.numberNode(Double.NEGATIVE_INFINITY);
                            case "nan":
                                return nodeF.numberNode(Double.NaN);
                            default:
                                return nodeF.numberNode(NumberInput.parseBigDecimal(value.stringValue(), false));
                        }
                    case "bool":
                    case "boolean":
                        return nodeF.booleanNode(Boolean.parseBoolean(value.stringValue()));
                    case "datetime":
                    case "offset datetime":
                        return nodeF.pojoNode(OffsetDateTime.parse(value.stringValue()));
                    case "datetime-local":
                    case "local datetime":
                        return nodeF.pojoNode(LocalDateTime.parse(value.stringValue()));
                    case "date":
                    case "date-local":
                    case "local date":
                        return nodeF.pojoNode(LocalDate.parse(value.stringValue()));
                    case "time":
                    case "time-local":
                    case "local time":
                        return nodeF.pojoNode(LocalTime.parse(value.stringValue()));
                    case "array":
                        return mapFromComplianceNode(value);
                    default:
                        throw new AssertionError(expectedObject);
                }
            }
            ObjectNode object = expectedObject.objectNode();
            for (Map.Entry<String, JsonNode> field : expectedObject.properties()) {
                object.set(field.getKey(), mapFromComplianceNode(field.getValue()));
            }
            return object;
        }
        if (expected.isArray()) {
            ArrayNode array = JsonMapper.shared().createArrayNode();
            for (JsonNode member : expected) {
                array.add(mapFromComplianceNode(member));
            }
            return array;
        }
        throw new AssertionError(expected);
    }

    private static boolean semanticallyEquals(JsonNode expected, JsonNode actual) {
        if (expected.isNumber() && actual.isNumber()) {
            // Compare integrals via BigInteger first: doubleValue() on a large
            // BigInteger/BigDecimal overflows to Infinity, which would otherwise
            // make two distinct large values compare equal via the non-finite path.
            if (expected.isIntegralNumber() && actual.isIntegralNumber()) {
                return toBigInteger(expected).equals(toBigInteger(actual));
            }
            // Only Float/Double nodes can hold NaN/±Infinity.
            if (expected.isFloat() || expected.isDouble()
                    || actual.isFloat() || actual.isDouble()) {
                double e = expected.doubleValue();
                double a = actual.doubleValue();
                if (!Double.isFinite(e) || !Double.isFinite(a)) {
                    return Double.compare(e, a) == 0;
                }
            }
            return toBigDecimal(expected).compareTo(toBigDecimal(actual)) == 0;
        }
        if (expected.isObject() && actual.isObject()) {
            if (expected.size() != actual.size()) {
                return false;
            }
            for (String name : expected.propertyNames()) {
                JsonNode actualValue = actual.get(name);
                if (actualValue == null || !semanticallyEquals(expected.get(name), actualValue)) {
                    return false;
                }
            }
            return true;
        }
        if (expected.isArray() && actual.isArray()) {
            if (expected.size() != actual.size()) {
                return false;
            }
            for (int i = 0; i < expected.size(); i++) {
                if (!semanticallyEquals(expected.get(i), actual.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return expected.equals(actual);
    }

    private static BigInteger toBigInteger(JsonNode node) {
        if (node.isBigInteger()) {
            return node.bigIntegerValue();
        }
        return BigInteger.valueOf(node.longValue());
    }

    private static BigDecimal toBigDecimal(JsonNode node) {
        if (node.isBigDecimal()) {
            return node.decimalValue();
        }
        if (node.isIntegralNumber()) {
            return new BigDecimal(toBigInteger(node));
        }
        return BigDecimal.valueOf(node.doubleValue());
    }
}
