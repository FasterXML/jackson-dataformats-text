package com.fasterxml.jackson.dataformat.yaml.type;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.yaml.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TypeIdTest extends ModuleTestBase
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(Impl.class) })
    static abstract class Base {
        public int a;
        
        public Base() { }
        public Base(int a) {
            this.a = a;
        }
    }

    @JsonTypeName("impl")
    static class Impl extends Base {
        public Impl() { }
        public Impl(int a) { super(a); }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    public void testNativeSerialization() throws Exception
    {
        ObjectMapper mapper = newObjectMapper();
        String yaml = mapper.writeValueAsString(new Impl(13));
        yaml = yaml.trim();
        assertEquals("--- !<impl>\na: 13", yaml);
    }

    @Test
    public void testNonNativeSerialization() throws Exception
    {
        YAMLMapper mapper = newObjectMapper();
        mapper.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
        String yaml = mapper.writeValueAsString(new Impl(13));
        yaml = yaml.trim();
        assertEquals("---\ntype: \"impl\"\na: 13", yaml);

        // Let's also round-trip it
        Base back = mapper.readValue(yaml, Impl.class);
        assertNotNull(back);
        assertEquals(Impl.class, back.getClass());
    }
    
    @Test
    public void testDeserialization() throws Exception
    {
        /* Looks like there are couple of alternative ways to indicate
         * type ids... so let's verify variations we know of.
         */
        ObjectMapper mapper = newObjectMapper();
        
        for (String typeId : new String[] {
                "--- !<impl>",
                "--- !impl",
                "!<impl>",
                "!impl",
                // 04-May-2014, tatu: I _think_ we should support this too but...
//                "---\nTYPE: impl\n",
            }) {
            final String input = typeId + "\na: 13";
            Base result = mapper.readValue(input, Base.class);
            _verify(result);
        }
    }

    @Test
    public void testRoundtripWithBuffer() throws Exception
    {
        ObjectMapper mapper = newObjectMapper();
        TokenBuffer tbuf = mapper.readValue("--- !impl\na: 13\n", TokenBuffer.class);
        assertNotNull(tbuf);
        Base result = mapper.readValue(tbuf.asParser(), Base.class);
        tbuf.close();
        _verify(result);
    }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private void _verify(Base result)
    {
        assertNotNull(result);
        assertEquals(Impl.class, result.getClass());
        Impl i = (Impl) result;
        assertEquals(13, i.a);
    }
}
