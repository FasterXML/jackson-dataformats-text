## Overview

[Jackson](/FasterXML/jackson) (Java) data format module that supports reading and writing 
[Java Properties](https://en.wikipedia.org/wiki/.properties) files,
using naming convention to determine implied structure (by default
assuming dotted notation, but configurable from non-nested to other separators).

## Status

Jackson 2.8.0 was the first official release. With 2.8.x this is still considered somewhat
experimental module.

## Maven dependency

To use this extension on Maven-based projects, use following dependency:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.dataformat</groupId>
  <artifactId>jackson-dataformat-properties</artifactId>
  <version>2.8.8</version>
</dependency>
```

# Usage

Basic usage is by using `JavaPropsFactory` in places where you would usually use `JsonFactory`;
or, for convenience, `JavaPropsMapper` instead of plain `ObjectMapper`.

```java
JavaPropsMapper mapper = new JavaPropsMapper();
// and then read/write data as usual
SomeType value = ...;
String props = mapper.writeValueAsBytes(value);
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

## Basics of conversion

Since default `java.util.Properties` can read "flat" key/value entries in,
what is the big deal here?

Most properties files actually use an implied structure by using a naming convention;
most commonly by using period ('.') as logical path separator. So you may have something like:

```
title=Home Page
site.host=localhost
site.port=8080
```

to group related properties together. This implied structure could easily be made actual explicit Structure:
for example, we could consider following Class definitions

```java
public class WebPage {
  public String title;
  public Site site;
}
static class Site {
  public String host;
  public int port;
}
```

So what this module does is to convert "flattened" properties keys into hierarchic structure
and back; and by doing this allows Jackson databinding to bind such properties content into
full hierarcic Object model.

## Simple POJO serialization

Let's consider another simple POJO definition of:

```java
public class User {
    public Gender gender;
    public Name name; // nested object
    public boolean verified;
    public byte[] userImage;
}
static class Name {
    public String first, last;
}
enum Gender { M, F, O; }
```

and code

```java
User user = new User(Gender.M, new Name("Bob", "Palmer"),
   true, new byte[] { 1, 2, 3, 4 });
String propStr = mapper.writeValueAsString(user);
```

and with that, we could have following contents in String `propStr`:

```
gender=M
name.first=Bob
name.last=Palmer
verified=true
userImage=AQIDBA==
```

## Simple POJO deserialization

Given a String of `propStr`, we could easily read contents back as a POJO with:

```java
User result = mapper.readValue(props, User.class);
```

and veirfy that contents are as expected.


## Basic array handling

Although path notation usually assumes that path segments (pieces between separators,
comma by default) are to be considered logical POJO properties (no pun intended), or fields,
there is default handling (which may be disabled) to infer index values from path segments
that are simple numbers. This means that default handling for Properties content like:

```
boxes.1.x = 5
boxes.1.y = 6
boxes.2.x = -5
boxes.2.y = 15
```

could be easily read into following POJO:

```java
public class Container {
  public List<Box> boxes;
}
static class Box {
  public int x, y;
}
```

with

```java
Container c = mapper.readValue(propsFile, Boxes.class);
```

Similarly when writing out Properties content, default inclusion mechanism is to use "simple"
indexes, and to start with index `1`. Note that most of these aspects are configurable; and
note also that when reading, absolute value of index is not used as-is but only to indicate
ordering of entries: that is, gaps in logical numbering do not indicate `null` values in resulting
arrays and `List`s.

## Using "Any" properties

TO BE WRITTEN

## Customizing handling with `JavaPropsSchema`

The most important format-specific configuration mechanism is the ability to define
a `FormatSchema` (type define in `jackson-core`) to use for parsing and generating
content in specific format. In case of Java Properties, schema type to use is
`JavaPropsSchema`.

Format definitions may be passed either directly to specific `JavaPropsGenerator` / `JavaPropsParser`
(when working directly with low-level Streaming API), or, more commonly, when constructing
`ObjectReader` / `ObjectWriter` (as shown in samples above). If no schema is explicitly specified,
the default instance (accessible with `JavaPropsSchema.emptySchema()`) is used.

Schema instances are created by using "mutant factory" methods like `withPathSeparator` on
an instance; since schema instances are fully immutable (to allow thread-safe sharing and
reuse), a new instance is always created if settings would need to change.
Typical usage, then is:

```java
JavaPropsSchema schema = JavaPropsSchema.emptySchema()
   .withPathSeparator("->");
Pojo stuff = mapper.readerFor(Pojo.class)
   .with(schema)
   .readValue(source);
// and writing
mapper.writer(schema)
   .writeValue(stuff, new File("stuff.properties");

```

Currently existing configuration settings to use can be divide into three groups:

* Separators for indicating how components of logical entries are separated from each other. Some settings apply to both reading and writing, others to only generation
* Array settings for indicating how flat Property keys can be interpreted as hierarchic paths
* Other settings for things that are neither separators nor array settings.

### JavaPropsSchema: separators

#### JavaPropsSchema.keyValueSeparator

* Marker used to separate property key and value segments when writing content
    * Only affects writing: currently (2.7) has no effect on reading
* Default value: "="
* Mutator method: `JavaPropsSchema.withKeyValueSeparator(String)`

#### JavaPropsSchema.lineEnding

* String output after each logical key/value entry, usually linefeed
    * Only affects writing: currently (2.7) has no effect on reading (reader assumes a linefeed)
* Default value: "\n" (Unix linefeed)
* Mutator method: `JavaPropsSchema.withLineEnding(String)`

#### JavaPropsSchema.lineIndentation

* String output before each logical key/value entry, useful for adding indentation
    * Only affects writing: currently (2.7) has no effect on reading (reader skips any white space that precedes key name)
* Default value: "" (empty String; that is, no indentation prepended)
* Mutator method: `JavaPropsSchema.withLineIndentation(String)`

#### JavaPropsSchema.pathSeparator

* Marker used to separate logical path segments within key name, if any; if disabled (specified as empty String), no path component separation is performed
    * Affects both reading and writing
* Default value: "."
* Mutator methods
    * `JavaPropsSchema.withPathSeparator(String)` to assign path separator (except if "" given, same as disabling)
    * `JavaPropsSchema.withoutPathSeparator()` to disable use of path logic; if so, only main-level properties are available, with exact property key as name

### JavaPropsSchema: array representation

#### JavaPropsSchema.firstArrayOffset

* If array handling is enabled (simple and/or marker-based indexes), defines the value used as path component for the first element; other elements advance index value by 1
    * Has only effect on writing: when reading, index values are used for ordering but are not used as absolute physical indexes within logical arrays
    * Most commonly used values are `1` and `0`, although any integer value may be used
* Default value: `1`
* Mutator method: `JavaPropsSchema.withFirstArrayOffset(int)`

#### JavaPropsSchema.indexMarker

* Optional pair of start- and end-markers that may be used to denote array entries
* Default value: `Markers("[", "]")` -- allows use of notation like "path.array[1].x = 15"
    * Means that by default marker-based notation IS allowed
* Mutator methods:
    * `JavaPropsSchema.withIndexMarker(Markers)`
    * `JavaPropsSchema.withoutIndexMarker()`

#### JavaPropsSchema.parseSimpleIndexes

* On/off setting that determines whether path segments that are valid positive integers are automatically considered to indicate an array value
    * As name implies, only affects reading
* Default value: `true` -- means that by default path segments that "look like array index" will be considered to be array entries
* Mutator method: `JavaPropsSchema.withParseSimpleIndexes(boolean)`

#### JavaPropsSchema.writeIndexUsingMarkers

* On/off setting that determines which notation (simple, or marker-based) is used for writing paths for array values: if `true`, marker-based (start/end markers) indexing is used, if `false`, "simple" notation where index is output as path segment
    * With default settings these would lead to either "path.array.1.x = 15"  (`false`) or "path.array[1].x = 15" output
* Default value: `false` (so "simple" notation is used, "path.array.1.x = 15")
* Mutator method: `JavaPropsSchema.withWriteIndexUsingMarkers(boolean)`

### JavaPropsSchema: other configuration

#### JavaPropsSchema.header

* Optional String to output at the beginning of resulting Properties output, before any of the values is output; typically to add comment line(s)
    * NOTE: should always end with a linefeed (unless specified as empty String), as otherwise first entry would be concatenated as part of last line of header
* Default value: "" (empty String, that is, no output)
* Mutator method: `JavaPropsSchema.withHeader(String)`

# Sample Usage

## ZooKeeper Configs

Consider following simple [ZooKeeper](https://zookeeper.apache.org/) config file
from [ZK Documentation](https://zookeeper.apache.org/doc/r3.1.2/zookeeperStarted.html):

```
tickTime=2000
dataDir=/var/zookeeper
clientPort=2181
initLimit=5
syncLimit=2
server.1=zoo1:2888:3888
server.2=zoo2:2888:3888
server.3=zoo3:2888:3888
```

this could be expressed by, say, following Java classes (used by unit tests of this module):

```java
static class ZKConfig {
    public int tickTime;
    public File dataDir;
    public int clientPort;
    public int initLimit, syncLimit;
    @JsonProperty("server")
    public List<ZKServer> servers;
}

static class ZKServer {
  public final int srcPort, dstPort;
  public final String host;

  @JsonCreator
  public ZKServer(String combo) { // should validate better; should work for now
    String[] parts = combo.split(":");
	host = parts[0];h
    srcPort = Integer.parseInt(parts[1]);
    dstPort = Integer.parseInt(parts[2]);
  }

  @JsonValue
  public String asString() {
    return String.format("%s:%d:%d", host, srcPort, dstPort);
  }
}
```

where the only slightly complicated part is splitting of colon-separated endpoint definition.
As earlier, reading such Configuration would be as simple as:

```java
ZKConfig config = propsMapper.readValue(new File("zook.properties"), ZKConfig.class);
```

after which access to properties would be done using simple field access (or, if we
prefer, additional getters).

Note: in this example, default schema configuration worked so we did not have to set it
for reading. If we did, we would have used something like:

```java
JavaPropsSchema schema = JavaPropsSchema.emptySchema()
   .withPathSeparator("->");
propsMapper.writer(schema)
   .writeValue(config, new File("zook-modified.properties");
// and similarly when reading
```


