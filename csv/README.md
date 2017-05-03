# Overview

[Jackson](../../../jackson) data format module for reading and writing [CSV](http://en.wikipedia.org/wiki/Comma-separated_values) encoded data, either as "raw" data (sequence of String arrays), or via data binding to/from Java Objects (POJOs).

Project is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

# Status

Since version 2.3 this module is considered complete and production ready.
All Jackson layers (streaming, databind, tree model) are supported.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.dataformat/jackson-dataformat-csv/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.dataformat/jackson-dataformat-csv/)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.fasterxml.jackson.dataformat/jackson-dataformat-csv/badge.svg)](http://www.javadoc.io/doc/com.fasterxml.jackson.dataformat/jackson-dataformat-csv)

## Maven dependency

To use this extension on Maven-based projects, use following dependency:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.dataformat</groupId>
  <artifactId>jackson-dataformat-csv</artifactId>
  <version>2.8.8</version>
</dependency>
```

# Usage

## CSV Schema: what is that?

CSV documents are essentially rows of data, instead of JSON Objects (sequences of key/value pairs).

So one potential way to expose this data is to expose a sequence of JSON arrays; and similarly allow writing of arrays.
Jackson supports this use-case (which works if you do not pass "CSV schema"), but it is not a very convenient way.

The alternative (and most commonly used) approach is to use a "CSV schema", object that defines set of names (and optionally types) for columns. This allows `CsvParser` to expose CSV data as if it was a sequence of JSON objects, name/value pairs.

## 3 ways to define Schema

So how do you get a CSV Schema instance to use? There are 3 ways:

 * Create schema based on a Java class
 * Build schema manually
 * Use the first line of CSV document to get the names (no types) for Schema

Here is code for above cases:

```java
// Schema from POJO (usually has @JsonPropertyOrder annotation)
CsvSchema schema = mapper.schemaFor(Pojo.class);

// Manually-built schema: one with type, others default to "STRING"
CsvSchema schema = CsvSchema.builder()
        .addColumn("firstName")
        .addColumn("lastName")
        .addColumn("age", CsvSchema.ColumnType.NUMBER)
        .build();

// Read schema from the first line; start with bootstrap instance
// to enable reading of schema from the first line
// NOTE: reads schema and uses it for binding
CsvSchema bootstrapSchema = CsvSchema.emptySchema().withHeader();
ObjectMapper mapper = new CsvMapper();
mapper.readerFor(Pojo.class).with(bootstrapSchema).readValue(json);
```

It is important to note that the schema object is needed to ensure correct ordering of columns; schema instances are immutable and fully reusable (as are `ObjectWriter` instances).

Note also that while explicit type can help efficiency it is usually not required, as Jackson data binding can do common conversions/coercions such as parsing numbers from Strings.

## Data-binding with schema

CSV content can be read either using `CsvFactory` (and parser, generators it creates) directly, or through `CsvMapper` (extension of standard `ObjectMapper`).

When using `CsvMapper`, you will be creating `ObjectReader` or `ObjectWriter` instances that pass `CsvSchema` along to `CsvParser` / `CsvGenerator`.
When creating parser/generator directly, you will need to explicitly call `setSchema(schema)` before starting to read/write content.

The most common method for reading CSV data, then, is:

```java
CsvMapper mapper = new CsvMapper();
Pojo value = ...;
CsvSchema schema = mapper.schemaFor(Pojo.class); // schema from 'Pojo' definition
String csv = mapper.writer(schema).writeValueAsString(value);
MappingIterator<Pojo> it = mapper.readerFor(Pojo.class).with(schema)
  .readValues(csv);
// Either read them all one by one (streaming)
while (it.hasNextValue()) {
  Pojo value = it.nextValue();
  // ... do something with the value
}
// or, alternatively all in one go
List<Pojo> all = it.readAll();
```

## Data-binding without schema

But even if you do not know (or care) about column names you can read/write CSV documents. The main difference is that in this case data is exposed as a sequence of ("JSON") Arrays, not Objects, as "raw" tabular data.

So let's consider following CSV input:

```
a,b
c,d
e,f
```

By default, Jackson `CsvParser` would see it as equivalent to following JSON:

```json
["a","b"]
["c","d"]
["e","f"]
```


This is easy to use; in fact, if you ignore everything to do with Schema from above examples, you get working code. For example:

```java
CsvMapper mapper = new CsvMapper();
// important: we need "array wrapping" (see next section) here:
mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
File csvFile = new File("input.csv"); // or from String, URL etc
MappingIterator<String[]> it = mapper.readerFor(String[].class).readValues(csvFile);
while (it.hasNext()) {
  String[] row = it.next();
  // and voila, column values in an array. Works with Lists as well
}
```

## With column names from first row

But if you want a "data as Map" approach, with data that has expected column names as the first row,
followed by data rows, you can iterate over entries quite conveniently as well.
Assuming we had CSV content like:

```csv
name,age
Billy,28
Barbara,36
```

we could use following code:

```java
CsvMapper mapper = new CsvMapper();
CsvSchema schema = CsvSchema.emptySchema().withHeader(); // use first row as header; otherwise defaults are fine
MappingIterator<Map<String,String>> it = mapper.readerFor(Map.class)
   .with(schema)
   .readValues(csvFile);
while (it.hasNext()) {
  Map<String,String> rowAsMap = it.next();
  // access by column name, as defined in the header row...
}
```

and get two rows as `java.util.Map`s, similar to what JSON like this

```json
{"name":"Billy","age":"28"}
{"name":"Barbara","age":"36"}
```

would produce.

Additionally, to generate a schema for the `Map<String,String>`,
we could do the following :

```java
CsvSchema.Builder schema = new CsvSchema.Builder();
for (String value : map.keySet())
{
    schema.addColumn(value, CsvSchema.ColumnType.STRING);
}
new CsvMapper().writerFor(Map.class).with(schema.build());
```


## Adding virtual Array wrapping

In addition to reading things as root-level Objects or arrays, you can also force use of virtual "array wrapping".

This means that using earlier CSV data example, parser would instead expose it similar to following JSON:

```json
[
  ["a","b"]
  ["c","d"]
  ["e","f"]
]
```

This is useful if functionality expects a single ("JSON") Array; this was the case for example when using `ObjectReader.readValues()` functionality.

## Configuring `CsvSchema`

Besides defining how CSV columns are mapped to and from Java Object properties, `CsvSchema` also
defines low-level encoding details. These are details be changed by using various `withXxx()` and
`withoutXxx` methods (or through associated `CsvSchema.Builder` object); for example:

```java
CsvSchema schema = mapper.schemaFor(Pojo.class);
// let's do pipe-delimited, not comma-delimited
schema = schema.withColumnSeparator('|')
   // and write Java nulls as "NULL" (instead of empty string)
   .withNullValue("NULL")
   // and let's NOT allow escaping with backslash ('\')
   .withoutEscapeChar()
   ;
ObjectReader r = mapper.readerFor(Pojo.class).with(schema);
Pojo value = r.readValue(csvInput);
```

For full description of all configurability, please see [CsvSchema](../../../wiki/CsvSchema).

# Documentation

* [Wiki](../../../wiki) (includes javadocs)
* How-to
    * [CSV with Jackson 2.0](http://www.cowtowncoder.com/blog/archives/2012/03/entry_468.html)
    * [Writing CSV using Jackson CSVMapper & Mixin annotations](http://demeranville.com/writing-csv-using-jackson-csvmapper-mixin-annotations/)
    * [CSV with mix-ins](http://demeranville.com/writing-csv-using-jackson-csvmapper-mixin-annotations/)
* Performance
    * [Java CSV parser comparison](https://github.com/uniVocity/csv-parsers-comparison)

# CSV Compatibility

Since CSV is a very loose "standard", there are many extensions to basic functionality.
Jackson supports following extension or variations:

* Customizable delimiters (through `CsvSchema`)
    * Default separator is comma (`,`), but any other character can be specified as well
    * Default text quoting is done using double-quote (`"`), may be changed
    * It is possible to enable use of an "escape character" (by default, not enabled): some variations use `\` for escaping. If enabled, character immediately followed will be used as-is, except for a small set of "well-known" escapes (`\n`, `\r`, `\t`, `\0`)
    * Linefeed character: when generating content, the default linefeed String used is "`\n`" but this may be changed
* Null value: by default, null values are serialized as empty Strings (""), but any other String value be configured to be used instead (like, say, "null", "N/A" etc)
* Use of first row as set of column names: as explained earlier, it is possible to configure `CsvSchema` to indicate that the contents of the first (non-comment) document row is taken to mean set of column names to use
* Comments
    * When enabled (via `CsvSchema`, or enabling `JsonParser.Feature.ALLOW_YAML_COMMENTS`), if a row starts with a `#` character, it will be considered a comment and skipped

# Limitations

* Due to tabular nature of `CSV` format, deeply nested data structures are not well supported.
    * You can use `@JsonUnwrapped` to get around this
* Use of Tree Model (`JsonNode`) is supported, but only within limitations of `CSV` format.

# Future improvements

Areas that are planned to be improved include things like:

* Optimizations to make number handling as efficient as from JSON (but note: even with existing code, performance is typically limited by I/O and NOT parsing or generation)
    * Although, as per [Java CSV parser comparison](https://github.com/uniVocity/csv-parsers-comparison), this module is actually performing quite well already (at 2.4)
* Mapping of nested POJOs using dotted notation (similar to `@JsonUnwrapped`, but without requiring its use -- note that `@JsonUnwrapped` is already supported)
