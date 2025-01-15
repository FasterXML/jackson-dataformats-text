// TOML Main artifact Module descriptor
module tools.jackson.dataformat.toml
{
    requires tools.jackson.core;
    requires tools.jackson.databind;

    exports tools.jackson.dataformat.toml;

    provides tools.jackson.core.TokenStreamFactory with
            tools.jackson.dataformat.toml.TomlFactory;
    provides tools.jackson.databind.ObjectMapper with
            tools.jackson.dataformat.toml.TomlMapper;
}
