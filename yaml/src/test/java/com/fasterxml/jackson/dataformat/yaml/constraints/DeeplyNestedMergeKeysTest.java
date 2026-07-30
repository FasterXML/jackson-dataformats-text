package com.fasterxml.jackson.dataformat.yaml.constraints;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLAnchorReplayingFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for [dataformats-text#707]: nesting of YAML merge keys ({@code <<}) read via
 * {@link YAMLAnchorReplayingFactory} must be limited by
 * {@link StreamReadConstraints#getMaxNestingDepth()}.
 *<p>
 * Merge keys used to be followed by recursion in {@code YAMLAnchorReplayingParser.getEvent()},
 * and merge nesting was not counted towards maximum nesting depth at all, so that a ~20 kB
 * document could exhaust the stack with {@code StackOverflowError} (GHSA-255r-36wv-4qpr).
 */
public class DeeplyNestedMergeKeysTest
    extends ModuleTestBase
{
    private final YAMLAnchorReplayingFactory F = new YAMLAnchorReplayingFactory();

    private final YAMLMapper MAPPER = new YAMLMapper(F);

    // Small enough stack that the recursive implementation is guaranteed to fail, big
    // enough that the non-recursive one has plenty of room
    private final static int SMALL_STACK = 256 * 1024;

    // Depth of the original report; well past the default 1000 nesting depth
    private final static int REPORTED_DEPTH = 3300;

    /**
     * {@code {<<: {<<: ... {x: 1} ...}}}: each level of nesting used to add stack frames
     * that were only released once the innermost node had been reached.
     */
    private static String nestedMergeKeys(int depth) {
        StringBuilder sb = new StringBuilder(6 * depth + 20);
        for (int i = 0; i < depth; ++i) {
            sb.append("{<<: ");
        }
        sb.append("{x: 1}");
        for (int i = 0; i < depth; ++i) {
            sb.append('}');
        }
        return "result:\n  <<: "+sb+"\n";
    }

    @Test
    public void testDeeplyNestedMergeKeysStreaming() throws Exception
    {
        final String doc = nestedMergeKeys(REPORTED_DEPTH);
        Throwable failure = failureOnSmallStack(() -> {
            try (JsonParser p = F.createParser(doc)) {
                while (p.nextToken() != null) { }
            }
        });
        _assertNestingDepthFailure(failure, 1001, 1000);
    }

    @Test
    public void testDeeplyNestedMergeKeysDatabind() throws Exception
    {
        final String doc = nestedMergeKeys(REPORTED_DEPTH);
        Throwable failure = failureOnSmallStack(() -> MAPPER.readTree(doc));
        _assertNestingDepthFailure(failure, 1001, 1000);
    }

    /**
     * The limit must be the configured one, not a hard-coded constant.
     */
    @Test
    public void testMergeKeyNestingLimitConfigurable() throws Exception
    {
        final YAMLAnchorReplayingFactory f = new YAMLAnchorReplayingFactory(
                YAMLFactory.builder()
                    .streamReadConstraints(StreamReadConstraints.builder()
                            .maxNestingDepth(10).build())
                    .build(),
                (ObjectCodec) null);
        final YAMLMapper mapper = new YAMLMapper(f);

        // NOTE: `nestedMergeKeys(n)` reaches a combined depth of n+3: two structural
        // levels (root mapping, value of `result`), plus n+1 nested merges

        // Combined depth of 10 still fine...
        assertNotNull(mapper.readTree(nestedMergeKeys(7)));

        // ... 11 is not
        Throwable failure = failureOnSmallStack(() -> mapper.readTree(nestedMergeKeys(8)));
        _assertNestingDepthFailure(failure, 11, 10);
    }

    /**
     * Nesting below the limit must keep working -- and on a small stack, to verify the
     * handling no longer consumes a stack frame per level.
     */
    @Test
    public void testMergeKeyNestingWithinLimit() throws Exception
    {
        final String doc = nestedMergeKeys(900);
        Throwable failure = failureOnSmallStack(() -> assertNotNull(MAPPER.readTree(doc)));
        if (failure != null) {
            throw new AssertionError("Failed to read document with 900 nested merge keys: "
                    +failure, failure);
        }
    }

    /**
     * Structural nesting and merge nesting share one limit: neither half of this document
     * exceeds the default 1000 on its own, but together they do.
     */
    @Test
    public void testCombinedStructuralAndMergeNesting() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 600; ++i) {
            sb.append("{a: ");
        }
        for (int i = 0; i < 600; ++i) {
            sb.append("{<<: ");
        }
        sb.append("{x: 1}");
        for (int i = 0; i < 1200; ++i) {
            sb.append('}');
        }
        final String doc = "result: "+sb+"\n";

        Throwable failure = failureOnSmallStack(() -> MAPPER.readTree(doc));
        _assertNestingDepthFailure(failure, 1001, 1000);
    }

    /**
     * Alias expansion must not corrupt depth accounting: the first replayed event used to
     * be returned without depth tracking, so every alias of a collection lowered the
     * tracked depth by one -- enough aliases up front and deep merge nesting slipped past
     * the limit entirely.
     */
    @Test
    public void testMergeNestingLimitNotBypassableViaAliases() throws Exception
    {
        StringBuilder sb = new StringBuilder("base: &b {x: 1}\n");
        for (int i = 0; i < 30; ++i) {
            sb.append("r").append(i).append(": *b\n");
        }
        sb.append("deep:\n  <<: ");
        for (int i = 0; i < 20; ++i) {
            sb.append("{<<: ");
        }
        sb.append("{x: 1}");
        for (int i = 0; i < 20; ++i) {
            sb.append('}');
        }
        final String doc = sb.append('\n').toString();

        final YAMLMapper mapper = new YAMLMapper(new YAMLAnchorReplayingFactory(
                YAMLFactory.builder()
                    .streamReadConstraints(StreamReadConstraints.builder()
                            .maxNestingDepth(10).build())
                    .build(),
                (ObjectCodec) null));
        Throwable failure = failureOnSmallStack(() -> mapper.readTree(doc));
        _assertNestingDepthFailure(failure, 11, 10);
    }

    /**
     * Merge keys that are siblings rather than nested must NOT count towards maximum
     * nesting depth: each one is popped again when its mapping ends, so a shallow
     * document may contain any number of them.
     */
    @Test
    public void testManySiblingMergeKeys() throws Exception
    {
        final int COUNT = 3 * StreamReadConstraints.DEFAULT_MAX_DEPTH;
        StringBuilder sb = new StringBuilder("base: &b {x: 1}\nitems:\n");
        for (int i = 0; i < COUNT; ++i) {
            sb.append("  k").append(i).append(":\n    <<: *b\n");
        }
        JsonNode root = MAPPER.readTree(sb.toString());
        assertEquals(COUNT, root.get("items").size());
        assertEquals(1, root.get("items").get("k0").get("x").intValue());
        assertEquals(1, root.get("items").get("k"+(COUNT-1)).get("x").intValue());
    }

    /**
     * Fetching the value of a merge key is the one remaining recursive call, and it can only
     * recurse if that value is the scalar {@code <<} itself -- which always fails on the
     * next event. Pinned here because it is what makes the recursion non-chaining, and so
     * bounded without a counter.
     */
    @Test
    public void testMergeKeyAsMergeValue() throws Exception
    {
        for (String doc : new String[] {
                "a: {<<: <<}\n",
                "a:\n  <<: <<\n",
                "a: {<<: {<<: <<}}\n"
        }) {
            Throwable failure = failureOnSmallStack(() -> MAPPER.readTree(doc));
            assertNotNull(failure, "Should have failed: "+doc);
            if (!(failure instanceof JsonProcessingException)) {
                throw new AssertionError("Expected JsonProcessingException for "+doc
                        +", got: "+failure, failure);
            }
            assertTrue(failure.getMessage().startsWith("found field '<<' but value isn't a map"),
                    "Unexpected message: "+failure.getMessage());
        }
    }

    /**
     * Merging flattens into a single level, so merged values are all visible as fields of
     * one object: verify the loop-based handling produces the same content as before.
     */
    @Test
    public void testNestedMergeKeysContent() throws Exception
    {
        final String EXP = "{\"result\":{\"x\":1}}";

        assertEquals(EXP, MAPPER.readTree(nestedMergeKeys(100)).toString());

        // and sanity-check the shallow, hand-written equivalent
        JsonNode root = MAPPER.readTree("result:\n  <<: {<<: {<<: {x: 1}}}\n");
        assertEquals(EXP, root.toString());
    }

    private void _assertNestingDepthFailure(Throwable failure, int depth, int max)
    {
        assertNotNull(failure, "Should have failed with StreamConstraintsException");
        if (!(failure instanceof StreamConstraintsException)) {
            throw new AssertionError("Expected StreamConstraintsException, got: "+failure,
                    failure);
        }
        assertEquals("Document nesting depth ("+depth+") exceeds the maximum allowed ("
                +max+", from `StreamReadConstraints.getMaxNestingDepth()`)",
                failure.getMessage());
    }

    private interface Body {
        void call() throws Exception;
    }

    /**
     * Runs given body on a thread with an explicitly small stack, so that recursive
     * handling of merge keys fails deterministically.
     *
     * @return Throwable body failed with, if any; {@code null} if it succeeded.
     *   Assertion failures from within the body are rethrown as-is instead, since those
     *   are test failures and not parser outcomes.
     */
    private Throwable failureOnSmallStack(Body body) throws Exception
    {
        final Throwable[] failure = new Throwable[1];
        Thread t = new Thread(null, () -> {
            try {
                body.call();
            } catch (Throwable e) {
                failure[0] = e;
            }
        }, "nested-merge-keys", SMALL_STACK);
        t.start();
        t.join();
        if (failure[0] instanceof AssertionError) {
            throw (AssertionError) failure[0];
        }
        return failure[0];
    }
}
