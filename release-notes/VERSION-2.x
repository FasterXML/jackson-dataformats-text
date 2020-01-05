Project: jackson-datatypes-text
Modules:
  jackson-dataformat-csv
  jackson-dataformat-properties
  jackson-dataformat-yaml

------------------------------------------------------------------------
=== Releases ===
------------------------------------------------------------------------

2.11.0 (not yet released)

#7: Add `CsvParser.Feature.EMPTY_STRING_AS_NULL` to allow coercing empty Strings
  into `null` values
 (contributed by Tyler C-R)
#115: JsonProperty index is not honored by CsvSchema builder
 -- actually fixed by [databind#2555]

2.10.2 (05-Jan-2020)

#163: (yaml) `SequenceWriter` does not create multiple docs in a single yaml file
 (reported by Piyush K)
#166: (csv) Incorrect `JsonParseException` Message for missing separator char
 (reported by gimiz@github)

2.10.1 (09-Nov-2019)

#15: Add a `CsvParser.Feature.SKIP_EMPTY_LINES` to allow skipping empty rows
 (implemented by Vincent B)

2.10.0 (26-Sep-2019)

#50: (yaml) Empty string serialized without quotes if MINIMIZE_QUOTES is enabled
 (reported by tim-palmer@github)
#83: (yaml) Update index of sequence context
 (reported by Matti B; fix contributed by Maarten W)
#100: (properties) Add an option to specify properties prefix
 (contributed by Alon B-L)
#101: (yaml) Use latest SnakeYAML version 1.24 and get rid of deprecated methods
 (contributed by Andrey S)
#108: (csv) Add new `CsvParser.Feature.ALLOW_COMMENTS` to replace use of deprecated
 `JsonParser.Feature.ALLOW_YAML_COMMENTS`
#116: (yaml) Error handling "null" String when Feature.MINIMIZE_QUOTES is active
 (reported by Stefan L)
#129: (yaml) Convert YAML string issue
 (fix contributed by Guillaume S)
#134: (csv) `CSVParserBootstrapper` creates `UTF8Reader` which is incorrectly not auto-closed
 (reported by iuliu-b@github)
#139: (properties) Support for Map<String, String> in `JavaPropsMapper`
 (suggested by Filip H)
#140: (yaml) Implement `JsonGenerator.writeFieldId(...)` for `YAMLGenerator`
- Add JDK9+ module info using Moditect plugin

2.9.10 (21-Sep-2019)

No changes since 2.9.9

2.9.9 (16-May-2019)

#63: (yaml) `null` Object Id serialized as anchor for YAML
 (reported by jflefebvre06@github)
#68: (yaml) When field names are reserved words, they should be written out with quotes
 (reported by Gowtam L)
#90: (yaml) Exception when decoding Jackson-encoded `Base64` binary value in YAML
 (reported by Tanguy L)
#91: (properties) `JavaPropsGenerator#writeFieldName()` should not escape property keys
 (reported by Dimitris M)
#122: (csv) `readValues(null)` causes infinite loop
 (reported by andyeko@github) 
#123: (yaml) YAML Anchor, reference fails with simple example
#124: (csv) Add `CsvGenerator.Feature.ESCAPE_CONTROL_CHARS_WITH_ESCAPE_CHAR` for escaping
  non-printable characters in CSV output/input 
 (contributed by Henning S)

2.9.8 (15-Dec-2018)

#99: `YamlGenerator` closes the target stream when configured not to
 (reported by moabck@github; fix contributed by vboulaye@github)

2.9.7 (19-Sep-2018)

#81: Jackson 2.9.5, 2.9.6 incompatible with snakeyaml 1.20, 1.21
 (reported by Yegor B)

2.9.6 (12-Jun-2018)

#84 (yaml): Add option to allow use of platform-linefeed
  (`YAMLGenerator.Feature.USE_PLATFORM_LINE_BREAKS`)
 (contributed by Thomas H)

2.9.5 (26-Mar-2018)

#74 (properties): `JavaPropsMapper` issue deserializing multiple byte array properties
 (reported by Fabrice D)

2.9.4 (24-Jan-2018)

#62 (yaml): SnakeYAML base64Coder is not OSGI exported
 (reported by mbechler@github)
#65 (yaml): `YAMLParser` incorrectly handles numbers with underscores in them
 (reported by Mike K)

2.9.3 (09-Dec-2017)

#39 (yaml): Binary data not recognized by YAML parser
 (repoted by tmoschou@github)
#42 (csv): Add support for escaping double quotes with the configured escape character
 (contributed by frankgrimes97@github)
#51 (csv): Set of custom objects with `IGNORE_UNKNOWN` brokes silently csv
 (reported by Simone L)
#53: (yaml) Binary values written without type tag
 (reported by arulrajnet@github)

2.9.2 (14-Oct-2017)

No changes since 2.9.1

2.9.1 (07-Sep-2017)

#34: (yaml)  Problem with `YAMLGenerator.Feature.INDENT_ARRAYS`, nested Objects
(yaml): Upgrade `SnakeYAML` dep 1.17 -> 1.18

2.9.0 (30-Jul-2017)

CSV:
  (Note: issue numbers point to old, separate repos of `jackson-dataformat-csv`)

#127: Add `CsvGenerator.Feature.ALWAYS_QUOTE_EMPTY_STRINGS` to allow forced
  quoting of empty Strings.
 (contributed by georgewfraser@github)
#130: Add fluent addColumns operation to CsvSchema.Builder
 (contributed by Peter A)
#137: Inject "missing" trailing columns as `null`s
   (`JsonParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS`)
#139: Add `CsvParser.Feature.ALLOW_TRAILING_COMMA` to allow enforcing strict handling
 (contributed by Nick B)
#140: Fail for missing column values (`JsonParser.Feature.FAIL_ON_MISSING_COLUMNS`)
 (suggested by jwilmoth@github)
#142: Add methods for appending columns of a `CsvSchema` into another
 (suggested by Austin S)
- Add new exception type `CsvMappingException` to indicate CSV-mapping issues (and
  give access to effective Schema)

Properties:

#1: Add convenience method(s) for reading System properties
#3: Write into `Properties` instance (factory, mapper) using
  `JavaPropsMapper.writeValue()` with `Properties` and
  `JavaPropsMapper.writeValueAsProperties()`
#4: Allow binding from `Properties` instance

YAML:
  (Note: issue numbers point to old, separate repos of `jackson-dataformat-yaml`)

#67: Add `YAMLGenerator.Feature.INDENT_ARRAYS`
#76: Add `YAMLGenerator.Feature.LITERAL_BLOCK_STYLE` for String output
 (contributed by Roland H)
