package com.fasterxml.jackson.dataformat.yaml.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLAnchorReplayingFactory;
import com.fasterxml.jackson.dataformat.yaml.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * [dataformats-text#296]: YAML Anchor and alias with ObjectIdGenerators.None
 *<p>
 * NOTE: ObjectIdGenerators.None with YAML anchors/aliases currently does NOT work
 * as expected due to a limitation in Jackson's databind layer.
 *<p>
 * While the parser correctly exposes object IDs via getObjectId(), Jackson's
 * object ID resolution doesn't properly cache and reuse objects when using
 * ObjectIdGenerators.None, even with YAMLAnchorReplayingFactory.
 *<p>
 * WORKAROUND: Users should use ObjectIdGenerators.StringIdGenerator (or other
 * generators) instead of ObjectIdGenerators.None for YAML anchor/alias support.
 */
public class ObjectIdNone296Test extends ModuleTestBase
{
    // YAMLAnchorReplayingFactory properly replays anchored events, but
    // ObjectIdGenerators.None still doesn't work for de-duplication
    private final ObjectMapper MAPPER = YAMLMapper.builder(new YAMLAnchorReplayingFactory()).build();

    @JacksonTestFailureExpected
    @Test
    public void testObjectIdUsingNativeAnchorsWithNoneGenerator() throws Exception
    {
        // YAML content with anchor (&foo1) and alias (*foo1)
        final String YAML_CONTENT =
                "foo: &foo1\n" +
                "  value: bar\n" +
                "boo: *foo1\n";

        ScratchModel result = MAPPER.readValue(YAML_CONTENT, ScratchModel.class);

        assertNotNull(result);
        assertNotNull(result.foo);
        assertEquals("bar", result.foo.value);
        assertNotNull(result.boo);
        assertEquals("bar", result.boo.value);
        // The key assertion: both fields should point to the same object instance
        // Currently FAILS - creates two separate instances with same content
        assertSame(result.foo, result.boo);
    }

    static class ScratchModel {
        public StringHolder foo;
        public StringHolder boo;
    }

    // Using ObjectIdGenerators.None should allow YAML's native anchor/alias to work
    @JsonIdentityInfo(generator = ObjectIdGenerators.None.class)
    static class StringHolder {
        public String value;

        protected StringHolder() { }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public StringHolder(String v) { value = v; }

        @Override
        public String toString() {
            return "StringHolder{" + value +" }";
        }
    }
}
