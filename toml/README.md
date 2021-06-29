## Overview

[Jackson](/FasterXML/jackson) (Java) data format module that supports reading and writing 
[TOML](https://en.wikipedia.org/wiki/TOML) files.

Support exists both at streaming and databinding level.

Note that while both reading (parsing) and writing (generation) are supported,
writing will currently (2.13) produce less than optimal formatting -- content is
valid and retains information but is typically not "pretty".
This may be improved in future.

## Status

Jackson 2.12.3 was the first beta release; 2.13.0 the official introduction.
2.13 version is still considered somewhat experimental module.

## Maven dependency

To use this extension on Maven-based projects, use following dependency:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.dataformat</groupId>
  <artifactId>jackson-dataformat-toml</artifactId>
  <version>2.13.0</version>
</dependency>
```

# Usage

The typical usage is by using `TomlMapper` in places where you would usually use `JsonMapper`
(and other `ObjectMapper`s) -- but if you want to, it is also possible to use low-level
streaming `TomlFactory` (instead of `JsonFactory`)

```java
TomlMapper mapper = new TomlMapper();
// and then read/write data as usual
SomeType value = ...;
String doc = mapper.writeValueAsBytes(value);
// or
mapper.writeValue(new File("stuff.properties", value);
SomeType otherValue = mapper.readValue(props, SomeType.class);
```

For reading and writing, you most commonly use `ObjectReader` and `ObjectWriter`,
created using mapper object you have; for example:

```java
String props = mapper.writerFor(Pojo.class)
    .writeValueAsString(pojo);
Pojo result = mapper.readerFor(Pojo.class)
    .with(schema) // if customization needed
    .readValue(propsFile);
```
