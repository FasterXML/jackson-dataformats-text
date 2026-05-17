package tools.jackson.dataformat.toml;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.TreeTraversingParser;

/**
 * Tests for round-trip preservation of TOML table syntax: parsing a document
 * with {@code [path]} headers or {@code [[path]]} array-of-tables and writing
 * it back should preserve those forms instead of collapsing to dotted keys.
 *
 * Also covers the explicit generator-level API
 * ({@link TomlGenerator#writeStartTable()},
 *  {@link TomlGenerator#writeStartArrayOfTables()}) and the strict-ordering
 * enforcement that backs it.
 *
 * Resolves https://github.com/FasterXML/jackson-dataformats-text/issues/254
 */
public class TomlRoundTripTest extends TomlMapperTestBase {

    private final TomlMapper mapper = newTomlMapper();

    private String roundTrip(String input) {
        return mapper.writeValueAsString(mapper.readTree(input));
    }

    /*
    /**********************************************************************
    /* Round-trip: parsed [table] survives writeTree
    /**********************************************************************
     */

    @Test
    public void siblingNestedTablesUnderSharedAncestorRoundTrip() {
        // [server.database] and [server.logging] share an unflagged `server`
        // ancestor. After the first child TABLE closes, the generator must
        // restore _basePath to "server" so writing the second child's name
        // (`logging`) builds the right "server.logging" path.
        String output = roundTrip(
                "[server.database]\n" +
                "host = 'localhost'\n" +
                "\n" +
                "[server.logging]\n" +
                "level = 'debug'\n");
        assertTrue(output.contains("[server.database]"), output);
        assertTrue(output.contains("[server.logging]"), output);
        assertTrue(output.contains("host = 'localhost'"));
        assertTrue(output.contains("level = 'debug'"));
    }

    @Test
    public void siblingArrayOfTablesAndStandardTableRoundTrip() {
        // [[server.workers]] and [server.database] both nest under the same
        // unflagged `server`; mixing the two kinds at the same level
        // exercises the AOT close path's outer-base-path restore.
        String output = roundTrip(
                "[[server.workers]]\n" +
                "id = 1\n" +
                "\n" +
                "[server.database]\n" +
                "host = 'localhost'\n");
        assertTrue(output.contains("[[server.workers]]"), output);
        assertTrue(output.contains("[server.database]"), output);
    }

    @Test
    public void parsedArrayOfTablesRoundTrips() {
        String output = roundTrip(
                "[[products]]\nname = 'Hammer'\nsku = 738594937\n"
                + "\n[[products]]\nname = 'Nail'\nsku = 284758393\n");
        int first = output.indexOf("[[products]]");
        int second = output.indexOf("[[products]]", first + 1);
        assertTrue(first >= 0 && second >= 0,
                "expected two [[products]] headers, got:\n" + output);
        assertTrue(output.contains("name = 'Hammer'"));
        assertTrue(output.contains("name = 'Nail'"));
    }

    @Test
    public void parsedQuotedSegmentHeaderRoundTrips() {
        // Both literal-string ('...') and basic-string ("...") quoting are
        // valid for keys containing '/'. Either is acceptable round-trip.
        String output = roundTrip("[projects.\"some/path\"]\ntrust = 'high'\n");
        boolean hasHeader = output.contains("[projects.\"some/path\"]")
                || output.contains("[projects.'some/path']");
        assertTrue(hasHeader,
                "expected quoted-segment header preserved, got:\n" + output);
    }

    /*
    /**********************************************************************
    /* Round-trip with mutation
    /**********************************************************************
     */

    @Test
    public void addingScalarUnderTableFlaggedParentReorders() {
        // User adds a new scalar to a parsed table; output must keep it
        // grouped under the [server] header (not appended at root after the
        // section ends).
        ObjectNode root = (ObjectNode) mapper.readTree("[server]\nhost = 'localhost'\nport = 5432\n");
        ((ObjectNode) root.get("server")).put("name", "primary");
        String output = mapper.writeValueAsString(root);
        assertTrue(output.contains("[server]"));
        assertTrue(output.contains("name = 'primary'"));
    }

    static Stream<Arguments> rootScalarBeforeHeaderCases() {
        return Stream.of(
                Arguments.of("[server.database]\nhost = 'localhost'\nport = 5432\n", "[server.database]"),
                Arguments.of("[[items]]\nid = 1\n", "[[items]]"));
    }

