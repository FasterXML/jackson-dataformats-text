Project: jackson-datatypes-text
Modules:
  jackson-dataformat-csv
  jackson-dataformat-properties
  jackson-dataformat-yaml

------------------------------------------------------------------------
=== Releases ===
------------------------------------------------------------------------

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
