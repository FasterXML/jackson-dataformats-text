package tools.jackson.dataformat.javaprop;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.core.JacksonException;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.InputDecorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// [jackson-dataformats-text#720]: a failed File/Path parse must not close the
// source twice. The base factory already closes what it opened.
public class ParserSourceCloseOnFailureTest extends ModuleTestBase
{
    @TempDir
    Path tempDir;

    @Test
    public void failedFileParseClosesDecoratedSourceOnce() throws Exception {
        Path file = tempDir.resolve("bad.properties");
        Files.writeString(file, "key=\\uZZZZ\n");
        CountingDecorator decorator = new CountingDecorator();
        JavaPropsFactory factory = propertiesFactoryBuilder().inputDecorator(decorator).build();

        assertThatThrownBy(() -> factory.createParser(ObjectReadContext.empty(), file.toFile()))
            .isInstanceOf(JacksonException.class)
            .satisfies(ex -> assertThat(ex.getSuppressed()).isEmpty());
        assertThat(decorator.closes).isEqualTo(1);
    }

    @Test
    public void failedPathParseClosesDecoratedSourceOnce() throws Exception {
        Path file = tempDir.resolve("bad-path.properties");
        Files.writeString(file, "key=\\uZZZZ\n");
        CountingDecorator decorator = new CountingDecorator();
        JavaPropsFactory factory = propertiesFactoryBuilder().inputDecorator(decorator).build();

        assertThatThrownBy(() -> factory.createParser(ObjectReadContext.empty(), file))
            .isInstanceOf(JacksonException.class)
            .satisfies(ex -> assertThat(ex.getSuppressed()).isEmpty());
        assertThat(decorator.closes).isEqualTo(1);
    }

    @Test
    public void successfulFileParseClosesDecoratedSourceOnce() throws Exception {
        Path file = tempDir.resolve("ok.properties");
        Files.writeString(file, "key=value\n");
        CountingDecorator decorator = new CountingDecorator();
        JavaPropsFactory factory = propertiesFactoryBuilder().inputDecorator(decorator).build();

        try (var parser = factory.createParser(ObjectReadContext.empty(), file.toFile())) {
            assertThat(parser.nextToken()).isNotNull();
        }
        assertThat(decorator.closes).isEqualTo(1);
    }

    static final class CountingDecorator extends InputDecorator {
        int closes;

        @Override
        public InputStream decorate(IOContext ctxt, InputStream in) {
            return new OnceInputStream(in, this);
        }

        @Override
        public InputStream decorate(IOContext ctxt, byte[] src, int offset, int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.io.Reader decorate(IOContext ctxt, java.io.Reader src) {
            throw new UnsupportedOperationException();
        }
    }

    static final class OnceInputStream extends FilterInputStream {
        private final CountingDecorator _owner;
        private boolean _closed;

        OnceInputStream(InputStream in, CountingDecorator owner) {
            super(in);
            _owner = owner;
        }

        @Override
        public void close() throws IOException {
            if (_closed) {
                throw new IOException("already closed");
            }
            _closed = true;
            _owner.closes++;
            super.close();
        }
    }
}
