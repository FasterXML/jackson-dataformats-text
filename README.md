## Overview

This is a multi-module umbrella project for [Jackson](../../../jackson)
standard text-format dataformat backends.

Dataformat backends are used to support format alternatives to JSON, using
general-purpose Jackson API. Formats included allow access using all 3
API styles (streaming, databinding, tree model).

For Jackson 2.x this is done by sub-classing Jackson core abstractions of:

* All backends sub-class `JsonFactory`, which is factory for:
    * `JsonParser` for reading data (decoding data encoding in supported format)
    * `JsonGenerator` for writing data (encoding data using supported format)
* Some backends sub-class `ObjectMapper` for additional support for databinding

there will be some changes (such as introduction of format-specific `ObjectMapper`
sub-classes) in Jackson 3.0.

## Branches

`master` branch is for developing the next major Jackson version -- 3.0 -- but there
are active maintenance branches in which much of development happens:

* `2.10` is for developing the next (and possibly last) minor 2.x version
* `2.9` is for backported fixes for 2.9 patch versions

Older branches are usually not changed but are available for historic reasons.
All released versions have matching git tags (`jackson-dataformats-text-2.9.4`).

Note that since individual format modules used to live in their own repositories,
older branches and tags do not exist in this repository.

## Textual formats included

Currently included backends are:

* [CSV](csv/)
* [Properties](properties/)
* [YAML](yaml/)

Standard supported formats that are not yet included here (but are likely added
in future)  are:

* [XML](../../../jackson-dataformat-xml)

## License

All modules are licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

## Status

[![Build Status](https://travis-ci.org/FasterXML/jackson-dataformats-text.svg?branch=master)](https://travis-ci.org/FasterXML/jackson-dataformats-text)

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
