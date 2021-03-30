module com.fasterxml.jackson.dataformat.toml {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    exports com.fasterxml.jackson.dataformat.toml;

    provides com.fasterxml.jackson.core.TokenStreamFactory with
            com.fasterxml.jackson.dataformat.toml.TomlFactory;
    provides com.fasterxml.jackson.databind.ObjectMapper with
            com.fasterxml.jackson.dataformat.toml.TomlMapper;
}
