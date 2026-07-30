package com.fasterxml.jackson.dataformat.yaml.constraints;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLAnchorReplayingFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    private final ObjectMapper MAPPER = new ObjectMapper(F);

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
        Throwable failure = failureOnSmallStack(new Body() {
            @Override
            public void call() throws Exception {
                try (JsonParser p = F.createParser(doc)) {
                    while (p.nextToken() != null) { }
                }
            }
        });
        _assertNestingDepthFailure(failure, 1001, 1000);
    }

    @Test
    public void testDeeplyNestedMergeKeysDatabind() throws Exception
    {
        final String doc = nestedMergeKeys(REPORTED_DEPTH);
        Throwable failure = failureOnSmallStack(new Body() {
            @Override
            public void call() throws Exception {
                MAPPER.readTree(doc);
            }
        });
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
        final ObjectMapper mapper = new ObjectMapper(f);

        // NOTE: `nestedMergeKeys(n)` has n+1 merge keys (outer one, plus n nested)

        // 10 levels of merge nesting still fine...
        assertNotNull(mapper.readTree(nestedMergeKeys(9)));

        // ... 11 is not
        Throwable failure = failureOnSmallStack(new Body() {
            @Override
            public void call() throws Exception {
                mapper.readTree(nestedMergeKeys(10));
            }
        });
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
        Throwable failure = failureOnSmallStack(new Body() {
            @Override
            public void call() throws Exception {
                assertNotNull(MAPPER.readTree(doc));
            }
        });
        if (failure != null) {
            throw new AssertionError("Failed to read document with 900 nested merge keys: "
                    +failure, failure);
        }
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
     * Runs given body on a thread with an explicitly small stack.
     *
     * @return Throwable body failed with, if any; {@code null} if it succeeded
     */
    private Throwable failureOnSmallStack(final Body body) throws Exception
    {
        final Throwable[] failure = new Throwable[1];
        Thread t = new Thread(null, new Runnable() {
            @Override
            public void run() {
                try {
                    body.call();
                } catch (Throwable e) {
                    failure[0] = e;
                }
            }
        }, "nested-merge-keys", SMALL_STACK);
        t.start();
        t.join();
        return failure[0];
    }
}
