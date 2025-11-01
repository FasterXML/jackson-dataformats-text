package com.fasterxml.jackson.dataformat.yaml.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

// [dataformats-text#296]: YAML Anchor and alias fails with ObjectIdGenerators.None
public class ObjectIdNone296Test extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newObjectMapper();

    @JacksonTestFailureExpected
    @Test
    public void testObjectIdUsingNativeAnchorsWithNoneGenerator() throws Exception
    {
        // YAML content with anchor (&foo1) and alias (*foo1)
        final String YAML_CONTENT =
                "foo: &foo1\n" +
                "  value: bar\n" +
                "boo: *foo1\n";

        // This should work: YAML anchors/aliases should be recognized natively
        // when using ObjectIdGenerators.None, but currently fails with:
        // "Cannot construct instance of StringHolder... no String-argument
        // constructor/factory method to deserialize from String value ('foo1')"
        ScratchModel result = MAPPER.readValue(YAML_CONTENT, ScratchModel.class);

        assertNotNull(result);
        assertNotNull(result.foo);
        assertEquals("bar", result.foo.value);
        assertNotNull(result.boo);
        assertEquals("bar", result.boo.value);
        // The key assertion: both fields should point to the same object instance
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
