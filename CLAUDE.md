# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Jackson Dataformats Text is a multi-module Maven project providing Jackson format backends for text-based data formats (CSV, YAML, TOML, Java Properties). Each module extends Jackson core abstractions (`JsonFactory`, `JsonParser`, `JsonGenerator`) to support non-JSON formats using the standard Jackson API.

This file documents the **2.x line** (target branch: `2.x`, currently `2.23.0-SNAPSHOT`).

**Branches:**
- `2.x` — active development for the next 2.x minor release (2.23.0-SNAPSHOT)
- `2.21`, `2.18` — maintenance branches for patch releases
  (2.21.6, 2.18.10 SNAPSHOTs respectively)
- `3.x` — Jackson 3.3 development (3.3.0-SNAPSHOT)
- `3.1` — Jackson 3.1 patch releases (3.1.6-SNAPSHOT)
- `master` — not used

**Merge-forward convention:** fixes land on the **oldest** branch that should receive them, then are
merged forward: `2.18 → 2.19 → 2.20 → 2.21 → 2.22 → 2.x → 3.1 → 3.2 → 3.x `. Never cherry-pick a fix
independently into multiple branches — commit to the oldest target and merge up, so history stays linear.
New features (not bug fixes) go to `2.x` (and `3.x`) only.

**Java baseline for 2.x:** Java 8. CI builds on JDK 8, 11, 17 and 21 (`.github/workflows/main.yml`);
the JDK 8 build is the release build. Do not use post-8 language features or APIs in `src/main`.

## Build and Test Commands

### Building
```bash
# Build entire project (all modules)
./mvnw clean verify

# Build specific module
./mvnw -pl csv clean verify
./mvnw -pl yaml clean verify
./mvnw -pl toml clean verify
./mvnw -pl properties clean verify

# Skip tests
./mvnw clean install -DskipTests

# Skip a module
./mvnw clean verify -pl '!toml'
```

Note: the parent is `com.fasterxml.jackson:jackson-base` at the matching SNAPSHOT version. When
building against unreleased jackson-core/databind changes, those must be installed locally first.

### Testing
```bash
# Run all tests
./mvnw test

# Run tests for specific module
./mvnw -pl csv test
./mvnw -pl yaml test

# Run single test class
./mvnw -pl csv test -Dtest=CsvParserTest

# Run single test method
./mvnw -pl csv test -Dtest=CsvParserTest#testSimpleExplicit
```

Coverage report (JaCoCo) at `{module}/target/site/jacoco/jacoco.xml`.

### Other Useful Commands
```bash
# Check for dependency updates
./mvnw versions:display-dependency-updates

# View effective POM
./mvnw help:effective-pom
```

## Architecture

All format modules follow a consistent **Factory + Builder + Streaming Parser/Generator** pattern:

1. **Factory** (`XxxFactory extends JsonFactory`) — creates parsers and generators; immutable after
   construction (reuse/cache it); built via `XxxFactoryBuilder` (fluent API)
2. **Parser** (`XxxParser`) — streaming parser, typically extending `ParserBase`/`ParserMinimalBase`
3. **Generator** (`XxxGenerator`) — streaming generator, typically extending `GeneratorBase`
4. **Mapper** (`XxxMapper extends ObjectMapper`) — convenience databinding entry point preconfigured
   with the format factory
5. **Feature Enums** (implement `FormatFeature`) — module-specific flags, combined as bitmasks

### Module layout and package names

| Module | Java package | Key classes |
|---|---|---|
| `csv` | `com.fasterxml.jackson.dataformat.csv` | `CsvFactory`, `CsvParser`, `CsvGenerator`, `CsvMapper`, `CsvSchema` |
| `yaml` | `com.fasterxml.jackson.dataformat.yaml` | `YAMLFactory`, `YAMLParser`, `YAMLGenerator`, `YAMLMapper`, `YAMLAnchorReplayingFactory` |
| `toml` | `com.fasterxml.jackson.dataformat.toml` | `TomlFactory`, `TomlGenerator`, `TomlMapper`, `Parser` (internal), `TomlReadFeature`/`TomlWriteFeature` |
| `properties` | `com.fasterxml.jackson.dataformat.javaprop` | `JavaPropsFactory`, `JavaPropsParser`, `JavaPropsGenerator`, `JavaPropsMapper`, `JavaPropsSchema` |

Note the mismatch for the properties module: directory is `properties/`, package is `javaprop`, classes
are `JavaProps*`.

### Module-Specific Patterns

**CSV** (`csv/`):
- `CsvSchema` defines column order/types/separators; required for positional (non-header) access.
  `CsvSchema` is immutable — mutate via builder or `withXxx()` methods
- `impl/` holds `CsvDecoder`, `CsvEncoder`, `CsvParserBootstrapper` (quote/escape/BOM handling)
- `CsvValueDecorator`/`CsvValueDecorators` for per-column value pre/post-processing

**YAML** (`yaml/`):
- Wraps external SnakeYAML; configured through `DumperOptions` and `LoaderOptions`
- Supports YAML-specific features (anchors, aliases, tags); `YAMLAnchorReplayingParser` handles
  anchor/alias replay
- `util/StringQuotingChecker` decides when scalars must be quoted on output
- SnakeYAML exceptions are re-wrapped via the `yaml.snakeyaml.error` shim package

**TOML** (`toml/`):
- Parse-to-tree approach: `Parser` reads the whole document into an `ObjectNode`, then `TomlFactory`
  wraps it in a `TreeTraversingParser`. There is no incremental TOML parser
