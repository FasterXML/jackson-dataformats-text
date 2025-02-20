package com.fasterxml.jackson.dataformat.toml;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ComplianceInvalidTest extends TomlMapperTestBase {

    private final ObjectMapper MAPPER = newTomlMapper();

    private final Path path;

    public ComplianceInvalidTest(Path path) {
        this.path = path;
    }

    // JUnit 5 throws error when methodSource provides empty `Stream` which
    // seems to always be the case....
    @Disabled
    @MethodSource("data")
    @ParameterizedTest
    public void test(Path path) throws IOException {
        // Test implementation
        assertThrows(
                TomlStreamReadException.class,
                () -> MAPPER.readTree(path.toFile())
        );
    }

    // This is the static method that provides data for the parameterized test
    public static Stream<Object[]> data() throws IOException {
        Path folder = Paths.get("compliance", "invalid");
        if (!Files.exists(folder)) {
            return Stream.empty();  // Ensure this folder exists with files for the test to run
        }
        return Files.walk(folder)
                .filter(Files::isRegularFile)
                .map(p -> new Object[]{p});
    }

}
