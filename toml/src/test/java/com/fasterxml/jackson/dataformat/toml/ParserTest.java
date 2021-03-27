package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.intellij.lang.annotations.Language;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@SuppressWarnings("OctalInteger")
public class ParserTest {
    private final ParserOptions testOptions = new ParserOptions(false, true);
    private final ObjectMapper jsonMapper = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .build();

    private ObjectNode json(@Language("json") String json) {
        return (ObjectNode) jsonMapper.readTree(json);
    }

    private ObjectNode toml(@Language("toml") String toml) throws IOException {
        return toml(testOptions, toml);
    }

    private ObjectNode toml(ParserOptions opts, @Language("toml") String toml) throws IOException {
        return Parser.parse(new JacksonTomlParseException.ErrorContext(null, null), opts, new StringReader(toml));
    }

    @SuppressWarnings("deprecation")
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void unclosed() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("EOF");
        toml("\"abc");
    }

    // from the manual

    @Test
    public void keyValuePair() throws IOException {
        Assert.assertEquals(
                json("{\"key\": \"value\"}"),
                toml("key = \"value\""));
    }

    @Test(expected = JacksonTomlParseException.class)
    public void unspecified() throws IOException {
        toml("key =");
    }

    @Test(expected = JacksonTomlParseException.class)
    public void singleLine() throws IOException {
        toml("first = \"Tom\" last = \"Preston-Werner\"");
    }

    @Test
    public void comment() throws IOException {
        Assert.assertEquals(
                json("{\"key\": \"value\", \"another\": \"# This is not a comment\"}"),
                toml("# This is a full-line comment\n" +
                        "key = \"value\"  # This is a comment at the end of a line\n" +
                        "another = \"# This is not a comment\""));
    }

    @Test
    public void bareKeys() throws IOException {
        Assert.assertEquals(
                json("{\"key\": \"value\", \"bare_key\": \"value\", \"bare-key\": \"value\", \"1234\": \"value\"}"),
                toml("key = \"value\"\n" +
                        "bare_key = \"value\"\n" +
                        "bare-key = \"value\"\n" +
                        "1234 = \"value\""));
    }

    @Test
    public void quotedKeys() throws IOException {
        Assert.assertEquals(
                json("{\"127.0.0.1\": \"value\", \"character encoding\": \"value\", \"ʎǝʞ\": \"value\", \"key2\": \"value\", \"quoted \\\"value\\\"\": \"value\"}"),
                toml("\"127.0.0.1\" = \"value\"\n" +
                        "\"character encoding\" = \"value\"\n" +
                        "\"ʎǝʞ\" = \"value\"\n" +
                        "'key2' = \"value\"\n" +
                        "'quoted \"value\"' = \"value\""));
    }

    @Test(expected = JacksonTomlParseException.class)
    public void bareKeyNonEmpty() throws IOException {
        toml("= \"no key name\"");
    }

    @Test
    public void quotedKeyEmpty() throws IOException {
        Assert.assertEquals(json("{\"\": \"blank\"}"), toml("\"\" = \"blank\""));
        Assert.assertEquals(json("{\"\": \"blank\"}"), toml("'' = 'blank'"));
    }

    @Test
    public void dottedKeys() throws IOException {
        Assert.assertEquals(
                json("{\n" +
                        "  \"name\": \"Orange\",\n" +
                        "  \"physical\": {\n" +
                        "    \"color\": \"orange\",\n" +
                        "    \"shape\": \"round\"\n" +
                        "  },\n" +
                        "  \"site\": {\n" +
                        "    \"google.com\": true\n" +
                        "  }\n" +
                        "}"),
                toml("name = \"Orange\"\n" +
                        "physical.color = \"orange\"\n" +
                        "physical.shape = \"round\"\n" +
                        "site.\"google.com\" = true"));
    }

    @Test
    public void dottedKeysWhitespace() throws IOException {
        Assert.assertEquals(
                json("{\"fruit\": {\"name\": \"banana\", \"color\": \"yellow\", \"flavor\": \"banana\"}}"),
                toml("fruit.name = \"banana\"     # this is best practice\n" +
                        "fruit. color = \"yellow\"    # same as fruit.color\n" +
                        "fruit . flavor = \"banana\"   # same as fruit.flavor"));
    }

    @Test
    public void collision() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Duplicate key");
        toml("name = \"Tom\"\n" +
                "name = \"Pradyun\"");
    }

    @Test
    public void collisionQuoted() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Duplicate key");
        toml("spelling = \"favorite\"\n" +
                "\"spelling\" = \"favourite\"");
    }

    @Test
    public void keyMixed() throws IOException {
        Assert.assertEquals(
                json("{\"fruit\": {\"apple\": {\"smooth\": true}, \"orange\": 2}}"),
                toml("# This makes the key \"fruit\" into a table.\n" +
                        "fruit.apple.smooth = true\n" +
                        "\n" +
                        "# So then you can add to the table \"fruit\" like so:\n" +
                        "fruit.orange = 2")
        );
    }

    @Test
    public void collisionNested() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Path into existing non-object value of type NUMBER");
        toml("# This defines the value of fruit.apple to be an integer.\n" +
                "fruit.apple = 1\n" +
                "\n" +
                "# But then this treats fruit.apple like it's a table.\n" +
                "# You can't turn an integer into a table.\n" +
                "fruit.apple.smooth = true");
    }

    @Test
    public void outOfOrder() throws IOException {
        Assert.assertEquals(
                json("{\"apple\": {\"type\": \"fruit\",\"skin\": \"thin\", \"color\": \"red\"}, \"orange\": {\"type\": \"fruit\", \"skin\": \"thick\", \"color\": \"orange\"}}"),
                toml("apple.type = \"fruit\"\n" +
                        "orange.type = \"fruit\"\n" +
                        "\n" +
                        "apple.skin = \"thin\"\n" +
                        "orange.skin = \"thick\"\n" +
                        "\n" +
                        "apple.color = \"red\"\n" +
                        "orange.color = \"orange\"")
        );
    }

    @Test
    public void inOrder() throws IOException {
        Assert.assertEquals(
                json("{\"apple\": {\"type\": \"fruit\",\"skin\": \"thin\", \"color\": \"red\"}, \"orange\": {\"type\": \"fruit\", \"skin\": \"thick\", \"color\": \"orange\"}}"),
                toml("apple.type = \"fruit\"\n" +
                        "apple.skin = \"thin\"\n" +
                        "apple.color = \"red\"\n" +
                        "\n" +
                        "orange.type = \"fruit\"\n" +
                        "orange.skin = \"thick\"\n" +
                        "orange.color = \"orange\"")
        );
    }

    @Test
    public void numberDottedKey() throws IOException {
        Assert.assertEquals(
                json("{ \"3\": { \"14159\": \"pi\" } }"),
                // intellij doesn't like this one :)
                toml("3.14159 = \"pi\"")
        );
    }

    @Test
    public void stringBasic() throws IOException {
        Assert.assertEquals(
                json("{ \"str\": \"I'm a string. \\\"You can quote me\\\". Name\\tJosé\\nLocation\\tSF.\" }"),
                toml("str = \"I'm a string. \\\"You can quote me\\\". Name\\tJos\\u00E9\\nLocation\\tSF.\"")
        );
    }

    @Test
    public void multiLineBasic() throws IOException {
        Assert.assertEquals(
                json("{\"str1\": \"Roses are red\\nViolets are blue\"}"),
                toml("str1 = \"\"\"\n" +
                        "Roses are red\n" +
                        "Violets are blue\"\"\"")
        );
    }

    @Test
    public void multiLineEscapeNl() throws IOException {
        Assert.assertEquals(
                json("{\"str1\": \"The quick brown fox jumps over the lazy dog.\",\"str2\": \"The quick brown fox jumps over the lazy dog.\",\"str3\": \"The quick brown fox jumps over the lazy dog.\"}"),
                toml("str1 = \"The quick brown fox jumps over the lazy dog.\"\n" +
                        "\n" +
                        "str2 = \"\"\"\n" +
                        "The quick brown \\\n" +
                        "\n" +
                        "\n" +
                        "  fox jumps over \\\n" +
                        "    the lazy dog.\"\"\"\n" +
                        "\n" +
                        "str3 = \"\"\"\\\n" +
                        "       The quick brown \\\n" +
                        "       fox jumps over \\\n" +
                        "       the lazy dog.\\\n" +
                        "       \"\"\"")
        );
    }

    @Test
    public void escapedQuotes() throws IOException {
        Assert.assertEquals(
                json("{\"str4\": \"Here are two quotation marks: \\\"\\\". Simple enough.\", \"str5\": \"Here are three quotation marks: \\\"\\\"\\\".\", \"str6\": \"Here are fifteen quotation marks: \\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\".\", \"str7\": \"\\\"This,\\\" she said, \\\"is just a pointless statement.\\\"\"}"),
                toml("str4 = \"\"\"Here are two quotation marks: \"\". Simple enough.\"\"\"\n" +
                        "# str5 = \"\"\"Here are three quotation marks: \"\"\".\"\"\"  # INVALID\n" +
                        "str5 = \"\"\"Here are three quotation marks: \"\"\\\".\"\"\"\n" +
                        "str6 = \"\"\"Here are fifteen quotation marks: \"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\".\"\"\"\n" +
                        "\n" +
                        "# \"This,\" she said, \"is just a pointless statement.\"\n" +
                        "str7 = \"\"\"\"This,\" she said, \"is just a pointless statement.\"\"\"\"")
        );
    }

    @Test
    public void missingQuotesEscape() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("More data after value has already ended. Invalid value preceding this position?");
        toml("str5 = \"\"\"Here are three quotation marks: \"\"\".\"\"\"");
    }

    @Test
    public void literalStrings() throws IOException {
        Assert.assertEquals(
                json("{\"winpath\": \"C:\\\\Users\\\\nodejs\\\\templates\", \"winpath2\": \"\\\\\\\\ServerX\\\\admin$\\\\system32\\\\\", \"quoted\": \"Tom \\\"Dubs\\\" Preston-Werner\", \"regex\": \"<\\\\i\\\\c*\\\\s*>\"}"),
                toml("winpath  = 'C:\\Users\\nodejs\\templates'\n" +
                        "winpath2 = '\\\\ServerX\\admin$\\system32\\'\n" +
                        "quoted   = 'Tom \"Dubs\" Preston-Werner'\n" +
                        "regex    = '<\\i\\c*\\s*>'")
        );
    }

    @Test
    public void multiLineLiteral() throws IOException {
        Assert.assertEquals(
                json("{\"regex2\": \"I [dw]on't need \\\\d{2} apples\", \"lines\": \"The first newline is\\ntrimmed in raw strings.\\n   All other whitespace\\n   is preserved.\\n\"}"),
                toml("regex2 = '''I [dw]on't need \\d{2} apples'''\n" +
                        "lines  = '''\n" +
                        "The first newline is\n" +
                        "trimmed in raw strings.\n" +
                        "   All other whitespace\n" +
                        "   is preserved.\n" +
                        "'''")
        );
    }

    @Test
    public void multiLineLiteralQuotes() throws IOException {
        Assert.assertEquals(
                json("{\"quot15\": \"Here are fifteen quotation marks: \\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\\\"\", \"apos15\": \"Here are fifteen apostrophes: '''''''''''''''\", \"str\": \"'That,' she said, 'is still pointless.'\"}"),
                toml("quot15 = '''Here are fifteen quotation marks: \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"'''\n" +
                        "apos15 = \"Here are fifteen apostrophes: '''''''''''''''\"\n" +
                        "str = ''''That,' she said, 'is still pointless.''''")
        );
    }

    @Test
    public void integer() throws IOException {
        Assert.assertEquals(
                json("{\"int1\": 99, \"int2\": 42, \"int3\": 0, \"int4\": -17}"),
                toml("int1 = +99\n" +
                        "int2 = 42\n" +
                        "int3 = 0\n" +
                        "int4 = -17")
        );
    }

    @Test
    public void integerUnderscore() throws IOException {
        Assert.assertEquals(
                json("{\"int5\": 1000, \"int6\": 5349221, \"int7\": 5349221, \"int8\": 12345}"),
                toml("int5 = 1_000\n" +
                        "int6 = 5_349_221\n" +
                        "int7 = 53_49_221  # Indian number system grouping\n" +
                        "int8 = 1_2_3_4_5  # VALID but discouraged")
        );
    }

    @Test
    public void integerBase() throws IOException {
        Assert.assertEquals(
                json("{\"hex1\": 3735928559, \"hex2\": 3735928559, \"hex3\": 3735928559, \"oct1\": 342391, \"oct2\": 493, \"bin1\": 214}"),
                toml("# hexadecimal with prefix `0x`\n" +
                        "hex1 = 0xDEADBEEF\n" +
                        "hex2 = 0xdeadbeef\n" +
                        "hex3 = 0xdead_beef\n" +
                        "\n" +
                        "# octal with prefix `0o`\n" +
                        "oct1 = 0o01234567\n" +
                        "oct2 = 0o755 # useful for Unix file permissions\n" +
                        "\n" +
                        "# binary with prefix `0b`\n" +
                        "bin1 = 0b11010110")
        );
    }

    @Test
    public void floats() throws IOException {
        Assert.assertEquals(
                json("{\"flt1\": 1.0, \"flt2\": 3.1415, \"flt3\": -0.01, \"flt4\": 5.0e22, \"flt5\": 1e06, \"flt6\": -2e-2, \"flt7\": 6.626e-34}"),
                toml("# fractional\n" +
                        "flt1 = +1.0\n" +
                        "flt2 = 3.1415\n" +
                        "flt3 = -0.01\n" +
                        "\n" +
                        "# exponent\n" +
                        "flt4 = 5e+22\n" +
                        // intellij doesn't like this one either :)
                        "flt5 = 1e06\n" +
                        "flt6 = -2E-2\n" +
                        "\n" +
                        "# both\n" +
                        "flt7 = 6.626e-34")
        );
    }

    @Test
    public void invalidFloat1() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Unknown token");
        toml("invalid_float_1 = .7");
    }

    @Test
    public void invalidFloat2() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("More data after value has already ended. Invalid value preceding this position?");
        toml("invalid_float_2 = 7.");
    }

    @Test
    public void invalidFloat3() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("More data after value has already ended. Invalid value preceding this position?");
        toml("invalid_float_3 = 3.e+20");
    }

    @Test
    public void floatUnderscore() throws IOException {
        Assert.assertEquals(
                json("{\"flt8\": 224617.445991228}"),
                toml("flt8 = 224_617.445_991_228")
        );
    }

    @Test
    public void floatSpecial() throws IOException {
        Assert.assertEquals(
                json("{\"sf1\": Infinity, \"sf2\": Infinity, \"sf3\": -Infinity, \"sf4\": NaN, \"sf5\": NaN, \"sf6\": NaN}"),
                toml("# infinity\n" +
                        "sf1 = inf  # positive infinity\n" +
                        "sf2 = +inf # positive infinity\n" +
                        "sf3 = -inf # negative infinity\n" +
                        "\n" +
                        "# not a number\n" +
                        "sf4 = nan  # actual sNaN/qNaN encoding is implementation-specific\n" +
                        "sf5 = +nan # same as `nan`\n" +
                        "sf6 = -nan # valid, actual encoding is implementation-specific")
        );
    }

    @Test
    public void booleans() throws IOException {
        Assert.assertEquals(
                json("{\"bool1\": true, \"bool2\": false}"),
                toml("bool1 = true\n" +
                        "bool2 = false\n" +
                        "\n")
        );
    }

    @Test
    public void odt() throws IOException {
        Assert.assertEquals(
                json("{\"odt1\": \"1979-05-27T07:32:00Z\", \"odt2\": \"1979-05-27T00:32:00-07:00\", \"odt3\": \"1979-05-27T00:32:00.999999-07:00\", \"odt4\": \"1979-05-27T07:32:00Z\"}"),
                toml("odt1 = 1979-05-27T07:32:00Z\n" +
                        "odt2 = 1979-05-27T00:32:00-07:00\n" +
                        "odt3 = 1979-05-27T00:32:00.999999-07:00\n" +
                        "odt4 = 1979-05-27 07:32:00Z")
        );
    }

    @Test
    public void ldt() throws IOException {
        Assert.assertEquals(
                json("{\"ldt1\": \"1979-05-27T07:32:00\", \"ldt2\": \"1979-05-27T00:32:00.999999\"}"),
                toml("ldt1 = 1979-05-27T07:32:00\n" +
                        "ldt2 = 1979-05-27T00:32:00.999999")
        );
    }

    @Test
    public void ld() throws IOException {
        Assert.assertEquals(
                json("{\"ld1\": \"1979-05-27\"}"),
                toml("ld1 = 1979-05-27")
        );
    }

    @Test
    public void lt() throws IOException {
        Assert.assertEquals(
                json("{\"lt1\": \"07:32:00\", \"lt2\": \"00:32:00.999999\"}"),
                toml("lt1 = 07:32:00\n" +
                        "lt2 = 00:32:00.999999")
        );
    }

    @Test
    public void array() throws IOException {
        Assert.assertEquals(
                json("{\"integers\": [1,2,3], \"colors\": [\"red\",\"yellow\",\"green\"], \"nested_arrays_of_ints\": [ [ 1, 2 ], [3, 4, 5] ], \"nested_mixed_array\": [ [ 1, 2 ], [\"a\", \"b\", \"c\"] ], \"string_array\": [ \"all\", \"strings\", \"are the same\", \"type\" ],\n" +
                        "  \"numbers\": " +
                        "[ 0.1, 0.2, 0.5, 1, 2, 5 ], \"contributors\": [\"Foo Bar <foo@example.com>\", { \"name\": \"Baz Qux\",\n" +
                        "  \"email\": " +
                        "\"bazqux@example.com\",\n" +
                        "  \"url\": " +
                        "\"https://example.com/bazqux\" }]\n" +
                        "}"),
                toml("integers = [ 1, 2, 3 ]\n" +
                        "colors = [ \"red\", \"yellow\", \"green\" ]\n" +
                        "nested_arrays_of_ints = [ [ 1, 2 ], [3, 4, 5] ]\n" +
                        "nested_mixed_array = [ [ 1, 2 ], [\"a\", \"b\", \"c\"] ]\n" +
                        "string_array = [ \"all\", 'strings', \"\"\"are the same\"\"\", '''type''' ]\n" +
                        "\n" +
                        "# Mixed-type arrays are allowed\n" +
                        "numbers = [ 0.1, 0.2, 0.5, 1, 2, 5 ]\n" +
                        "contributors = [\n" +
                        "  \"Foo Bar <foo@example.com>\",\n" +
                        "  { name = \"Baz Qux\", email = \"bazqux@example.com\", url = \"https://example.com/bazqux\" }\n" +
                        "]")
        );
    }

    @Test
    public void arrayMultiLine() throws IOException {
        Assert.assertEquals(
                json("{\"integers2\": [1,2,3]," +
                        "\"integers3\": [1,2]" +
                        "}"),
                toml("integers2 = [\n" +
                        "  1, 2, 3\n" +
                        "]\n" +
                        "\n" +
                        "integers3 = [\n" +
                        "  1,\n" +
                        "  2, # this is ok\n" +
                        "]")
        );
    }

    @Test
    public void table() throws IOException {
        Assert.assertEquals(
                json("{\"table\": {}}"),
                toml("[table]")
        );
    }

    @Test
    public void table2() throws IOException {
        Assert.assertEquals(
                json("{\"table-1\": {\"key1\": \"some string\", \"key2\": 123}, \"table-2\": {\"key1\": \"another string\", \"key2\": 456}}"),
                toml("[table-1]\n" +
                        "key1 = \"some string\"\n" +
                        "key2 = 123\n" +
                        "\n" +
                        "[table-2]\n" +
                        "key1 = \"another string\"\n" +
                        "key2 = 456")
        );
    }

    @Test
    public void tableQuoted() throws IOException {
        Assert.assertEquals(
                json("{\"dog\": {\"tater.man\": {\"type\": {\"name\": \"pug\"}}}}"),
                toml("[dog.\"tater.man\"]\n" +
                        "type.name = \"pug\"")
        );
    }

    @Test
    public void tableWhitespace() throws IOException {
        Assert.assertEquals(
                json("{\"a\": {\"b\": {" +
                        "\"c\": {}" +
                        "}}, \"d\": {\"e\": {\"f\": {}}},\"g\": {\"h\": {\"i\": {}}},\"j\": {\"ʞ\": {\"l\": {}}}}"),
                toml("[a.b.c]            # this is best practice\n" +
                        "[ d.e.f ]          # same as [d.e.f]\n" +
                        "[ g .  h  . i ]    # same as [g.h.i]\n" +
                        "[ j . \"ʞ\" . 'l' ]  # same as [j.\"ʞ\".'l']")
        );
    }

    @Test
    public void order() throws IOException {
        Assert.assertEquals(
                json("{\"x\": {\"y\": {" +
                        "\"z\": {\"w\": {}}" +
                        "}}}"),
                toml("# [x] you\n" +
                        "# [x.y] don't\n" +
                        "# [x.y.z] need these\n" +
                        "[x.y.z.w] # for this to work\n" +
                        "\n" +
                        "[x] # defining a super-table afterward is ok")
        );
    }

    @Test
    public void duplicateTable() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Table redefined");
        toml("[fruit]\n" +
                "apple = \"red\"\n" +
                "\n" +
                "[fruit]\n" +
                "orange = \"orange\"");
    }

    @Test
    public void mixedTable() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Path into existing non-object value of type STRING");
        toml("[fruit]\n" +
                "apple = \"red\"\n" +
                "\n" +
                "[fruit.apple]\n" +
                "texture = \"smooth\"");
    }

    @Test
    public void tableOutOfOrder() throws IOException {
        Assert.assertEquals(
                json("{\"fruit\": {\"apple\": {}, \"orange\": {}}, \"animal\": {}}"),
                toml("[fruit.apple]\n" +
                        "[animal]\n" +
                        "[fruit.orange]")
        );
    }

    @Test
    public void tableInOrder() throws IOException {
        Assert.assertEquals(
                json("{\"fruit\": {\"apple\": {}, \"orange\": {}}, \"animal\": {}}"),
                toml("[fruit.apple]\n" +
                        "[animal]\n" +
                        "[fruit.orange]")
        );
    }

    @Test
    public void rootTable() throws IOException {
        Assert.assertEquals(
                json("{\"name\": \"Fido\", \"breed\": \"pug\", \"owner\": {\"name\": \"Regina Dogman\"}}"),
                toml("# Top-level table begins.\n" +
                        "name = \"Fido\"\n" +
                        "breed = \"pug\"\n" +
                        "\n" +
                        "# Top-level table ends.\n" +
                        "[owner]\n" +
                        "name = \"Regina Dogman\"")
        );
    }

    @Test
    public void dottedDefinesTable() throws IOException {
        Assert.assertEquals(
                json("{\"fruit\": {\"apple\": {\"color\": \"red\", " +
                        "\"taste\": {\"sweet\": true}" +
                        "}}}"),
                toml("fruit.apple.color = \"red\"\n" +
                        "# Defines a table named fruit\n" +
                        "# Defines a table named fruit.apple\n" +
                        "\n" +
                        "fruit.apple.taste.sweet = true\n" +
                        "# Defines a table named fruit.apple.taste\n" +
                        "# fruit and fruit.apple were already created")
        );
    }

    @Test
    public void dottedCollisionRoot() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Table redefined");
        toml("fruit.apple.color = \"red\"\n" +
                "# Defines a table named fruit\n" +
                "# Defines a table named fruit.apple\n" +
                "\n" +
                "[fruit]\n" +
                "foo" +
                " = \"bar\"");
    }

    @Test
    public void dottedCollisionNest() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Table redefined");
        toml("[fruit]\n" +
                "apple.color = \"red\"\n" +
                "apple.taste.sweet = true\n" +
                "\n" +
                "[fruit.apple]  # INVALID");
    }

    @Test
    public void dottedSubTable() throws IOException {
        Assert.assertEquals(
                json("{\"fruit\": {\"apple\": {\"color\": \"red\", " +
                        "\"taste\": {\"sweet\": true}, \"texture\": {\"smooth\": true}" +
                        "}}}"),
                toml("[fruit]\n" +
                        "apple.color = \"red\"\n" +
                        "apple.taste.sweet = true\n" +
                        "\n" +
                        "# [fruit.apple]  # INVALID\n" +
                        "# [fruit.apple.taste]  # INVALID\n" +
                        "\n" +
                        "[fruit.apple.texture]  # you can add sub-tables\n" +
                        "smooth = true")
        );
    }

    @Test
    public void inlineTable() throws IOException {
        Assert.assertEquals(
                json("{\"name\": {\"first\": \"Tom\", \"last\": \"Preston-Werner\"}, \"point\": {\"x\": 1, \"y\": 2}, \"animal\": {\"type\": {\"name\": \"pug\"}}}"),
                toml("name = { first = \"Tom\", last = \"Preston-Werner\" }\n" +
                        "point = { x = 1, y = 2 }\n" +
                        "animal = { type.name = \"pug\" }")
        );
    }

    @Test
    public void inlineTableSelfContained() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Object already closed");
        toml("[product]\n" +
                "type = { name = \"Nail\" }\n" +
                "type.edible = false  # INVALID");
    }

    @Test
    public void inlineTableSelfContained2() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Duplicate key");
        toml("[product]\n" +
                "type.name = \"Nail\"\n" +
                "type = { edible = false }  # INVALID");
    }

    @Test
    public void arrayTable() throws IOException {
        Assert.assertEquals(
                json("{\n" +
                        "  \"products\": [\n" +
                        "    { \"name\": \"Hammer\", \"sku\": 738594937 },\n" +
                        "    { },\n" +
                        "    { \"name\": \"Nail\", \"sku\": 284758393, \"color\": \"gray\" }\n" +
                        "  ]\n" +
                        "}"),
                toml("[[products]]\n" +
                        "name = \"Hammer\"\n" +
                        "sku = 738594937\n" +
                        "\n" +
                        "[[products]]  # empty table within the array\n" +
                        "\n" +
                        "[[products]]\n" +
                        "name = \"Nail\"\n" +
                        "sku = 284758393\n" +
                        "\n" +
                        "color = \"gray\"")
        );
    }

    @Test
    public void arrayTableDotted() throws IOException {
        Assert.assertEquals(
                json("{\n" +
                        "  \"fruits\": [\n" +
                        "    {\n" +
                        "      \"name\": \"apple\",\n" +
                        "      \"physical\": {\n" +
                        "        \"color\": \"red\",\n" +
                        "        \"shape\": \"round\"\n" +
                        "      },\n" +
                        "      \"varieties\": [\n" +
                        "        { \"name\": \"red delicious\" },\n" +
                        "        { \"name\": \"granny smith\" }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"name\": \"banana\",\n" +
                        "      \"varieties\": [\n" +
                        "        { \"name\": \"plantain\" }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"),
                toml("[[fruits]]\n" +
                        "name = \"apple\"\n" +
                        "\n" +
                        "[fruits.physical]  # subtable\n" +
                        "color = \"red\"\n" +
                        "shape = \"round\"\n" +
                        "\n" +
                        "[[fruits.varieties]]  # nested array of tables\n" +
                        "name = \"red delicious\"\n" +
                        "\n" +
                        "[[fruits.varieties]]\n" +
                        "name = \"granny smith\"\n" +
                        "\n" +
                        "\n" +
                        "[[fruits]]\n" +
                        "name = \"banana\"\n" +
                        "\n" +
                        "[[fruits.varieties]]\n" +
                        "name = \"plantain\"")
        );
    }

    @Test
    public void arrayTableStillMissing() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Path into existing non-array value of type OBJECT");
        toml("# INVALID TOML DOC\n" +
                "[fruit.physical]  # subtable, but to which parent element should it belong?\n" +
                "color = \"red\"\n" +
                "shape = \"round\"\n" +
                "\n" +
                "[[fruit]]  # parser must throw an error upon discovering that \"fruit\" is\n" +
                "           # an array rather than a table\n" +
                "name = \"apple\"");
    }

    @Test
    public void arrayInlineAndTable() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Array already finished");
        toml("# INVALID TOML DOC\n" +
                "fruits = []\n" +
                "\n" +
                "[[fruits]] # Not allowed");
    }

    @Test
    public void arrayCollision1() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Path into existing non-object value of type ARRAY");
        toml("# INVALID TOML DOC\n" +
                "[[fruits]]\n" +
                "name = \"apple\"\n" +
                "\n" +
                "[[fruits.varieties]]\n" +
                "name = \"red delicious\"\n" +
                "\n" +
                "# INVALID: This table conflicts with the previous array of tables\n" +
                "[fruits.varieties]\n" +
                "name = \"granny smith\"");
    }

    @Test
    public void arrayCollision2() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Path into existing non-array value of type OBJECT");
        toml("# INVALID TOML DOC\n" +
                "[[fruits]]\n" +
                "name = \"apple\"\n" +
                "\n" +
                "[fruits.physical]\n" +
                "color = \"red\"\n" +
                "shape = \"round\"\n" +
                "\n" +
                "# INVALID: This array of tables conflicts with the previous table\n" +
                "[[fruits.physical]]\n" +
                "color = \"green\"");
    }

    @Test
    public void points() throws IOException {
        Assert.assertEquals(
                json("{\"points\":  [ { \"x\":1, \"y\":2, \"z\":3 },\n" +
                        "  { \"x\":7, \"y\":8, \"z\":9 },\n" +
                        "  { \"x\":2, \"y\":4, \"z\": 8}]}"),
                toml("points = [ { x = 1, y = 2, z = 3 },\n" +
                        "           { x = 7, y = 8, z = 9 },\n" +
                        "           { x = 2, y = 4, z = 8 } ]")
        );
    }

    // from the manual END
    // following are our tests :)

    @Test
    public void inlineTableTrailingComma() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Trailing comma not permitted for inline tables");
        toml("foo = {bar = 'baz',}");
    }

    @Test
    public void inlineTableEmpty() throws IOException {
        Assert.assertEquals(
                json("{\"foo\": {}}"),
                toml("foo = {}")
        );
    }

    @Test
    public void inlineTableNl() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Newline not permitted here");
        toml("foo = {bar = 'baz',\n" +
                "a = 'b'}");
    }

    @Test
    public void extendedUnicodeEscape() throws IOException {
        // 🆒
        Assert.assertEquals(
                json("{\"foo\": \"\\uD83C\\uDD92\"}"),
                toml("foo = \"\\U0001f192\"")
        );
    }

    @Test
    public void extendedUnicodeEscapeInvalid() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Invalid code point ffffffff");
        toml("foo = \"\\Uffffffff\"");
    }

    @Test
    public void doubles() throws IOException {
        // this is the same test as above, except with doubles instead of BigDecimals
        Assert.assertEquals(
                JsonMapper.builder().build()
                        .readTree("{\"flt1\": 1.0, \"flt2\": 3.1415, \"flt3\": -0.01, \"flt4\": 5.0e22, \"flt5\": 1e06, \"flt6\": -2e-2, \"flt7\": 6.626e-34}"),
                toml(new ParserOptions(false, false), "# fractional\n" +
                        "flt1 = +1.0\n" +
                        "flt2 = 3.1415\n" +
                        "flt3 = -0.01\n" +
                        "\n" +
                        "# exponent\n" +
                        "flt4 = 5e+22\n" +
                        "flt5 = 1e06\n" +
                        "flt6 = -2E-2\n" +
                        "\n" +
                        "# both\n" +
                        "flt7 = 6.626e-34")
        );
    }

    @Test
    public void intTypes() throws IOException {
        Assert.assertEquals(
                JsonNodeFactory.instance.objectNode()
                        .put("int1", 99)
                        .put("int2", 4242424242L)
                        .put("int3", new BigInteger("171717171717171717171717")),
                toml(new ParserOptions(false, false), "int1 = +99\n" +
                        "int2 = 4242424242\n" +
                        "int3 = 171717171717171717171717")
        );
    }

    @Test
    public void longBase() throws IOException {
        Assert.assertEquals(
                JsonNodeFactory.instance.objectNode()
                        .put("hex1", 0xDDEADBEEFL)
                        .put("hex2", 0xddeadbeefL)
                        .put("hex3", 0xddead_beefL)
                        .put("oct1", 01234567777777L)
                        .put("bin1", 0b11010110101010101010101010101010101010L),
                toml(new ParserOptions(false, false), "hex1 = 0xdDEADBEEF\n" +
                        "hex2 = 0xddeadbeef\n" +
                        "hex3 = 0xddead_beef\n" +
                        "oct1 = 0o1234567777777\n" +
                        "bin1 = 0b11010110101010101010101010101010101010")
        );
    }

    @Test
    public void bigintBase() throws IOException {
        Assert.assertEquals(
                JsonNodeFactory.instance.objectNode()
                        .put("hex1", new BigInteger("DDEADBEEFDDEADBEEF", 16))
                        .put("hex2", new BigInteger("DDEADBEEFDDEADBEEF", 16))
                        .put("hex3", new BigInteger("DDEADBEEFDDEADBEEF", 16))
                        .put("oct1", new BigInteger("12345677777771234567777777", 8))
                        .put("bin1", new BigInteger("1101011010101010101010101010101010101011010110101010101010101010101010101010", 2)),
                toml(new ParserOptions(false, false), "hex1 = 0xDDEADBEEFDDEADBEEF\n" +
                        "hex2 = 0xddeadbeefddeadbeef\n" +
                        "hex3 = 0xddead_beefddead_beef\n" +
                        "oct1 = 0o12345677777771234567777777\n" +
                        "bin1 = 0b1101011010101010101010101010101010101011010110101010101010101010101010101010")
        );
    }

    @Test
    public void javaTimeDeser() throws IOException {
        // this is the same test as above, except with explicit java.time deserialization
        ParserOptions options = new ParserOptions(true, false);

        Assert.assertEquals(
                JsonNodeFactory.instance.objectNode()
                        .set("odt1", JsonNodeFactory.instance.pojoNode(OffsetDateTime.parse("1979-05-27T07:32:00Z")))
                        .set("odt2", JsonNodeFactory.instance.pojoNode(OffsetDateTime.parse("1979-05-27T00:32:00-07:00")))
                        .set("odt3", JsonNodeFactory.instance.pojoNode(OffsetDateTime.parse("1979-05-27T00:32:00.999999-07:00")))
                        .set("odt4", JsonNodeFactory.instance.pojoNode(OffsetDateTime.parse("1979-05-27T07:32:00Z"))),
                toml(options,
                        "odt1 = 1979-05-27T07:32:00Z\n" +
                                "odt2 = 1979-05-27T00:32:00-07:00\n" +
                                "odt3 = 1979-05-27T00:32:00.999999-07:00\n" +
                                "odt4 = 1979-05-27 07:32:00Z")
        );
        Assert.assertEquals(
                JsonNodeFactory.instance.objectNode()
                        .set("ldt1", JsonNodeFactory.instance.pojoNode(LocalDateTime.parse("1979-05-27T07:32:00")))
                        .set("ldt2", JsonNodeFactory.instance.pojoNode(LocalDateTime.parse("1979-05-27T00:32:00.999999"))),
                toml(options,
                        "ldt1 = 1979-05-27T07:32:00\n" +
                                "ldt2 = 1979-05-27T00:32:00.999999")
        );
        Assert.assertEquals(
                JsonNodeFactory.instance.objectNode()
                        .set("ld1", JsonNodeFactory.instance.pojoNode(LocalDate.parse("1979-05-27"))),
                toml(options, "ld1 = 1979-05-27")
        );
        Assert.assertEquals(
                JsonNodeFactory.instance.objectNode()
                        .set("lt1", JsonNodeFactory.instance.pojoNode(LocalTime.parse("07:32:00")))
                        .set("lt2", JsonNodeFactory.instance.pojoNode(LocalTime.parse("00:32:00.999999"))),
                toml(options,
                        "lt1 = 07:32:00\n" +
                                "lt2 = 00:32:00.999999")
        );
    }

    @Test
    public void controlCharInComment() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Illegal control character");
        // https://github.com/toml-lang/toml/pull/812
        toml("a = \"0x7f\" # \u007F");
    }

    @Test
    public void controlCharInLiteralString() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Illegal control character");
        // Not explicit in spec, but only in the abnf
        toml("a = '\u007F'");
    }

    @Test
    public void zeroPrefixedInt() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Zero-prefixed ints are not valid");
        toml("foo = 01");
    }

    @Test
    public void signedBase() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("More data after value has already ended. Invalid value preceding this position?");
        toml("foo = +0b1");
    }

    @Test
    public void illegalComment() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Comment not permitted here");
        toml("foo = # bar");
    }

    @Test
    public void unknownEscape() throws IOException {
        expectedException.expect(JacksonTomlParseException.class);
        expectedException.expectMessage("Unknown escape sequence");
        toml("foo = \"\\k\"");
    }
}