package com.fasterxml.jackson.dataformat.yaml.misc;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

//for [dataformats-text#123], problem with YAML, Object Ids
public class ObjectId123Test extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newObjectMapper();
    
    @Test
    public void testObjectIdUsingNative() throws Exception
    {
        final String YAML_CONTENT =
                "foo: &foo1\n" +
                "  value: bar\n" +
                "boo: *foo1\n";
        ScratchModel result = MAPPER.readValue(YAML_CONTENT, ScratchModel.class);
        assertNotNull(result);
        assertNotNull(result.foo);
        assertNotNull(result. boo);
        assertSame(result.foo, result.boo);
    }

    static class ScratchModel {
        public StringHolder foo;
        public StringHolder boo;
    }

//    @JsonIdentityInfo(generator = ObjectIdGenerators.None.class)
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
    static class StringHolder {
        public String value;

        @Override
        public String toString() {
            return value;
        }
    }
}
