Here are people who have contributed to the development of Jackson JSON processor
text dataformats project, version 2.x
(version numbers in brackets indicate release in which the problem was fixed)

(note: for older credits, check out release notes for 1.x versions)

Tatu Saloranta, tatu.saloranta@iki.fi: author

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

* Contibuted #84 (yaml): Add option to allow use of platform-linefeed
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
