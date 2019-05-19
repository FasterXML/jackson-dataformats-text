// Generated 27-Mar-2019 using Moditect maven plugin
module com.fasterxml.jackson.dataformat.yaml {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    // Not sure what canonical module name is for SnakeYAML?
    requires org.yaml.snakeyaml;

    exports com.fasterxml.jackson.dataformat.yaml;
    // probably need to expose this for 2.x?
    exports com.fasterxml.jackson.dataformat.yaml.snakeyaml.error;

    provides com.fasterxml.jackson.core.JsonFactory with
        com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
    provides com.fasterxml.jackson.core.ObjectCodec with
        com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
}
