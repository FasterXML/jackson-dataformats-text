Here are people who have contributed to the development of Jackson JSON processor
text dataformats project, version 2.x
(version numbers in brackets indicate release in which the problem was fixed)

(note: for older credits, check out release notes for 1.x versions)

* Jonas Konrad (@yawkat): author of TOML module
* Tatu Saloranta (@cowtowncoder): author of CSV, Properties and YAML modules
* PJ Fanning (@pjfanning): active contributor to most modules

--------------------------------------------------------------------------------
Credits for specific contributions
--------------------------------------------------------------------------------

Simone Locci (pimuzzo@github)

* Reported #51 (csv): Set of custom objects with `IGNORE_UNKNOWN` brokes silently csv
 (2.9.3)

Mike Kobit (mkobit@github.com)

* Reported #65 (yaml): `YAMLParser` incorrectly handles numbers with underscores in them
 (2.9.4)

Fabrice Delhoste (spifd@github)

* Reported #74 (properties): `JavaPropsMapper` issue deserializing multiple byte array properties
 (2.9.5)

Thomas Hauk (thauk-copperleaf@github)

* Contributed #84 (yaml): Add option to allow use of platform-linefeed
  (`YAMLGenerator.Feature.USE_PLATFORM_LINE_BREAKS`)
 (2.9.6)

Yegor Borovikov (yborovikov@gitub)

* Reported #81 (yaml): Jackson 2.9.5, 2.9.6 incompatible with snakeyaml 1.20, 1.21
 (2.9.7)

Gowtam Lal (baconmania@github)

* Reported #68: (yaml) When field names are reserved words, they should be
  written out with quotes
 (2.9.9)

Dimitris Mandalidis (dmandalidis@github)

* Reported #91: (properties) `JavaPropsGenerator#writeFieldName()` should not escape property keys
 (2.9.9)

Henning Schmiedehausen (hgschmie@github)
* Contributed #125: (csv) Add `CsvGenerator.Feature.ESCAPE_CONTROL_CHARS_WITH_ESCAPE_CHAR` for escaping
  non-printable characters in CSV output/input
 (2.9.9)

Tanguy Leroux (tlrx@github)
* Reported #90: Exception when decoding Jackson-encoded `Base64` binary value in YAML
 (2.10.0)

Andrey Somov (asomov@github)
* Contributed #101: Use latest SnakeYAML version 1.23 and get rid of deprecated methods
 (2.10.0)

Alon Bar-Lev (alonbl@github)
* Contributed #100: (properties) Add an option to specify properties prefix
 (2.10.0)

Stehan Leh (stefanleh@github)
* Reported #116: (yaml) Error handling "null" String when Feature.MINIMIZE_QUOTES is active
 (2.10.0)

Guillaume Smaha (GuillaumeSmaha@github)
* Contributed fix for #129: (yaml) Convert YAML string issue
 (2.10.0)

Filip Hrisafov (filiphr@github)
* Suggested #139: Support for Map<String, String> in `JavaPropsMapper`
 (2.10.0)

Matti Bickel (wundrian@github)
* Reported #83: Update index of sequence context
 (2.10.0)

Maarten Winkels (mwinkels@github)
* Contributed fix for #83: Update index of sequence context
 (2.10.0)

Vincent Boulaye (vboulaye@github)
* Implemented #15: Add a `CsvParser.Feature.SKIP_EMPTY_LINES` to allow
  skipping empty rows
 (2.10.1)

Piyush Kumar (piyushkumar13@github)
* Reported #163: (yaml) `SequenceWriter` does not create multiple docs in a single yaml file
 (2.10.2)

Francisco Colmenares (fcolmenarez@github)
* Reported #179 (properties): `JavaPropsMapper` doesn't close the .properties file
  properly after reading
 (2.10.4)

Jason van Zyl (jvanzyl@github)
* Reported #184 (properties, yaml): ` jackson-databind` should not be optional/provided dependency
   for Properties, YAML modules
 (2.10.4)

Jochen Schalanda (joschi@github)
* Reported #187: Update to SnakeYAML to 1.26 (from 1.24) to address CVE-2017-18640
 (2.10.4)

Sergey Medelyan (smedelyan@github)
* Reported #146: Jackson can't handle underscores in numbers
 (2.10.5)

Conor Ward (conor-ward@github)
* Contributed fix for #146: Jackson can't handle underscores in numbers
 (2.10.5)

Tyler Carpenter-Rivers (tyler2cr@github)
#7: Add `CsvParser.Feature.EMPTY_STRING_AS_NULL` to allow coercing empty Strings
  into `null` values
 (2.11.0)

* Reported, constributed fix for #180: (yaml) YAMLGenerator serializes string with special
  chars unquoted when using `MINIMIZE_QUOTES` mode
 (2.11.0)

Yohann BONILLO (ybonillo@github)
* Reported #174: (csv) `CsvParser.Feature.SKIP_EMPTY_LINES` results in a mapping error
 (2.11.0)

Damian Servin (Qnzvna@github)
* Contributed #195 (csv) Adds schema creating csv schema with View
 (2.11.0)
 
Rob Spoor (robtimus@github)
* Reported #51: (yaml) `YAMLParser._locationFor()` does not use index available from
  `Mark`object of Event
 (2.11.1)

