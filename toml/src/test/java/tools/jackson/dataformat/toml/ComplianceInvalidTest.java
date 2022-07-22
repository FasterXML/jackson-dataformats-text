package tools.jackson.dataformat.toml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ComplianceInvalidTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        Path folder = Paths.get("compliance", "invalid");
        if (!Files.exists(folder)) {
            return Collections.emptyList();
        }
        return Files.walk(folder)
                .filter(Files::isRegularFile)
                .map(p -> new Object[]{p})
                .collect(Collectors.toList());
    }

    private final Path path;

    public ComplianceInvalidTest(Path path) {
        this.path = path;
    }

    @Test(expected = TomlStreamReadException.class)
    public void test() {
        TomlMapper.shared().readTree(path);
    }
}
