package com.fasterxml.jackson.dataformat.toml;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.io.IOContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringOutputUtilTest extends TomlMapperTestBase {
    @Test
    public void exhaustiveWriteReadTest() throws Exception {
        // this test attempts single-character writes for *all* code points, and sees whether they're read back
        // correctly.

        TomlStreamReadException.ErrorContext errorContext = new TomlStreamReadException.ErrorContext(null, null);

        // reused for performance
        StringBuilder builder = new StringBuilder();

        int nUnquoted = 0;
        int nLiteral = 0;
        int nBasicNoEscape = 0;
        int nBasic = 0;

        for (int c = 0; c <= Character.MAX_CODE_POINT; c++) {
            int cats = StringOutputUtil.categorize(c);
            builder.setLength(0);
            builder.appendCodePoint(c);
            String rawString = builder.toString();

            if ((cats & StringOutputUtil.UNQUOTED_KEY) != 0) {
                nUnquoted++;

                Lexer lexer = new Lexer(new StringReader(rawString), _ioContext(builder), errorContext);
                lexer.yybegin(Lexer.EXPECT_INLINE_KEY);
                assertEquals(TomlToken.UNQUOTED_KEY, lexer.yylex());
                lexer.releaseBuffers();
            }

            if ((cats & StringOutputUtil.LITERAL_STRING) != 0) {
                nLiteral++;

                builder.setLength(0);
                builder.append('\'');
                builder.appendCodePoint(c);
                builder.append('\'');

                Lexer lexer = new Lexer(new StringReader(builder.toString()), _ioContext(builder), errorContext);
                lexer.yybegin(Lexer.EXPECT_VALUE);
                assertEquals(TomlToken.STRING, lexer.yylex());
                assertEquals(rawString, lexer.textBuffer.contentsAsString());
                lexer.releaseBuffers();
            }

            if ((cats & StringOutputUtil.BASIC_STRING_NO_ESCAPE) != 0) {
                nBasicNoEscape++;

                builder.setLength(0);
                builder.append('"');
                builder.appendCodePoint(c);
                builder.append('"');

                Lexer lexer = new Lexer(new StringReader(builder.toString()), _ioContext(builder), errorContext);
                lexer.yybegin(Lexer.EXPECT_VALUE);
                assertEquals(TomlToken.STRING, lexer.yylex());
                assertEquals(rawString, lexer.textBuffer.contentsAsString());
                lexer.releaseBuffers();
            }

            if ((cats & StringOutputUtil.BASIC_STRING) != 0 && c < 0x10000) {
                nBasic++;

                builder.setLength(0);
                builder.append('"');
                String escape = StringOutputUtil.getBasicStringEscape((char) c);
                if (escape == null) {
                    builder.append((char) c);
                } else {
                    builder.append(escape);
                }
                builder.append('"');

                Lexer lexer = new Lexer(new StringReader(builder.toString()),
                        _ioContext(builder), errorContext);
                lexer.yybegin(Lexer.EXPECT_VALUE);
                assertEquals(TomlToken.STRING, lexer.yylex());
                assertEquals(rawString, lexer.textBuffer.contentsAsString());
                lexer.releaseBuffers();
            }
        }

        assertEquals(26 * 2 + 10 + 2, nUnquoted);
        assertTrue(nBasic > 10000);
        assertTrue(nBasicNoEscape > 10000);
        assertTrue(nLiteral > 10000);
    }

    private IOContext _ioContext(CharSequence toml) {
        return testIOContext();
    }
}
