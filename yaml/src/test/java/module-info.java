// Properties unit test Module descriptor
module tools.jackson.dataformat.yaml
{
    // Since we are not split from Main artifact, will not
    // need to depend on Main artifact -- but need its dependencies

    requires tools.jackson.core;
    requires tools.jackson.databind;

    requires org.snakeyaml.engine.v2;

    // Additional test lib/framework dependencies
    requires org.assertj.core;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;

    // Further, need to open up some packages for JUnit et al
    opens tools.jackson.dataformat.yaml;
    opens tools.jackson.dataformat.yaml.constraints;
    opens tools.jackson.dataformat.yaml.deser;
    opens tools.jackson.dataformat.yaml.filter;
    opens tools.jackson.dataformat.yaml.fuzz;
    opens tools.jackson.dataformat.yaml.misc;
    opens tools.jackson.dataformat.yaml.ser;
    opens tools.jackson.dataformat.yaml.ser.dos;
    opens tools.jackson.dataformat.yaml.testutil;
    opens tools.jackson.dataformat.yaml.testutil.failure;
    opens tools.jackson.dataformat.yaml.tofix;
    opens tools.jackson.dataformat.yaml.type;
}