Francesco Tumanischvili (frantuma@github)
* Contibuted fix for #201: (yaml) Improve `MINIMIZE_QUOTES` handling to avoid quoting
  for some uses of `#` and `:`
 (2.11.1)

Björn Michael (bjmi@github)
* Reported #204: (csv) `CsvParser.Feature.ALLOW_TRAILING_COMMA` doesn't work with header columns
 (2.11.2)
* Reported #285: (csv) Missing columns from header line (compare to `CsvSchema`) not detected
  when reordering columns (add `CsvParser.Feature.FAIL_ON_MISSING_HEADER_COLUMNS`)
 (2.14.0)

Jesper Nielsen (jn-asseco@github)
* Requested #175: (yaml) Add `YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR`
  to indent by 2 spaces
 (2.12.0)

Damian Swiecki (dswiecki@github)
* Contributed fix for #175: (yaml) Add `YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR`
  to indent by 2 spaces
 (2.12.0)

Simon Levermann (sonOfRa@github)
* Reported #199: (csv) Empty Lists can only be String-typed in CSV
 (2.12.0)

Alex Heneveld (ahgittin@github)
* Reported and proposed fix for #246: (yaml) Special characters shouldn't force double quoting
   for multi-line strings
 (2.12.3)

James Wynn (jameswynn@github)
* Reported #274: YAMLGenerator does not quote tilde (~) characters when MINIMIZE_QUOTES
  is enabled
 (2.12.4)

Suminda Sirinath Salpitikorala Dharmasena (sirinath@github)
* Requested #219: Add TOML (https://en.wikipedia.org/wiki/TOML) support
 (2.13.0)

Jonas Konrad (yawkat@github)
* Contributed #219: Add TOML (https://en.wikipedia.org/wiki/TOML) support
 (2.13.0)
* Contributed #356: Fix TOML parse failure when number token hits buffer edge
 (2.14.2)

Krzysztof Debski (kdebski85@github)
#270: Should not quote with strict quoting when line starts with `#` but comments
  are disabled
 (2.13.0)

PJ Fanning (pjfanning@github)
#283: (csv) `CsvSchema.getColumnDesc()` returns unpaired square bracket when columns
  are empty
 (2.13.0)
#314: (csv) Add fast floating-point parsing, generation support
 (2.14.0)
#390: (yaml) Upgrade to Snakeyaml 2.0 (resolves CVE-2022-1471)
 (2.15.0)
 ... and many, many more not listed here

Falk Hanisch (mrpiggi@github)
#288: Caching conflict when creating CSV schemas with different views
  for the same POJO
 (2.13.1)
#308: (csv) `CsvMapper.typedSchemaForWithView()` with `DEFAULT_VIEW_INCLUSION`
 (2.13.2)
#297: (csv) CSV schema caching POJOs with different views
 (2.14.0)

Esteban Ginez (eginez@github)
#306: (yaml) Error when generating/serializing keys with multilines and colon
 (2.13.2)

Jim Talbut (Yaytay@github)
#169: (properties) Need a way to escape dots in property keys (add path separator configuration)
 (2.14.0)

Simon Dean (msmsimondean@github)
* Requested #244: (yaml) Add `YAMLGenerator.Feature.ALLOW_LONG_KEYS` to allow writing keys
  longer than 128 characters (default)
 (2.14.0)

Shauni Arima (ShauniArima@github)
* Contributed #244: (yaml) Add `YAMLGenerator.Feature.ALLOW_LONG_KEYS` to allow writing keys
  longer than 128 characters (default)
 (2.14.0)

Matteo Giordano (malteo@github)
* Contributed fix for #352: Disabling `CsvParser.Feature.FAIL_ON_MISSING_HEADER_COLUMNS` has no effect
 (2.14.1)

Axel Niklasson (axelniklasson@github)

* Contributed #388: (yaml) Add `YAMLParser.Feature.PARSE_BOOLEAN_LIKE_WORDS_AS_STRINGS`
  to allow parsing "boolean" words as strings instead of booleans
 (2.15.0)

Niels Basjes (nielsbasjes@github)

* Contributed #415: (yaml) Use `LoaderOptions.allowDuplicateKeys` to enforce duplicate key detection
 (2.15.0)

Peter Haumer (phaumer@github)

* Reported #404: (yaml) Cannot serialize YAML with Deduction-Based Polymorphism
 (2.15.1)

Arthur Chan (arthurscchan@github)

* Contributed fix for #445: `YAMLParser` throws unexpected `NullPointerException` in certain
  number parsing cases
 (2.16.1)
* Contributed fix for #454: (yaml) Unexpected `NumberFormatException` in `YAMLParser`
 (2.17.0)

Mathieu Lavigne (@mathieu-lavigne)

* Proposed #45 (and suggested implementation): (csv) Allow skipping ending line break
  (`CsvGenerator.Feature.WRITE_LINEFEED_AFTER_LAST_ROW`)
 (2.17.0)

Michael Edgar (@MikeEdgar)

* Contributed #465: (yaml) YAML: consider starting `#` and ending `:` as quotable characters
 (2.17.0)

David Pujol (@PujolDavid)

* Contributed #469: (csv) Allow CSV to differentiate between `null` and empty
  fields (foo,,bar vs. foo,"",bar)
 (2.18.0)
