package com.fasterxml.jackson.dataformat.toml;

import org.intellij.lang.annotations.Language;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LexerTest {
    private List<TomlToken> tokenize(@Language("toml") String s) throws IOException {
        Lexer lexer = new Lexer(new StringReader(s));
        List<TomlToken> collected = new ArrayList<>();
        while (!lexer.yyatEOF()) {
            TomlToken token = lexer.yylex();
            if (token == null) {
                Assert.assertTrue(lexer.yyatEOF());
                break;
            }
            collected.add(token);
        }
        return collected;
    }

    @Test
    public void values() throws IOException {
        Assert.assertEquals(
                Arrays.asList(TomlToken.UNQUOTED_KEY, TomlToken.KEY_VAL_SEP, TomlToken.STRING),
                tokenize("key = \"abc\"")
        );
        Assert.assertEquals(
                Arrays.asList(TomlToken.UNQUOTED_KEY, TomlToken.KEY_VAL_SEP, TomlToken.TRUE),
                tokenize("key = true")
        );
        Assert.assertEquals(
                Arrays.asList(TomlToken.UNQUOTED_KEY, TomlToken.KEY_VAL_SEP, TomlToken.LOCAL_DATE),
                tokenize("key = 2021-03-24")
        );
        Assert.assertEquals(
                Arrays.asList(TomlToken.UNQUOTED_KEY, TomlToken.KEY_VAL_SEP, TomlToken.INTEGER),
                tokenize("key = 123")
        );
        Assert.assertEquals(
                Arrays.asList(TomlToken.UNQUOTED_KEY, TomlToken.KEY_VAL_SEP, TomlToken.FLOAT),
                tokenize("key = 123.456")
        );
    }
}