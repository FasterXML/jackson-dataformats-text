package tools.jackson.dataformat.toml;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ComplianceInvalidTest extends TomlMapperTestBase
{
    private static final ObjectMapper MAPPER = TomlMapper.builder()
            .enable(TomlReadFeature.VALIDATE_DATE_TIME)
            .build();

    @ParameterizedTest
    @MethodSource("data")
    public void tomlTestInvalidCorpus(Path path) {
        assumeTrue(path != null, "Set -Dtoml.corpus.dir=/path/to/toml-test to run TOML corpus tests");
        assertThrows(TomlStreamReadException.class, () -> MAPPER.readTree(path.toFile()));
    }

    public static Stream<Arguments> data() throws Exception {
        Path corpusRoot = ComplianceValidTest.corpusRoot();
        if (corpusRoot == null) {
            return Stream.of(Arguments.of((Path) null));
        }
        return ComplianceValidTest.corpusFiles(corpusRoot, "invalid/").stream()
                .map(Arguments::of);
    }
}
