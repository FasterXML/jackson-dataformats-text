## Overview

This is a multi-module umbrella project for [Jackson](../../../jackson)
standard text-format dataformat backends.

Dataformat backends are used to support format alternatives to JSON, supported
by default. This is done by sub-classing Jackson core abstractions of:

* All backends sub-class `JsonFactory`, which is factory for:
    * `JsonParser` for reading data (decoding data encoding in supported format)
    * `JsonGenerator` for writing data (encoding data using supported format)
* Some backends sub-class `ObjectMapper` for additional support for databinding


## Textual formats included

Currently included backends are:

* [CSV](csv/)
* [Properties](properties/)
* [YAML](yaml/)

Standard supported formats that are not yet included here (but are likely added
in future)  are:

* [XML](../../jackson-dataformt-xml)

## License

All modules are licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

## Status

[![Build Status](https://travis-ci.org/FasterXML/jackson-dataformat-csv.svg?branch=master)](https://travis-ci.org/FasterXML/jackson-dataformat-csv)

## Maven dependencies

To use these format backends Maven-based projects, use following dependency:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.dataformat</groupId>
  <artifactId>jackson-dataformat-[FORMAT]</artifactId>
  <version>2.9.0</version>
</dependency>
```

where `[FORMAT]` is one of supported modules (`csv`, `properties`, `yaml`)

## More

See [Wiki](../../wiki) for more information (javadocs).
