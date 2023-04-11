package tools.jackson.dataformat.toml;

public abstract class TomlMapperTestBase {
    protected static TomlFactory newTomlFactory() {
        return TomlFactory.builder().build();
    }

    protected static TomlMapper newTomlMapper() {
        return new TomlMapper(newTomlFactory());
    }

    protected static TomlMapper newTomlMapper(TomlFactory tomlFactory) {
        return new TomlMapper(tomlFactory);
    }
}