    @ParameterizedTest
    @MethodSource("rootScalarBeforeHeaderCases")
    public void appendingRootScalarBeforeHeaderKeepsScalarFirst(String input, String header) {
        // Header flags only the leaf, leaving its ancestor unflagged. The
        // reorder predicate must look at descendants, not just immediate
        // flags, or a root scalar appended after parsing lands inside the
        // header's section on round-trip.
        ObjectNode root = (ObjectNode) mapper.readTree(input);
        root.put("name", "app");
        String output = mapper.writeValueAsString(root);
        int nameIdx = output.indexOf("name = 'app'");
        int headerIdx = output.indexOf(header);
        assertTrue(nameIdx >= 0 && headerIdx >= 0, output);
        assertTrue(nameIdx < headerIdx,
                "root scalar must precede header, got:\n" + output);
    }

    static Stream<Arguments> deepCopyPreservesFlagCases() {
        return Stream.of(
                Arguments.of("[server]\nhost = 'localhost'\n", "[server]"),
                Arguments.of("[[items]]\nid = 1\n\n[[items]]\nid = 2\n", "[[items]]"));
    }

    @ParameterizedTest
    @MethodSource("deepCopyPreservesFlagCases")
    public void deepCopyPreservesTomlNodeFlags(String input, String header) {
        // deepCopy must carry fromTableHeader / fromArrayOfTables: a
        // readTree -> deepCopy -> mutate -> write workflow otherwise silently
        // regresses to dotted keys.
        ObjectNode root = (ObjectNode) mapper.readTree(input);
        ObjectNode copy = root.deepCopy();
        String output = mapper.writeValueAsString(copy);
        assertTrue(output.contains(header), output);
    }

    @Test
    public void emptiedArrayOfTablesSerializesAsInlineEmpty() {
        // TOML has no empty [[path]] form. After removeAll(), the field
        // must still appear in the output as `items = []`, not silently
        // disappear.
        ObjectNode root = (ObjectNode) mapper.readTree("[[items]]\nid = 1\n\n[[items]]\nid = 2\n");
        ((ArrayNode) root.get("items")).removeAll();
        String output = mapper.writeValueAsString(root);
        assertTrue(output.contains("items = []"),
                "expected items = [], got:\n" + output);
    }

    @Test
    public void parsedTableSubtreeSerializesAsRoot() {
        // Writing a parsed [server] subtree directly (no enclosing field
        // name) must fall through to plain serialization rather than try to
        // emit a header with no path.
        JsonNode subtree = mapper.readTree("[server]\nhost = 'localhost'\n").get("server");
        String output = mapper.writeValueAsString(subtree);
        assertTrue(output.contains("host = 'localhost'"),
                "expected scalar in subtree output, got:\n" + output);
    }

    @Test
    public void parsedTableNodeReusedInsideInlineArraySerializesInline() {
        // A flagged TomlObjectNode reused as an element of an inline array
        // (the only legal containing form here) must serialize as a plain
        // inline object, not try to emit a `[section]` header.
        JsonNode parsed = mapper.readTree("[server]\nhost = 'localhost'\n").get("server");
        ObjectNode root = mapper.createObjectNode();
        root.putArray("entries").add(parsed);
        String output = mapper.writeValueAsString(root);
        assertTrue(output.contains("entries = "),
                "expected inline array key, got:\n" + output);
        assertTrue(output.contains("host = 'localhost'") || output.contains("host=\"localhost\""),
                "expected inlined scalar from reused subtree, got:\n" + output);
    }

    @Test
    public void parsedArrayOfTablesReusedInsideInlineArraySerializesInline() {
        // Symmetric case: a flagged TomlArrayNode placed inside an inline
        // array must not try to emit `[[path]]` headers.
        JsonNode parsed = mapper.readTree("[[items]]\nid = 1\n").get("items");
        ObjectNode root = mapper.createObjectNode();
        root.putArray("nested").add(parsed);
        // Just must not throw — section headers inside inline scopes are illegal.
        mapper.writeValueAsString(root);
    }

    @Test
    public void parsedArrayOfTablesSubtreeSerializesAsRoot() {
        JsonNode subtree = mapper.readTree("[[items]]\nid = 1\n").get("items");
        // Just must not throw — TOML's root is a table, so an array root has
        // no canonical form, but the generator should not crash with the
        // "without a name" error from _openSection.
        mapper.writeValueAsString(subtree);
    }

    /*
    /**********************************************************************
    /* Strict-ordering / illegal-write enforcement
    /**********************************************************************
     */

