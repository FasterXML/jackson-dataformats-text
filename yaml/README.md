# Overview

This project contains [Jackson](http://wiki.fasterxml.com/JacksonHome) extension component for reading and writing [YAML](http://en.wikipedia.org/wiki/YAML) encoded data.
[SnakeYAML](https://bitbucket.org/asomov/snakeyaml/) library is used for low-level YAML parsing.
This project adds necessary abstractions on top to make things work with other Jackson functionality.

Project is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

# Status

[![Build Status](https://travis-ci.org/FasterXML/jackson-dataformat-yaml.svg?branch=master)](https://travis-ci.org/FasterXML/jackson-dataformat-yaml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.dataformat/jackson-dataformat-yaml/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.dataformat/jackson-dataformat-yaml/)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.fasterxml.jackson.dataformat/jackson-dataformat-yaml/badge.svg)](http://www.javadoc.io/doc/com.fasterxml.jackson.dataformat/jackson-dataformat-yaml)

Module has been production ready since version 2.5.

## Maven dependency

To use this extension on Maven-based projects, use following dependency:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.dataformat</groupId>
  <artifactId>jackson-dataformat-yaml</artifactId>
  <version>2.14.2</version>
</dependency>
```

# Usage

## Simple usage

Usage is through basic `JsonFactory` and/or `ObjectMapper` API but you will construct instances differently:

```java
// Mapper with default configuration
ObjectMapper mapper = new YAMLMapper();
User user = mapper.readValue(yamlSource, User.class);

// Or using builder
ObjectMapper mapper = YAMLMapper.builder()
   .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
   .build();
Json

```

but you can also just use underlying `YAMLFactory` and parser it produces, for event-based processing:

```java
YAMLFactory factory = new YAMLFactory();
JsonParser parser = factory.createParser(yamlString); // don't be fooled by method name...
while (parser.nextToken() != null) {
  // do something!
}
```

## Configuration

Most configuration is applied during mapper instance configuration, through
`YAMLMapper.Builder`, similar to how JSON-based plain `ObjectMapper` is configured.

### SnakeYAML Configuration

Since jackson-dataformat-yaml 2.14, it is possible to configure the underlying [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml/wiki/Home) behavior.

If you are parsing YAML, you might consider configuring the
[LoaderOptions](https://www.javadoc.io/doc/org.yaml/snakeyaml/latest/org/yaml/snakeyaml/LoaderOptions.html).
See the related 'Known Problems' section below to see an example of how to do this. As well as configuring the
'codePointLimit', you might also want to configure the 'nestingDepthLimit'.

If you are generating YAML, you can also control the underlying SnakeYAML behavior by
setting the [DumperOptions](https://www.javadoc.io/doc/org.yaml/snakeyaml/latest/org/yaml/snakeyaml/DumperOptions.html)
on the [YAMLFactory.builder()](https://javadoc.io/static/com.fasterxml.jackson.dataformat/jackson-dataformat-yaml/2.14.2/com/fasterxml/jackson/dataformat/yaml/YAMLFactoryBuilder.html).

## Known Problems

### Maximum input YAML document size (3 MB)

SnakeYAML implementation (that Jackson uses for low-level encoding and decoding) starts imposing the default limit of 3 megabyte document size as of version 1.32, used by Jackson 2.14 (and later).
If you hit this limitation, you need to explicitly increase the limit by configuring `YAMLFactory` and constructing `YAMLMapper` with that:

```java
LoaderOptions loaderOptions = new LoaderOptions();
loaderOptions.setCodePointLimit(10 * 1024 * 1024); // 10 MB
YAMLFactory yamlFactory = YAMLFactory.builder()
    .loaderOptions(loaderOptions)
    .build();
YAMLMapper mapper = new YAMLMapper(yamlFactory);
```

# Documentation

* [Wiki](../../../wiki) contains links to Javadocs, external documentation
