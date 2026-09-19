package tools.jackson.dataformat.toml;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

import tools.jackson.core.io.IOContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringOutputUtilTest extends TomlMapperTestBase {
    // Categorization of a single char via `int`, `String` and `char[]` must agree
    // (ASCII goes through a lookup table; rest through full logic)
    @Test
    public void categorizeConsistency() {
        char[] buf = new char[3];
        for (int c = 0; c < 0x10000; c++) {
            if (Character.isSurrogate((char) c)) {
                continue; // lone surrogates handled separately below
            }
            int expected = StringOutputUtil.categorize(c);
            String str = String.valueOf((char) c);
            assertEquals(expected, StringOutputUtil.categorize(str), "String, c=0x" + Integer.toHexString(c));
            buf[0] = 'x'; buf[1] = (char) c; buf[2] = 'y';
            assertEquals(expected, StringOutputUtil.categorize(buf, 1, 1), "char[], c=0x" + Integer.toHexString(c));
        }
        // lone surrogate: not writable
        assertEquals(0, StringOutputUtil.categorize("\ud800"));
        assertEquals(0, StringOutputUtil.categorize(new char[] { '\udc00' }, 0, 1));
        // surrogate pair: non-ASCII content, allowed in all string types
        int nonAscii = StringOutputUtil.categorize(0x1F600);
        assertEquals(nonAscii, StringOutputUtil.categorize("\ud83d\ude00"));
        assertEquals(nonAscii, StringOutputUtil.categorize(new char[] { 'a', '\ud83d', '\ude00' }, 1, 2));
        // out of range for `int` variant
        assertEquals(0, StringOutputUtil.categorize(Character.MAX_CODE_POINT + 1));
    }

    // Categories of a String are the AND of categories of its characters
    @Test
    public void categorizeStrings() {
        String[] samples = new String[] {
            "", "abc", "a-b_c9", "a b", "a\"b", "a'b", "a\\b", "a\nb", "\t", "\u007f",
            "caf\u00e9", "\u65e5\u672c", "\ufeff", "a\ufeffb", "\ud83d\ude00x",
        };
        for (String str : samples) {
            int expected = -1;
            for (int i = 0; i < str.length();) {
                int cp = str.codePointAt(i);
                expected &= StringOutputUtil.categorize(cp);
                i += Character.charCount(cp);
            }
            if (str.isEmpty()) {
                expected = StringOutputUtil.categorize("");
            }
            assertEquals(expected, StringOutputUtil.categorize(str), str);
            char[] padded = ("<" + str + ">").toCharArray();
            assertEquals(expected, StringOutputUtil.categorize(padded, 1, str.length()), str);
        }
        assertEquals(StringOutputUtil.UNQUOTED_KEY, StringOutputUtil.categorize("abc") & StringOutputUtil.UNQUOTED_KEY);
        assertEquals(0, StringOutputUtil.categorize("a b") & StringOutputUtil.UNQUOTED_KEY);
        assertEquals(0, StringOutputUtil.categorize("a'b") & StringOutputUtil.LITERAL_STRING);
        assertEquals(0, StringOutputUtil.categorize("caf\u00e9") & StringOutputUtil.ASCII_ONLY);
    }

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
