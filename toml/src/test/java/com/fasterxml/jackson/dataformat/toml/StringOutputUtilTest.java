package com.fasterxml.jackson.dataformat.toml;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

public class StringOutputUtilTest {
    @Test
    public void exhaustiveWriteReadTest() throws IOException {
        // this test attempts single-character writes for *all* code points, and sees whether they're read back
        // correctly.

        TomlStreamReadException.ErrorContext errorContext = new TomlStreamReadException.ErrorContext(null, null);

        // reused for performance
        StringBuilder builder = new StringBuilder();
        Lexer lexer = new Lexer(null, errorContext);

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

                lexer.yyreset(new StringReader(rawString));
                lexer.yybegin(Lexer.EXPECT_INLINE_KEY);
                Assert.assertEquals(TomlToken.UNQUOTED_KEY, lexer.yylex());
            }

            if ((cats & StringOutputUtil.LITERAL_STRING) != 0) {
                nLiteral++;

                builder.setLength(0);
                builder.append('\'');
                builder.appendCodePoint(c);
                builder.append('\'');

                lexer.yyreset(new StringReader(builder.toString()));
                lexer.yybegin(Lexer.EXPECT_VALUE);
                Assert.assertEquals(TomlToken.STRING, lexer.yylex());
                Assert.assertEquals(rawString, lexer.stringBuilder.toString());
            }

            if ((cats & StringOutputUtil.BASIC_STRING_NO_ESCAPE) != 0) {
                nBasicNoEscape++;

                builder.setLength(0);
                builder.append('"');
                builder.appendCodePoint(c);
                builder.append('"');

                lexer.yyreset(new StringReader(builder.toString()));
                lexer.yybegin(Lexer.EXPECT_VALUE);
                Assert.assertEquals(TomlToken.STRING, lexer.yylex());
                Assert.assertEquals(rawString, lexer.stringBuilder.toString());
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

                lexer.yyreset(new StringReader(builder.toString()));
                lexer.yybegin(Lexer.EXPECT_VALUE);
                Assert.assertEquals(TomlToken.STRING, lexer.yylex());
                Assert.assertEquals(rawString, lexer.stringBuilder.toString());
            }
        }

        Assert.assertEquals(26 * 2 + 10 + 2, nUnquoted);
        Assert.assertTrue(nBasic > 10000);
        Assert.assertTrue(nBasicNoEscape > 10000);
        Assert.assertTrue(nLiteral > 10000);
    }
}