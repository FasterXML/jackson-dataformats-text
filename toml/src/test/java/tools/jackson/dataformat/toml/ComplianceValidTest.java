package tools.jackson.dataformat.toml;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import at.yawk.toml.test.TomlTestCase;

import tools.jackson.core.io.NumberInput;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeCreator;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

public class ComplianceValidTest extends TomlMapperTestBase
{
    private static final ObjectMapper TOML_MAPPER = TomlMapper.builder()
            .enable(TomlReadFeature.PARSE_JAVA_TIME)
            .build();
    private static final ObjectMapper JSON_MAPPER = JsonMapper.shared();

    @ParameterizedTest
    @MethodSource("at.yawk.toml.test.TomlTestSuite#validToml100")
    public void tomlTestValidCorpus(TomlTestCase test) throws Exception {
        String expectedJson = test.expectedJson();
        assertNotNull(expectedJson, "valid TOML test must have expected JSON");

        JsonNode expected = mapFromComplianceNode(JSON_MAPPER.readTree(expectedJson));
        JsonNode actual = TOML_MAPPER.readTree(test.tomlBytes());
        assertTrue(semanticallyEquals(expected, actual),
                "expected=" + expected + " actual=" + actual);
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
