package com.fasterxml.jackson.dataformat.yaml.constraints;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLAnchorReplayingFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLAnchorReplayingParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for the {@code MAX_xxx} limits of {@link YAMLAnchorReplayingParser}: exceeding one
 * must fail with a message naming both the count and the limit that was exceeded.
 */
public class AnchorReplayLimitsTest
    extends ModuleTestBase
{
    private final YAMLAnchorReplayingFactory F = new YAMLAnchorReplayingFactory();

    private void _readAll(String doc) throws Exception {
        try (JsonParser p = F.createParser(doc)) {
            while (p.nextToken() != null) { }
        }
    }

    @Test
    public void testTooManyReferences() throws Exception
    {
        // Each anchored scalar is remembered as a reference
        final int COUNT = YAMLAnchorReplayingParser.MAX_REFS + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < COUNT; ++i) {
            sb.append("k").append(i).append(": &a").append(i).append(" 1\n");
        }
        try {
            _readAll(sb.toString());
            fail("Should not pass");
        } catch (StreamConstraintsException e) {
            assertEquals("too many references in the document: "+COUNT
                    +" exceeds the maximum allowed ("+YAMLAnchorReplayingParser.MAX_REFS
                    +", from `YAMLAnchorReplayingParser.MAX_REFS`)",
                    e.getMessage());
        }
    }

    @Test
    public void testTooManyEventsToReplay() throws Exception
    {
        // One anchored sequence long enough that its recorded events exceed the limit
        final int COUNT = YAMLAnchorReplayingParser.MAX_EVENTS + 10;
        StringBuilder sb = new StringBuilder("big: &big [");
        for (int i = 0; i < COUNT; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i);
        }
        sb.append("]\n");
        try {
            _readAll(sb.toString());
            fail("Should not pass");
        } catch (StreamConstraintsException e) {
            assertEquals("too many events to replay: "
                    +(YAMLAnchorReplayingParser.MAX_EVENTS + 1)
                    +" exceeds the maximum allowed ("+YAMLAnchorReplayingParser.MAX_EVENTS
                    +", from `YAMLAnchorReplayingParser.MAX_EVENTS`)",
                    e.getMessage());
        }
    }
}
