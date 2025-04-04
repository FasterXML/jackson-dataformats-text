Project: jackson-datatypes-text
Modules:
  jackson-dataformat-csv
  jackson-dataformat-properties (since 2.8)
  jackson-dataformat-toml (since 2.13)
  jackson-dataformat-yaml

Active Maintainers:

* Jonas Konrad (@yawkat): author of TOML module
* Tatu Saloranta, tatu.saloranta@iki.fi: author of CSV, Properties and YAML modules

------------------------------------------------------------------------
=== Releases ===
------------------------------------------------------------------------

2.19.0 (not yet released)

#31: (csv) Header names seem to be trimmed
 (reported by Robert D)
#502: (yaml) Add an optional extended parser subclass (`YAMLAnchorReplayingFactory`)
  able to inline anchors
 (contributed by Heiko B)
#523: JSTEP-10: Unify testing structure/tools
 (contributed by Joo-Hyuk K)
#537: (yaml) Snakeyaml 2.4
 (contributed by @pjfanning)
#554: (csv) Enforce, document thread-safety of `CsvSchema`
 (requested by Gili T)

2.18.3 (28-Feb-2025)
2.18.2 (27-Nov-2024)

No changes since 2.18.1

2.18.1 (28-Oct-2024)

#499: (yaml) SnakeYAML upgrade to 2.3
 (contributed by @pjfanning)

2.18.0 (26-Sep-2024)

#442: (csv) Allow use of "value decorators" (like `[` and `]` for arrays)
  for reading `CsvSchema` columns
#468: (csv) Remove synchronization from `CsvMapper`
 (contributed by @pjfanning)
#469: (csv) Allow CSV to differentiate between `null` and empty
  fields (foo,,bar vs. foo,"",bar)
 (contributed by David P)
#482: (yaml) Allow passing `ParserImpl` by a subclass or overwrite the events
 (contributed by Heiko B)
#483: (csv) Incorrect location of CSV errors
 (reported by @RafeArnold)
#485: (csv) CSVDecoder: No Long and Int out of range exceptions
 (reported by Burdyug P)
#495: (csv) Support use of `CsvValueDecorator` for writing CSV column values

2.17.3 (01-Nov-2024)

#499: (yaml) SnakeYAML upgrade to 2.3
 (contributed by @pjfanning)

2.17.2 (05-Jul-2024)

#481: (csv) Fix issue in `setSchema()`
 (contributed by @pjfanning)

2.17.1 (04-May-2024)

No changes since 2.17.0

2.17.0 (12-Mar-2024)

#45: (csv) Allow skipping ending line break
  (`CsvGenerator.Feature.WRITE_LINEFEED_AFTER_LAST_ROW`)
 (proposed by Mathieu L)
#454: (yaml) Unexpected `NumberFormatException` in `YAMLParser`
 (fix contributed by Arthur C)
#456: (yaml) Support max Read/Write nesting depth limits (`StreamReadConstraints`/
  `StreamWriteConstraints`) for YAML
#465: (yaml) YAML: consider starting `#` and ending `:` as quotable characters
 (contributed by Michael E)

2.16.2 (09-Mar-2024)

No changes since 2.16.1

2.16.1 (24-Dec-2023)

#445: `YAMLParser` throws unexpected `NullPointerException` in certain
  number parsing cases
 (fix contributed by Arthur C)

2.16.0 (15-Nov-2023)

#198: (csv) Support writing numbers as quoted Strings with
  `CsvGenerator.Feature.ALWAYS_QUOTE_NUMBERS`
#422: (csv) Add `removeColumn()` method in `CsvSchema.Builder`
#435: (yaml) Minor parsing validation miss: tagged as `int`, exception
  on underscore-only values
#437: (yaml) Update SnakeYAML dependency to 2.2
#438: (csv) `BigInteger` and `BigDecimal` are quoted if
  `CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS` enabled

2.15.4 (15-Feb-2024)

No changes since 2.15.3

2.15.3 (12-Oct-2023)

#400: (yaml) `IllegalArgumentException` when attempting to decode invalid UTF-8
  surrogate by SnakeYAML (oss-fuzz 50431)
#406: (yaml) NumberFormatException from SnakeYAML due to int overflow for
  corrupt YAML version
#426: (yaml) Update to SnakeYAML 2.1

2.15.2 (30-May-2023)

No changes since 2.15.1

2.15.1 (16-May-2023)

#404: (yaml) Cannot serialize YAML with Deduction-Based Polymorphism
 (reported by Peter H)

2.15.0 (23-Apr-2023)

