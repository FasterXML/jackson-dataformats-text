// YAML Main artifact Module descriptor
module tools.jackson.dataformat.yaml
{
    requires tools.jackson.core;
    requires tools.jackson.databind;

    // Jackson 3.0 uses snakeyaml-engine (2.x snakeyaml)
    requires org.snakeyaml.engine.v2;

    exports tools.jackson.dataformat.yaml;
    exports tools.jackson.dataformat.yaml.util;

    provides tools.jackson.core.TokenStreamFactory with
        tools.jackson.dataformat.yaml.YAMLFactory;
    provides tools.jackson.databind.ObjectMapper with
        tools.jackson.dataformat.yaml.YAMLMapper;
}