- Configured via `TomlReadFeature`/`TomlWriteFeature` (no schema)

**Properties** (`properties/`):
- Dual output: writes to a `Writer` (`WriterBackedGenerator`) or to a `Map`/`Properties`
  (`PropertiesBackedGenerator`)
- Hierarchical path flattening/unflattening via `util/JPropNode`, `JPropNodeBuilder`,
  `JPropPathSplitter`; `JavaPropsSchema` configures path separator, index markers, prefix
- `io/JPropEscapes`, `io/Latin1Reader` implement `.properties` escaping/encoding rules

### Key Design Principles

- **Factory reuse**: factories are thread-safe and should be cached
- **IOContext management**: all I/O goes through `IOContext` for buffer recycling and resource lifecycle
- **Feature flags**: configuration via bitwise integer flags (not `EnumSet`)
- **Schema optional**: only CSV and Properties use schemas; YAML/TOML rely on format structure
- **Immutability**: factories, builders and schemas are immutable after construction
- **Backward compatibility**: Jackson has strong compatibility guarantees. In a patch branch
  (e.g. `2.18`) do not add or change public API — bug fixes only. New public API goes to `2.x`
- **StreamReadConstraints**: input-size/nesting limits come from jackson-core's constraints; see the
  `limits`/`constraints`/`dos` test packages

## Module Directory Structure

```
{module}/
├── src/
│   ├── main/java/com/fasterxml/jackson/dataformat/{package}/
│   │   ├── {Format}Factory.java
│   │   ├── {Format}FactoryBuilder.java
│   │   ├── {Format}Parser.java
│   │   ├── {Format}Generator.java
│   │   ├── {Format}Mapper.java
│   │   ├── {Format}Schema.java      (CSV and Properties only)
│   │   └── PackageVersion.java.in   (template; generated, never edit output)
│   ├── moditect/module-info.java    (Java 9+ module descriptor)
│   └── test/java/com/fasterxml/jackson/dataformat/{package}/
│       ├── ModuleTestBase.java      (csv, yaml, properties; toml uses TomlMapperTestBase)
│       ├── deser/                   (deserialization tests)
│       ├── ser/                     (serialization tests)
│       ├── filter/                  (JsonView / filtering tests)
│       ├── schema/                  (schema tests, CSV)
│       ├── fuzz/                    (regression tests from OSS-Fuzz reports)
│       ├── limits/, constraints/, dos/ (input-limit / DoS tests)
│       ├── testutil/                (shared test helpers + failure/ annotations)
│       └── tofix/                   (tests for known-broken behavior)
└── pom.xml
```

## Testing Conventions

- **Base class**: tests extend `ModuleTestBase` (`TomlMapperTestBase` in the toml module) for shared
  helpers (`mapperForCsv()`, `verifyException()`, etc.)
- **Framework**: JUnit 5 (JSTEP-10 migration completed in 2.19; older branches may still be JUnit 4)
- **Assertions**: mix of JUnit 5 assertions and AssertJ
- **Known-failing tests**: put them in the `tofix/` package (renamed from the older `failing/`) and
  annotate with `@JacksonTestFailureExpected` from `testutil/failure/`. The interceptor inverts the
  result, so the test fails once the bug is fixed — then move it out of `tofix/`
- **Naming**: test methods start with `test`; test classes end with `Test`
- **Fuzzer regressions**: name after the OSS-Fuzz issue, e.g. `FuzzTomlRead57237Test`

## Common Patterns When Working with This Codebase

### Adding a New Feature Flag

1. Add the enum constant to `{Format}Parser.Feature`, `{Format}Generator.Feature`, or the standalone
   `TomlReadFeature`/`TomlWriteFeature`
2. Implement the `FormatFeature` methods (`enabledByDefault()`, `getMask()`, `enabledIn()`)
3. Wire it into the factory builder
4. Test both enabled and disabled states
5. New features go to `2.x` (and `3.x`), never to a patch branch

### Modifying Parser/Generator Behavior

1. Changes go in the format-specific parser/generator (or its `impl`/`io` helpers)
2. Gate optional behavior behind a feature flag rather than hard-coding it
3. Preserve backward compatibility
4. Add tests covering both enabled and disabled states

### Fixing a Reported Issue

1. Determine the oldest branch that should get the fix; work there, then merge forward
2. Add a regression test named after the issue number where that's the local convention
3. Update `release-notes/VERSION-2.x` (and `release-notes/CREDITS-2.x` when a contributor
   reported/submitted it) — this is expected for every user-visible fix

### Package Version Generation

`PackageVersion.java` is generated at build time from `PackageVersion.java.in` by the
`maven-replacer-plugin` (inherited from `oss-base`). Edit the `.in` template, never the generated file.

## Dependencies

All modules depend on `jackson-core`, `jackson-databind` and `jackson-annotations` at the matching
2.x version (managed by `jackson-base`). Test scope adds JUnit 5 (`junit-jupiter`) and AssertJ.

Module-specific:
- `yaml`: SnakeYAML (external, compile scope)
- `csv`, `toml`, `properties`: no third-party runtime dependencies (parsers are implemented in-tree)

## Release and Versioning

- Semantic versioning; version is set in the parent `pom.xml` and inherited by modules
- Release tags: `jackson-dataformats-text-{version}`
- Releases are cut with `maven-release-plugin` from the branch matching the minor version
- `release-notes/VERSION-2.x` and `release-notes/CREDITS-2.x` are the changelog of record for 2.x
