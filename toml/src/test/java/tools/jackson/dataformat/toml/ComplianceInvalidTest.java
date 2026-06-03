package tools.jackson.dataformat.toml;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import at.yawk.toml.test.TomlTestCase;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ComplianceInvalidTest extends TomlMapperTestBase
{
    private static final ObjectMapper MAPPER = TomlMapper.builder()
            .enable(TomlReadFeature.VALIDATE_DATE_TIME)
            .build();

    @ParameterizedTest
    @MethodSource("at.yawk.toml.test.TomlTestSuite#invalidToml100")
    public void tomlTestInvalidCorpus(TomlTestCase test) {
        assertThrows(TomlStreamReadException.class, () -> MAPPER.readTree(test.tomlBytes()));
    }
}
