package com.fasterxml.jackson.dataformat.csv.ser;

import java.io.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

/**
 * Tests to verify that most core Jackson components can be serialized
 * using default JDK serialization: this feature is useful for some
 * platforms, such as Android, where memory management is handled
 * much more aggressively.
 */
public class JDKSerializationTest extends ModuleTestBase
{
    @JsonPropertyOrder({ "x", "y" })
    static class MyPojo {
        public int x;
        protected int y;
        
        public MyPojo() { }
        public MyPojo(int x0, int y0) {
            x = x0;
            y = y0;
        }
        
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
    }

    static class MyPojo2 extends MyPojo { }
    
    /*
    /**********************************************************
    /* Tests for individual objects
    /**********************************************************
     */

    // Let's not use the shared instance, but local one here.
    private final CsvMapper MAPPER = mapperForCsv();

    private final CsvSchema SCHEMA_POJO = MAPPER.schemaFor(MyPojo.class);

    public void testSchema() throws IOException
    {
        byte[] ser = jdkSerialize(SCHEMA_POJO);
        CsvSchema out = (CsvSchema) jdkDeserialize(ser);
        assertNotNull(out);
    }
    
    public void testObjectMapper() throws IOException
    {
        final String EXP_CSV = "2,3";
        final MyPojo p = new MyPojo(2, 3);
        assertEquals(EXP_CSV, MAPPER.writerFor(MyPojo.class)
                .with(SCHEMA_POJO).writeValueAsString(p).trim());

        byte[] bytes = jdkSerialize(MAPPER);
        CsvMapper mapper2 = jdkDeserialize(bytes);

        assertEquals(EXP_CSV, mapper2.writerFor(MyPojo.class)
                .with(SCHEMA_POJO).writeValueAsString(p).trim());
        MyPojo p2 = mapper2.readerFor(MyPojo.class).with(SCHEMA_POJO).readValue(EXP_CSV);
        assertEquals(p.x, p2.x);
        assertEquals(p.y, p2.y);

        // and just to be sure, try something different...
        String csv = mapper2.writerFor(MyPojo2.class)
                .with(mapper2.schemaFor(MyPojo2.class))
                .writeValueAsString(new MyPojo2());
        assertNotNull(csv);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    protected byte[] jdkSerialize(Object o) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1000);
        ObjectOutputStream obOut = new ObjectOutputStream(bytes);
        obOut.writeObject(o);
        obOut.close();
        return bytes.toByteArray();
    }

    @SuppressWarnings("unchecked")
    protected <T> T jdkDeserialize(byte[] raw) throws IOException
    {
        ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(raw));
        try {
            return (T) objIn.readObject();
        } catch (ClassNotFoundException e) {
            fail("Missing class: "+e.getMessage());
            return null;
        } finally {
            objIn.close();
        }
    }

}
