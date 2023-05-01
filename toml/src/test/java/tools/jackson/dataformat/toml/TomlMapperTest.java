package tools.jackson.dataformat.toml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Objects;

import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.WrappedIOException;
import tools.jackson.databind.node.JsonNodeFactory;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TomlMapperTest extends TomlMapperTestBase {
    @SuppressWarnings("deprecation")
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

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
        Assert.assertEquals(TEST_OBJECT, newTomlMapper().readValue(TEST_STRING, TestClass.class));
    }

    @Test
    public void bytes() {
        Assert.assertEquals(TEST_OBJECT, newTomlMapper().readValue(TEST_STRING.getBytes(StandardCharsets.UTF_8), TestClass.class));
    }

    @Test
    public void stream() {
        Assert.assertEquals(TEST_OBJECT, newTomlMapper().readValue(new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)), TestClass.class));
    }

    @Test
    public void reader() {
        Assert.assertEquals(TEST_OBJECT, newTomlMapper().readValue(new StringReader(TEST_STRING), TestClass.class));
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

    @Test
    public void bigInteger() {
        Assert.assertEquals(
                JsonNodeFactory.instance.objectNode()
                        .put("abc", new BigInteger("ffffffffffffffffffff", 16)),
                TomlMapper.builder()
                        .build()
                        .readTree("abc = 0xffffffffffffffffffff")
        );
    }

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    @Test
    public void bigDecimal() {
        BigDecimal testValue = BigDecimal.valueOf(Double.MIN_VALUE).divide(BigDecimal.valueOf(2));
        Assert.assertEquals(
                JsonNodeFactory.instance.objectNode()
                        .put("abc", testValue),
                newTomlMapper().readTree("abc = " + testValue.toString())
        );
    }

    @Test
    public void veryBigDecimal() throws Exception {
        final int len = 1200;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(1);
        }
        final String value = sb.toString();
        try {
            newTomlMapper().readTree("abc = " + value);
            Assert.fail("expected TomlStreamReadException");
        } catch (TomlStreamReadException e) {
            Assert.assertTrue("unexpected message: " + e.getMessage(),
                    e.getMessage().contains("Number value length (1200) exceeds the maximum allowed"));
        }
    }

    @Test
    public void veryBigDecimalWithNumLenUnlimited() throws Exception {
        final int len = 1200;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(1);
        }
        final String value = sb.toString();
        BigDecimal testValue = new BigDecimal(value);
        TomlFactory factory = TomlFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
                .build();
        TomlMapper mapper = newTomlMapper(factory);
        Assert.assertEquals(
                testValue,
                mapper.readTree("abc = " + value).get("abc").decimalValue()
        );
    }

    @Test
    public void temporalFieldFlag() throws Exception {
        Assert.assertEquals(
                LocalDate.of(2021, 3, 26),
                TomlMapper.builder()
                        .enable(TomlReadFeature.PARSE_JAVA_TIME)
                        .build()
                        .readValue("foo = 2021-03-26", ObjectField.class).foo
        );
        Assert.assertEquals(
                "2021-03-26",
                newTomlMapper().readValue("foo = 2021-03-26", ObjectField.class).foo
        );
    }

    public static class ObjectField {
        public Object foo;
    }

    @Test
    public void testIoException() {
        Assert.assertThrows(WrappedIOException.class, () -> TomlMapper.shared().readTree(new Reader() {
            @Override
            public int read(@NotNull char[] cbuf, int off, int len) throws IOException {
                throw new IOException("Test");
            }

            @Override
            public void close() throws IOException {
            }
        }));
    }

    @Test
    public void nullCoercion() {
        Assert.assertNull(TomlMapper.builder().build().readValue("foo = ''", ComplexField.class).foo);
    }

    public static class ComplexField {
        public ComplexField foo;
    }

    @Test
    public void nullEnabledDefault() {
        ComplexField cf = new ComplexField();
        cf.foo = null;
        Assert.assertEquals("foo = ''\n", TomlMapper.builder().build().writeValueAsString(cf));
    }

    @Test(expected = JacksonException.class)
    public void nullDisable() {
        ComplexField cf = new ComplexField();
        cf.foo = null;
        Assert.assertEquals("foo = ''\n", TomlMapper.builder()
                .enable(TomlWriteFeature.FAIL_ON_NULL_WRITE)
                .build().writeValueAsString(cf));
    }
}
