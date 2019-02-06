package com.fasterxml.jackson.dataformat.yaml.failing;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.*;

// for [dataformats-text#63], problem with YAML, Object Ids
public class ObjectId63Test extends ModuleTestBase
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    public static class SimplePojo {
        String id;
        String value;

        public String getId() {
            return this.id;
        }

        public void setId(final String newId) {
            this.id = newId;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(final String newValue) {
            this.value = newValue;
        }
    }

    private final ObjectMapper MAPPER = newObjectMapper();

    public void testIssue63() throws Exception
    {
        final SimplePojo simplePojoWithId = new SimplePojo();
        simplePojoWithId.setId("myId");
        simplePojoWithId.setValue("Value");

        final SimplePojo simplePojoWithoutId = new SimplePojo();
        simplePojoWithoutId.setValue("Value");

        assertEquals("---\n&myId id: \"myId\"\nvalue: \"Value\"",
                MAPPER.writeValueAsString(simplePojoWithId).trim());

        // `null` object id is not to be written as anchor but skipped; property itself
        // follows regular inclusion rules.
        assertEquals("---\nid: null\nvalue: \"Value\"",
                MAPPER.writeValueAsString(simplePojoWithoutId).trim());
    }
}
