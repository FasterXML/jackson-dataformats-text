module tools.jackson.dataformat.yaml {
    requires tools.jackson.core;
    requires tools.jackson.databind;

    // Not sure what canonical module name is for SnakeYAML?
    requires org.yaml.snakeyaml;

    exports tools.jackson.dataformat.yaml;
    exports tools.jackson.dataformat.yaml.util;

    provides tools.jackson.core.TokenStreamFactory with
        tools.jackson.dataformat.yaml.YAMLFactory;
    provides tools.jackson.databind.ObjectMapper with
        tools.jackson.dataformat.yaml.YAMLMapper;
}