    @FunctionalInterface
    private interface IllegalWrite {
        void apply(JsonGenerator g) throws Exception;
    }

    private static Stream<Arguments> illegalExplicitArrayOfTablesElements() {
        return Stream.of(
                // Inside [[path]], each element must be an object. A direct
                // scalar would land with no relative key and emit malformed TOML.
                Arguments.of("scalar element", (IllegalWrite) g -> g.writeNumber(1)),
                Arguments.of("inline array element", (IllegalWrite) g -> g.writeStartArray()),
                // Empty AOT has no valid TOML representation.
                Arguments.of("empty array of tables", (IllegalWrite) g -> {
                    g.writeEndArray();
                    g.writeEndObject();
                }));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("illegalExplicitArrayOfTablesElements")
    public void explicitArrayOfTablesRejectsIllegalElement(String name, IllegalWrite illegal) {
        assertThrows(TomlStreamWriteException.class, () -> {
            StringWriter w = new StringWriter();
            try (JsonGenerator g = mapper.createGenerator(w)) {
                g.writeStartObject();
                g.writeName("items");
                ((TomlGenerator) g).writeStartArrayOfTables();
                illegal.apply(g);
            }
        });
    }

    private static Stream<Arguments> illegalParsedArrayOfTablesMutations() {
        return Stream.of(
                // Scalar mutation: AOT array forced into ARRAY_OF_TABLES write
                // mode can't emit non-object elements.
                Arguments.of("scalar add", (java.util.function.Consumer<ArrayNode>) a -> a.add(3)),
                Arguments.of("array element add",
                        (java.util.function.Consumer<ArrayNode>) a -> a.addArray().add(1).add(2)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("illegalParsedArrayOfTablesMutations")
    public void parsedArrayOfTablesRejectsIllegalMutation(
            String name, java.util.function.Consumer<ArrayNode> mutate) {
        ObjectNode root = (ObjectNode) mapper.readTree("[[items]]\nid = 1\n");
        mutate.accept((ArrayNode) root.get("items"));
        assertThrows(TomlStreamWriteException.class,
                () -> mapper.writeValueAsString(root));
    }

    @Test
    public void scalarAfterTableThrows() {
        // Once a sub-table is opened at a level, subsequent scalars must error.
        assertThrows(TomlStreamWriteException.class, () -> {
            StringWriter w = new StringWriter();
            try (JsonGenerator g = mapper.createGenerator(w)) {
                g.writeStartObject();
                g.writeName("server");
                ((TomlGenerator) g).writeStartTable();
                g.writeName("host");
                g.writeString("localhost");
                g.writeEndObject();
                // ILLEGAL: scalar at root after [server] section opened
                g.writeName("trailing");
                g.writeString("oops");
                g.writeEndObject();
            }
        });
    }

    @Test
    public void scalarAtAncestorAfterNestedTableThrows() {
        // writeStartTable opened from inside a DOTTED_OBJECT (e.g. server.database
        // where `server` has no [server] header of its own) must lock the
        // ancestor too: a later scalar at the root would otherwise serialize
        // inside the [server.database] section.
        assertThrows(TomlStreamWriteException.class, () -> {
            StringWriter w = new StringWriter();
            try (JsonGenerator g = mapper.createGenerator(w)) {
                g.writeStartObject();
                g.writeName("server");
                g.writeStartObject();             // DOTTED_OBJECT, no header
                g.writeName("database");
                ((TomlGenerator) g).writeStartTable(); // [server.database]
                g.writeName("host");
                g.writeString("localhost");
                g.writeEndObject();               // close database
                g.writeEndObject();               // close server
                // ILLEGAL: root-level scalar after [server.database] header
                g.writeName("trailing");
                g.writeString("oops");
                g.writeEndObject();
            }
        });
    }

    /*
    /**********************************************************************
    /* Explicit generator-level API
    /**********************************************************************
     */

    @Test
    public void writeStartTableEmitsHeader() {
        StringWriter w = new StringWriter();
        try (JsonGenerator g = mapper.createGenerator(w)) {
            g.writeStartObject();
            g.writeName("title");
            g.writeString("config");
            g.writeName("server");
            ((TomlGenerator) g).writeStartTable();
            g.writeName("host");
            g.writeString("localhost");
            g.writeName("port");
            g.writeNumber(5432);
            g.writeEndObject();
            g.writeEndObject();
        }
        String out = w.toString();
        assertTrue(out.contains("title = 'config'"));
        assertTrue(out.contains("[server]"));
        assertTrue(out.contains("host = 'localhost'"));
        assertTrue(out.contains("port = 5432"));
    }

    @Test
    public void writeStartArrayOfTablesEmitsRepeatedHeaders() {
        StringWriter w = new StringWriter();
        try (JsonGenerator g = mapper.createGenerator(w)) {
            g.writeStartObject();
            g.writeName("items");
            ((TomlGenerator) g).writeStartArrayOfTables();
            g.writeStartObject();
            g.writeName("id");
            g.writeNumber(1);
            g.writeEndObject();
            g.writeStartObject();
            g.writeName("id");
            g.writeNumber(2);
            g.writeEndObject();
            g.writeEndArray();
            g.writeEndObject();
        }
        String out = w.toString();
        int first = out.indexOf("[[items]]");
        int second = out.indexOf("[[items]]", first + 1);
        assertTrue(first >= 0 && second >= 0,
                "expected two [[items]] headers, got:\n" + out);
        assertTrue(out.contains("id = 1"));
        assertTrue(out.contains("id = 2"));
    }

    @Test
    public void nestedTablePathBuiltCorrectly() {
        // [server.database] should appear when nested writeStartTable calls
        StringWriter w = new StringWriter();
        try (JsonGenerator g = mapper.createGenerator(w)) {
            g.writeStartObject();
            g.writeName("server");
            ((TomlGenerator) g).writeStartTable();
            g.writeName("database");
            ((TomlGenerator) g).writeStartTable();
            g.writeName("host");
            g.writeString("localhost");
            g.writeEndObject();
            g.writeEndObject();
            g.writeEndObject();
        }
        String out = w.toString();
        assertTrue(out.contains("[server.database]"),
                "expected nested [server.database] header, got:\n" + out);
    }

    /*
    /**********************************************************************
    /* End-to-end fixture (the motivating example)
    /**********************************************************************
     */

    @Test
    public void endToEndFixtureWithMutation() {
        ObjectNode root = (ObjectNode) mapper.readTree(
                "name = 'app'\n" +
                "\n" +
                "[server.database]\n" +
                "host = 'localhost'\n" +
                "port = 5432\n" +
                "\n" +
                "[[items]]\n" +
                "id = 1\n");
        // Add a brand-new sub-section under an existing path; setting the
        // fromTableHeader flag (package-private, hence same-package test
        // class) makes the generator emit a fresh [server.logging] header.
        TomlObjectNode logging = (TomlObjectNode) mapper.createObjectNode();
        logging.put("level", "debug");
        logging.fromTableHeader = true;
        ((ObjectNode) root.get("server")).set("logging", logging);

        String output = mapper.writeValueAsString(root);
        assertTrue(output.contains("[server.database]"), output);
        assertTrue(output.contains("[server.logging]"), output);
        assertTrue(output.contains("level = 'debug'"), output);
        assertTrue(output.contains("[[items]]"), output);
        int nameIdx = output.indexOf("name = 'app'");
        int firstHeaderIdx = output.indexOf('[');
        assertTrue(nameIdx >= 0 && nameIdx < firstHeaderIdx,
                "name should precede first table header, got:\n" + output);
    }

    /*
    /**********************************************************************
    /* Interaction with #669 (dotted key cannot extend explicit table)
    /**********************************************************************
     */

    @Test
    public void dottedKeyExtendingExplicitTableStillRejected() {
        // #669 landed on 3.x after this work began; confirm the round-trip
        // node subclasses (which now carry the explicitlyDefined flag) do not
        // regress the parser's rejection of a dotted key extending an
        // already-explicit [table].
        String input = "[fruit]\napple.color = 'red'\n\n[fruit.apple]\ntaste = 'sweet'\n";
        assertThrows(TomlStreamReadException.class, () -> mapper.readTree(input));
    }

    @Test
    public void explicitTableThenExplicitSubtableRoundTrips() {
        // The valid counterpart: a [fruit] table followed by a separately
        // declared [fruit.variety] subtable must round-trip with both headers.
        String output = roundTrip(
                "[fruit]\nname = 'apple'\n\n[fruit.variety]\nkind = 'gala'\n");
        assertTrue(output.contains("[fruit]"), output);
        assertTrue(output.contains("[fruit.variety]"), output);
    }

    /*
    /**********************************************************************
    /* Contract tests for TomlMapper._readTreeAndClose short-circuit
    /**********************************************************************
     */

    @Test
    public void treeTraversingParserExposesSourceNode() {
        // _readTreeAndClose relies on TreeTraversingParser.streamReadInputSource()
        // returning the source node so it can short-circuit and preserve
        // TomlObjectNode flags. If the databind contract changes, the override
        // silently regresses to the deserializer path and flags vanish.
        ObjectNode src = mapper.createObjectNode();
        src.put("k", "v");
        try (JsonParser p = new TreeTraversingParser(src)) {
            assertTrue(p.streamReadInputSource() == src,
                    "TreeTraversingParser.streamReadInputSource() must return the source JsonNode");
        }
    }

    @Test
    public void userConfiguredNodeFactoryIsHonored() {
        // A user who overrides the node factory expects readTree to produce
        // their factory's nodes. The short-circuit must not silently substitute
        // TomlNodeFactory.INSTANCE — fall through to the deserializer path
        // (giving up flag preservation) when the factory has been replaced.
        TomlMapper m = TomlMapper.builder()
                .nodeFactory(JsonNodeFactory.instance)
                .build();
        JsonNode root = m.readTree("[server]\nhost = 'localhost'\n");
        // The default JsonNodeFactory builds plain ObjectNode, not TomlObjectNode.
        assertTrue(root instanceof ObjectNode, root.getClass().getName());
        assertTrue(!(root instanceof TomlObjectNode),
                "expected plain ObjectNode under custom node factory, got " + root.getClass().getName());
    }

    @Test
    public void externalTreeTraversingParserDoesNotMutateSource() {
        // _readTreeAndClose must not short-circuit a caller-supplied
        // TreeTraversingParser: with STRIP_TRAILING_BIGDECIMAL_ZEROES the
        // short-circuit path mutates the source node in place, so a tree
        // built outside the mapper would silently change. Use the standard
        // deserializer path instead.
        TomlMapper stripping = TomlMapper.builder()
                .enable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
                .build();
        ObjectNode src = stripping.createObjectNode();
        src.put("v", new BigDecimal("1.50"));
        BigDecimal before = src.get("v").decimalValue();
        try (JsonParser p = new TreeTraversingParser(src)) {
            stripping.readTree(p);
        }
        assertEquals(before, src.get("v").decimalValue(),
                "readTree must not mutate an externally supplied tree's BigDecimal");
    }

    /*
    /**********************************************************************
    /* BigDecimal trailing-zero handling honors JsonNodeFeature
    /**********************************************************************
     */

    @Test
    public void floatPreservesTrailingZeroesByDefault() {
        // Default mapper has STRIP_TRAILING_BIGDECIMAL_ZEROES disabled, so
        // 1.0 and 1.00 must round-trip with their original scale rather
        // than collapse to 1.
        JsonNode tree = mapper.readTree("a = 1.0\nb = 1.00\n");
        assertEquals(new BigDecimal("1.0"), tree.get("a").decimalValue());
        assertEquals(new BigDecimal("1.00"), tree.get("b").decimalValue());
    }

    @Test
    public void floatStripsTrailingZeroesWhenFeatureEnabled() {
        TomlMapper stripping = TomlMapper.builder()
                .enable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
                .build();
        JsonNode tree = stripping.readTree("a = 1.0\nb = 1.00\nc = 1.50\nz = 0.0\n");
        assertEquals(0, BigDecimal.ONE.compareTo(tree.get("a").decimalValue()));
        assertEquals(0, BigDecimal.ONE.compareTo(tree.get("b").decimalValue()));
        // 1.50 strips to 1.5, not 1
        assertEquals(0, new BigDecimal("1.5").compareTo(tree.get("c").decimalValue()));
        // 0.0 -> stripTrailingZeros() yields 0E-1, not BigDecimal.ZERO; the
        // strip path explicitly normalizes zero to avoid scale surprise.
        assertEquals(0, BigDecimal.ZERO.compareTo(tree.get("z").decimalValue()));
    }

    @Test
    public void unflaggedNestedObjectStillUsesDottedKeys() {
        // Building a tree with createObjectNode() (not a parsed [section]) and
        // *only* scalar children should still produce dotted keys, matching
        // pre-feature behavior. This is the safety guarantee for users on
        // upgrade paths.
        ObjectNode root = mapper.createObjectNode();
        ObjectNode meta = root.putObject("meta");
        meta.put("kind", "scalar-only");
        String output = mapper.writeValueAsString(root);
        assertEquals("meta.kind = 'scalar-only'\n", output);
    }
}
