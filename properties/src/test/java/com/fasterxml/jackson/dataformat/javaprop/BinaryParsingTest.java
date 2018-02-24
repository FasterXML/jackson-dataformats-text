package com.fasterxml.jackson.dataformat.javaprop;

import static org.junit.Assert.assertArrayEquals;

import java.nio.ByteBuffer;
import java.util.Properties;

public class BinaryParsingTest extends ModuleTestBase
{
    // [dataformats-text#74]: problem with multiple binary fields
    static class MyBean {
        public byte[] a;
        public byte[] b;
        public ByteBuffer c;

        protected MyBean() { }
        public MyBean(boolean bogus) {
            a = new byte[] { 1 };
            b = new byte[] { 3, 28, 7 };
            c = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5, 67 });
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final JavaPropsMapper MAPPER = mapperForProps();

    // [dataformats-text#74]
    public void testMultipleBinaryFields() throws Exception
    {
        MyBean src = new MyBean(true);
        Properties props = MAPPER.writeValueAsProperties(src);

        MyBean result = MAPPER.readPropertiesAs(props, MyBean.class);
        assertArrayEquals(src.a, result.a);
        assertArrayEquals(src.b, result.b);
        ByteBuffer b1 = src.c;
        ByteBuffer b2 = result.c;

        assertEquals(b1, b2);
    }
}
