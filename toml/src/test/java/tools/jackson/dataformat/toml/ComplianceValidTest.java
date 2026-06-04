package tools.jackson.dataformat.toml;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import at.yawk.toml.test.TomlExpectedDocumentValidator;
import at.yawk.toml.test.TomlTestCase;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class ComplianceValidTest extends TomlMapperTestBase
{
    private static final ObjectMapper TOML_MAPPER = TomlMapper.builder()
            .enable(TomlReadFeature.PARSE_JAVA_TIME)
            .build();
    private static final ObjectMapper JSON_MAPPER = JsonMapper.shared();
    private static final TomlExpectedDocumentValidator VALIDATOR = new JacksonTomlExpectedDocumentValidator();

    @ParameterizedTest
    @MethodSource("at.yawk.toml.test.TomlTestSuite#validToml110")
    public void tomlTestValidCorpus(TomlTestCase test) throws Exception {
        JsonNode actual = TOML_MAPPER.readTree(test.tomlBytes());
        VALIDATOR.validate(test, JSON_MAPPER.convertValue(actual, new TypeReference<Map<String, ?>>() { }));
    }

    private static final class JacksonTomlExpectedDocumentValidator extends TomlExpectedDocumentValidator {
        @Override
        protected Map<String, ?> parseExpectedJson(String expectedJson) {
            try {
                return JSON_MAPPER.readValue(expectedJson, new TypeReference<Map<String, ?>>() { });
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse expected JSON", e);
            }
        }

        @Override
        protected void validateOffsetDateTime(String path, String expected, Object actual) {
            OffsetDateTime expectedValue = OffsetDateTime.parse(expected);
            if (actual instanceof OffsetDateTime actualOffsetDateTime && expectedValue.equals(actualOffsetDateTime)) {
                return;
            }
            if (actual instanceof String actualString && expectedValue.equals(OffsetDateTime.parse(actualString))) {
                return;
            }
            throw new AssertionError(path + ": expected offset date-time " + expected + " but was " + actual);
        }

        @Override
        protected void validateLocalDateTime(String path, String expected, Object actual) {
            LocalDateTime expectedValue = LocalDateTime.parse(expected);
            if (actual instanceof LocalDateTime actualLocalDateTime && expectedValue.equals(actualLocalDateTime)) {
                return;
            }
            if (actual instanceof String actualString && expectedValue.equals(LocalDateTime.parse(actualString))) {
                return;
            }
            throw new AssertionError(path + ": expected local date-time " + expected + " but was " + actual);
        }

        @Override
        protected void validateLocalDate(String path, String expected, Object actual) {
            LocalDate expectedValue = LocalDate.parse(expected);
            if (actual instanceof LocalDate actualLocalDate && expectedValue.equals(actualLocalDate)) {
                return;
            }
            if (actual instanceof String actualString && expectedValue.equals(LocalDate.parse(actualString))) {
                return;
            }
            throw new AssertionError(path + ": expected local date " + expected + " but was " + actual);
        }

        @Override
        protected void validateLocalTime(String path, String expected, Object actual) {
            LocalTime expectedValue = LocalTime.parse(expected);
            if (actual instanceof LocalTime actualLocalTime && expectedValue.equals(actualLocalTime)) {
                return;
            }
            if (actual instanceof String actualString && expectedValue.equals(LocalTime.parse(actualString))) {
                return;
            }
            throw new AssertionError(path + ": expected local time " + expected + " but was " + actual);
        }
    }
}
