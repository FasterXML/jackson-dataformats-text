package com.fasterxml.jackson.dataformat.toml;

import org.intellij.lang.annotations.Language;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TomlMapperTest {
    @Language("toml")
    private static final String TEST_STRING = "foo = 'bar'\n[nested]\nfoo = 4";
    private static final TestClass TEST_OBJECT;

    static {
        TEST_OBJECT = new TestClass();
        TEST_OBJECT.foo = "bar";
        TEST_OBJECT.nested = new TestClass.Nested();
        TEST_OBJECT.nested.foo = 4;
    }

    @Test
    public void string() {
        Assert.assertEquals(TEST_OBJECT, TomlMapper.shared().readValue(TEST_STRING, TestClass.class));
    }

    @Test
    public void bytes() {
        Assert.assertEquals(TEST_OBJECT, TomlMapper.shared().readValue(TEST_STRING.getBytes(StandardCharsets.UTF_8), TestClass.class));
    }

    @Test
    public void stream() {
        Assert.assertEquals(TEST_OBJECT, TomlMapper.shared().readValue(new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)), TestClass.class));
    }

    @Test
    public void reader() {
        Assert.assertEquals(TEST_OBJECT, TomlMapper.shared().readValue(new StringReader(TEST_STRING), TestClass.class));
    }

    public static class TestClass {
        public String foo;
        public Nested nested;

        public static class Nested {
            public int foo;

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Nested)) return false;
                Nested nested = (Nested) o;
                return foo == nested.foo;
            }

            @Override
            public int hashCode() {
                return Objects.hash(foo);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TestClass)) return false;
            TestClass testClass = (TestClass) o;
            return Objects.equals(foo, testClass.foo) && Objects.equals(nested, testClass.nested);
        }

        @Override
        public int hashCode() {
            return Objects.hash(foo, nested);
        }
    }
}