#373: (yaml) Positive numbers with plus sign not quoted correctly with
  `ALWAYS_QUOTE_NUMBERS_AS_STRINGS`
 (requested by @dyadyaJora)
#387: (toml) Stack overflow (50083) found by OSS-Fuzz
 (contributed by @yawkat)
#388: (yaml) Add `YAMLParser.Feature.PARSE_BOOLEAN_LIKE_WORDS_AS_STRINGS`
  to allow parsing "boolean" words as strings instead of booleans
 (contributed by Axel N)
#390: (yaml) Upgrade to Snakeyaml 2.0 (resolves CVE-2022-1471)
 (contributed by @pjfanning)
#411: (toml) Fuzzer-found issue #57237 (buffer boundary condition)
 (contributed by @yawkat)
#415: (yaml) Use `LoaderOptions.allowDuplicateKeys` to enforce duplicate key detection
 (contributed by Niels B)

2.14.3 (05-May-2023)

#378: Some artifacts missing `NOTICE`, `LICENSE` files

2.14.2 (28-Jan-2023)

#356: (toml) Fix TOML parse failure when number token hits buffer edge
 (fix by Jonas K)
#370: (yaml) Replace use of deprecated constructor of SnakeYAML ParserImpl
 (contributed by Andrey S)

2.14.1 (21-Nov-2022)

#352: Disabling `CsvParser.Feature.FAIL_ON_MISSING_HEADER_COLUMNS` has no effect
 (fix contributed by Matteo G)

2.14.0 (05-Nov-2022)

#169: (properties) Need a way to escape dots in property keys (add path separator configuration)
 (contributed by Jim T)
#244: (yaml) Add `YAMLGenerator.Feature.ALLOW_LONG_KEYS` to allow writing keys longer than
  128 characters (default)
 (requested by Simon D)
 (contributed by Shauni A)
#285: (csv) Missing columns from header line (compare to `CsvSchema`) not detected
  when reordering columns (add `CsvParser.Feature.FAIL_ON_MISSING_HEADER_COLUMNS`)
 (reported by Björn M)
#297: (csv) CSV schema caching POJOs with different views
 (contributed by Falk H)
#314: (csv) Add fast floating-point parsing, generation support
 (contributed by @pjfanning)
#335/#346: (yaml) Update to SnakeYAML 1.33
 (contributed by @pjfanning)
#337: (yaml) Allow overriding of file size limit for YAMLParser by exposing
  SnakeYAML `LoaderOptions`
 (contributed by @pjfanning)
#345: (yaml) Support configuring SnakeYAML DumperOptions directly
 (contributed by @pjfanning)
#351: (csv) Make CSVDecoder use lazy parsing of BigInteger/BigDecimal
* (yaml) Fixes to number decoding based on oss-fuzz findings

2.13.5 (23-Jan-2023)

#343: Incorrect output buffer boundary check in `CsvEncoder`
 (reported by k163377@github)

2.13.4 (03-Sep-2022)

#329: (yaml) Update to SnakeYAML 1.31
 (contributed by @pjfanning)

2.13.3 (14-May-2022)

No changes since 2.13.2

2.13.2 (06-Mar-2022)
 
#303: (yaml) Update to SnakeYAML 1.30
 (suggested by PJ Fanning)
#306: (yaml) Error when generating/serializing keys with multilines and colon
 (reported by Esteban G)
#308: (csv) `CsvMapper.typedSchemaForWithView()` with `DEFAULT_VIEW_INCLUSION`
 (contributed by Falk H)

2.13.1 (19-Dec-2021)

#288: (csv) Caching conflict when creating CSV schemas with different views
  for the same POJO
 (reported by Falk H)

2.13.0 (30-Sep-2021)

#219: (toml) Add TOML (https://en.wikipedia.org/wiki/TOML) support
 (requested by Suminda S; contributed by Jonas K)
#240: (csv) Split `CsvMappingException` into `CsvReadException`/`CsvWriteException`
#255: (properties) Ensure that empty String to null/empty works by default
  for Properties format
#270: (csv) Should not quote with strict quoting when line starts with `#` but comments
  are disabled
 (contributed by Krzysztof D)
#283: (csv) `CsvSchema.getColumnDesc()` returns unpaired square bracket when columns are empty
 (contributed by PJ Fanning)

2.12.7 (26-May-2022)
2.12.6 (15-Dec-2021)
2.12.5 (27-Aug-2021)

No changes since 2.12.4

2.12.4 (06-Jul-2021)

#274: YAMLGenerator does not quote tilde (~) characters when MINIMIZE_QUOTES
  is enabled
 (reported by James W)

