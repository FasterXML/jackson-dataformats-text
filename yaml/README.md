# Overview

This project contains [Jackson](../../../../jackson) extension component for reading and writing [YAML](http://en.wikipedia.org/wiki/YAML) encoded data.
Since version 3.0, [SnakeYAML Engine](https://bitbucket.org/snakeyaml/snakeyaml-engine/) is used for low-level YAML parsing.
For Jackson 2.x instructions, see the [2.x README](https://github.com/FasterXML/jackson-dataformats-text/blob/2.x/yaml/README.md).
This project adds necessary abstractions on top to make things work with other Jackson functionality.

Project is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

# Status

[![Build Status](https://github.com/FasterXML/jackson-dataformats-text/actions/workflows/main.yml/badge.svg?branch=3.x)](https://github.com/FasterXML/jackson-dataformats-text/actions?query=branch%3A3.x)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/tools.jackson.dataformat/jackson-dataformat-yaml/badge.svg)](https://maven-badges.herokuapp.com/maven-central/tools.jackson.dataformat/jackson-dataformat-yaml/)
[![Javadoc](https://javadoc.io/badge/tools.jackson.dataformat/jackson-dataformat-yaml.svg)](https://javadoc.io/doc/tools.jackson.dataformat/jackson-dataformat-yaml)

Module has been production ready since version 2.5.

## Maven dependency

Use Jackson 3.x coordinates in Maven. For example, with version 3.3.0:

```xml
<dependency>
  <groupId>tools.jackson.dataformat</groupId>
  <artifactId>jackson-dataformat-yaml</artifactId>
  <version>3.3.0</version>
</dependency>
```

# Usage

## Simple usage

Use `YAMLMapper` for data binding and tree-model access:

```java
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

// Mapper with default configuration
ObjectMapper mapper = new YAMLMapper();

// POJO example using a local record
record User(String name) {}

String yaml = "name: John Doe\n";
User user = mapper.readValue(yaml, User.class);
String output = mapper.writeValueAsString(new User("Jane Doe"));

// Or using builder with feature disabled
ObjectMapper noMarkerMapper = YAMLMapper.builder()
        .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
        .build();
```

The mapper can also create a streaming parser for event-based processing:

```java
import tools.jackson.core.JsonParser;
import tools.jackson.dataformat.yaml.YAMLMapper;

YAMLMapper mapper = new YAMLMapper();
try (JsonParser parser = mapper.createParser("name: Ada\n")) {
    while (parser.nextToken() != null) {
        // do something!
    }
}
```

## Configuration

Most configuration is applied during mapper instance configuration, through
`YAMLMapper.Builder`, similar to how JSON-based plain `ObjectMapper` is configured.

### SnakeYAML Engine Configuration

Since Jackson 3.x, the underlying SnakeYAML Engine library can be configured.

If you are parsing YAML, you can configure the [LoadSettings](https://javadoc.io/doc/org.snakeyaml/snakeyaml-engine/latest/org/snakeyaml/engine/v2/api/LoadSettings.html).
See the related 'Known Problems' section below for an example. `LoadSettings` controls the underlying parser settings; Jackson read constraints are configured separately.

If you are generating YAML, you can also control the underlying SnakeYAML Engine behavior by
setting the [DumpSettings](https://javadoc.io/doc/org.snakeyaml/snakeyaml-engine/latest/org/snakeyaml/engine/v2/api/DumpSettings.html)
on the [YAMLFactory.builder()](https://javadoc.io/doc/tools.jackson.dataformat/jackson-dataformat-yaml/latest/tools/jackson/dataformat/yaml/YAMLFactoryBuilder.html).

When configuring reading, use `YAMLFactory.builder().loadSettings(customLoadSettings)`.
When configuring writing, use `YAMLFactory.builder().dumperOptions(customDumpSettings)`.
Explicit `LoadSettings` take precedence over `yamlSchema`. To combine custom settings with Core schema, set an `org.snakeyaml.engine.v2.schema.CoreSchema` on the `LoadSettings` builder using `setSchema(...)`.

### YAML 1.2 Core Schema

Since 3.2, `YAMLFactory.builder().yamlSchema(YAMLSchema.CORE)` enables YAML 1.2 Core schema parsing. For example, `~` is parsed as null and `null` is also null, but a quoted `"~"` remains a string. See [YAMLSchema Javadoc](https://javadoc.io/doc/tools.jackson.dataformat/jackson-dataformat-yaml/latest/tools/jackson/dataformat/yaml/YAMLSchema.html) for details on other schema differences, including octal handling.
Note: Core schema is not full YAML 1.1 compatibility.

```java
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLSchema;
import tools.jackson.dataformat.yaml.YAMLMapper;

YAMLFactory factory = YAMLFactory.builder()
        .yamlSchema(YAMLSchema.CORE)
        .build();
YAMLMapper mapper = new YAMLMapper(factory);

JsonNode node = mapper.readTree("state: ~\nquoted: \"~\"\n");
System.out.println("state: " + node.get("state").isNull() + ", quoted: " + node.get("quoted").isTextual());
YAMLMapper defaultMapper = new YAMLMapper();
System.out.println(defaultMapper.readTree("state: ~\n").get("state").isTextual()); // true
```

## Known Problems

### Maximum input YAML document size

SnakeYAML Engine imposes a default limit on the number of code points in a YAML document.
If you hit this limitation, you need to explicitly increase the limit by configuring `LoadSettings` and setting it on `YAMLFactory` (and then constructing `YAMLMapper` with that factory). Note that Jackson's `StreamReadConstraints` still apply independently.

```java
import org.snakeyaml.engine.v2.api.LoadSettings;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

LoadSettings loadSettings = LoadSettings.builder()
        .setCodePointLimit(10 * 1024 * 1024) // 10485760 code points
        .build();
YAMLFactory yamlFactory = YAMLFactory.builder()
        .loadSettings(loadSettings)
        .build();
YAMLMapper mapper = new YAMLMapper(yamlFactory);
```

# Documentation

* [Wiki](../../../wiki) contains links to Javadocs, external documentation