2.12.3 (12-Apr-2021)

#246: (yaml) Special characters shouldn't force double quoting for multi-line strings
 (fix proposed by Alex H)

2.12.2 (03-Mar-2021)

- Need to export "com.fasterxml.jackson.dataformat.yaml.util" in `module-info.java`

2.12.1 (08-Jan-2021)

No changes since 2.12.0

2.12.0 (29-Nov-2020)

#71: (yaml) Hex number as an entry of an Object causing problem(s) with binding to POJO
#130: (yaml) Empty String deserialized as `null` instead of empty string
 (reported by iulianrosca@github)
#175: (yaml) Add `YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR` to indent by 2 spaces
 (requested by Jesper N; fix contributed by Damian S)
#199: (csv) Empty Lists can only be String-typed in CSV
 (reported by Simon L)
#222: (csv) `JsonParser.Feature.EMPTY_STRING_AS_NULL` does not work when
  text is parsed as `String[]`
 (reported by wkwkhautbois@github)
#226: (yaml) Quote 'y'/'Y'/'n'/'N' as names too (to avoid problems with Boolean keys)
 (requested by pnepywoda@github)
#229: (yaml) Allow configuring the way "must quote" is determined for property names, String values
#231: (yaml) Typed object with anchor throws Already had POJO for id (note: actual
   fix in `jackson-annotations`)
 (reported by almson@github)
#232: (yaml) Typed object throws "Missing type id" when annotated with @JsonIdentityInfo
 (reported by almson@github)
#233: (yaml) Support decoding Binary, Octal and Hex numbers as integers
- Add configurability of "YAML version generator is to follow" via "YAMLFactory.builder()"
- SnakeYAML 1.26 -> 1.27
- Add Gradle Module Metadata (https://blog.gradle.org/alignment-with-gradle-module-metadata)

2.11.4 (12-Dec-2020)

No changes since 2.11.3

2.11.3 (02-Oct-2020)

#217: (csv) Should quote strings with line separator under STRICT_CHECK_FOR_QUOTING
  mode
 (reported, fix contributed by wkwkhautbois@github)

2.11.2 (02-Aug-2020)

#204: (csv) `CsvParser.Feature.ALLOW_TRAILING_COMMA` doesn't work with header columns
 (reported by Björn M)

2.11.1 (25-Jun-2020)

#51: (yaml) `YAMLParser._locationFor()` does not use index available from `Mark`
  object of Event
 (reported by Rob S)
#201: (yaml) Improve `MINIMIZE_QUOTES` handling to avoid quoting for some use of `#` and `:`
 (contributed by Francesco T)

2.11.0 (26-Apr-2020)

#7: (csv) Add `CsvParser.Feature.EMPTY_STRING_AS_NULL` to allow coercing empty Strings
  into `null` values
 (contributed by Tyler C-R)
#115: (csv) JsonProperty index is not honored by CsvSchema builder
 -- actually fixed by [databind#2555]
#174: (csv) `CsvParser.Feature.SKIP_EMPTY_LINES` results in a mapping error
 (reported by Yohann B)
#180: (yaml) YAMLGenerator serializes string with special chars unquoted when
  using `MINIMIZE_QUOTES` mode
 (reported, fix contributed by Timo R)
#191: (csv) `ArrayIndexOutOfBoundsException` when skipping empty lines, comments
 (reported by f-julian@github)
#195 (csv) Adds schema creating csv schema with View
 (contributed by Damian S)

2.10.5 (21-Jul-2020)

#146: (yaml) Jackson can't handle underscores in numbers
 (reported by Sergey M; fix contributed by Conor W)
#204: (csv) `CsvParser.Feature.ALLOW_TRAILING_COMMA` doesn't work with header columns
 (reported by Björn M)

2.10.4 (03-May-2020)

#178: Upgrade SnakeYAML to 1.26 (from 1.24)
#179 (properties): `JavaPropsMapper` doesn't close the .properties file
   properly after reading
 (reported by Francisco C)
#182 (yaml): Negative numbers not quoted correctly wrt `ALWAYS_QUOTE_NUMBERS_AS_STRINGS
 (reported, contributed fix by dpdevin@github)
#184 (properties, yaml): ` jackson-databind` should not be optional/provided dependency
   for Properties, YAML modules
 (reported by Jason V-Z)
#187: Update to SnakeYAML to 1.26 (from 1.24) to address CVE-2017-18640
 (reported by Jochen S)

2.10.3 (03-Mar-2020)

No changes since 2.10.2

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
