package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

class Parser {
    private static final JsonNodeFactory factory = JsonNodeFactory.instance;

    private final Lexer lexer;

    private TomlToken next;

    private Parser(Reader reader) throws IOException {
        this.lexer = new Lexer(reader);
        this.next = lexer.yylex();
    }

    public static ObjectNode parse(Reader reader) throws IOException {
        return new Parser(reader).parse();
    }

    private TomlToken peek() throws IOException {
        TomlToken here = this.next;
        if (here == null) throw new EOFException();
        return here;
    }

    /**
     * Note: Polling also lexes the next token, so methods like {@link Lexer#yytext()} will not work afterwards
     */
    private TomlToken poll() throws IOException {
        TomlToken here = peek();
        next = lexer.yylex();
        return here;
    }

    private void pollExpected(TomlToken expected) throws IOException {
        TomlToken actual = poll();
        if (actual != expected) {
            throw unexpectedToken(actual, expected.toString());
        }
    }

    private JacksonException parseException(String msg) {
        return new JacksonException(msg) {
            @Override
            public Object processor() {
                return Parser.this; // TODO
            }
        };
    }

    private JacksonException unexpectedToken(TomlToken actual, String expected) {
        return parseException(
                "Unexpected token at " + lexer.positionString() + ": Got " + actual + ", expected " + expected);
    }

    public ObjectNode parse() throws IOException {
        ObjectNode root = factory.objectNode();
        ObjectNode currentTable = root;
        while (next != null) {
            TomlToken token = peek();
            if (token == TomlToken.UNQUOTED_KEY || token == TomlToken.STRING) {
                parseKeyVal(currentTable);
            } else if (token == TomlToken.STD_TABLE_OPEN) {
                pollExpected(TomlToken.STD_TABLE_OPEN);
                FieldRef fieldRef = parseAndEnterKey(root);
                currentTable = getOrCreateObject(fieldRef.object, fieldRef.key);
                pollExpected(TomlToken.STD_TABLE_CLOSE);
            } else if (token == TomlToken.ARRAY_TABLE_OPEN) {
                pollExpected(TomlToken.ARRAY_TABLE_OPEN);
                FieldRef fieldRef = parseAndEnterKey(root);
                ArrayNode array = getOrCreateArray(fieldRef.object, fieldRef.key);
                currentTable = array.addObject();
                pollExpected(TomlToken.ARRAY_TABLE_CLOSE);
            } else {
                throw unexpectedToken(token, "key or table");
            }
        }
        assert lexer.yyatEOF();
        if (lexer.yystate() != Lexer.EXPECT_KEY) {
            throw parseException("EOF in wrong state");
        }
        return root;
    }

    private FieldRef parseAndEnterKey(ObjectNode outer) throws IOException {
        ObjectNode node = outer;
        while (true) {
            TomlToken partToken = peek();
            String part;
            if (partToken == TomlToken.STRING) {
                part = lexer.stringBuilder.toString();
            } else if (partToken == TomlToken.UNQUOTED_KEY) {
                part = lexer.yytext();
            } else {
                throw unexpectedToken(partToken, "quoted or unquoted key");
            }
            pollExpected(partToken);
            if (peek() != TomlToken.DOT_SEP) {
                return new FieldRef(node, part);
            }
            node = getOrCreateObject(node, part);
            pollExpected(TomlToken.DOT_SEP);
        }
    }

    private JsonNode parseValue() throws IOException {
        TomlToken firstToken = peek();
        switch (firstToken) {
            case STRING:
                String text = lexer.stringBuilder.toString();
                pollExpected(TomlToken.STRING);
                return factory.textNode(text);
            case TRUE:
                pollExpected(TomlToken.TRUE);
                return factory.booleanNode(true);
            case FALSE:
                pollExpected(TomlToken.FALSE);
                return factory.booleanNode(false);
            case OFFSET_DATE_TIME:
            case LOCAL_DATE_TIME:
            case LOCAL_DATE:
            case LOCAL_TIME:
                return parseDateTime();
            case FLOAT:
                return parseFloat();
            case INTEGER:
                return parseInt();
            case ARRAY_OPEN:
                return parseArray();
            case INLINE_TABLE_OPEN:
                return parseInlineTable();
            default:
                throw unexpectedToken(firstToken, "value");
        }
    }

    private JsonNode parseDateTime() throws IOException {
        String text = lexer.yytext();
        TomlToken token = poll();
        Temporal value;
        if (token == TomlToken.LOCAL_DATE) {
            value = LocalDate.parse(text);
        } else if (token == TomlToken.LOCAL_TIME) {
            value = LocalTime.parse(text);
        } else {
            // the time-delim index can be [Tt ]. java.time supports only [Tt]
            if (text.charAt(10) == ' ') {
                text = text.substring(0, 10) + 'T' + text.substring(11);
            }
            if (token == TomlToken.LOCAL_DATE_TIME) {
                value = LocalDateTime.parse(text);
            } else if (token == TomlToken.OFFSET_DATE_TIME) {
                value = OffsetDateTime.parse(text);
            } else {
                throw new AssertionError();
            }
        }
        // TODO
        return factory.pojoNode(value);
    }

    private JsonNode parseInt() throws IOException {
        String text = lexer.yytext().replace("_", "");
        BigInteger v;
        if (text.startsWith("0x") || text.startsWith("0X")) {
            v = new BigInteger(text.substring(2), 16);
        } else if (text.startsWith("0o") || text.startsWith("0O")) {
            v = new BigInteger(text.substring(2), 8);
        } else if (text.startsWith("0b") || text.startsWith("0B")) {
            v = new BigInteger(text.substring(2), 2);
        } else {
            v = new BigInteger(text);
        }
        pollExpected(TomlToken.INTEGER);
        return factory.numberNode(v);
    }

    private JsonNode parseFloat() throws IOException {
        String text = lexer.yytext().replace("_", "");
        pollExpected(TomlToken.FLOAT);
        if (text.endsWith("nan")) {
            return factory.numberNode(Double.NaN);
        } else if (text.endsWith("inf")) {
            return factory.numberNode(text.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
        } else {
            return factory.numberNode(new BigDecimal(text));
        }
    }

    private ObjectNode parseInlineTable() throws IOException {
        // inline-table = inline-table-open [ inline-table-keyvals ] inline-table-close
        // inline-table-keyvals = keyval [ inline-table-sep inline-table-keyvals ]
        pollExpected(TomlToken.INLINE_TABLE_OPEN);
        ObjectNode node = factory.objectNode();
        while (true) {
            TomlToken token = peek();
            if (token == TomlToken.INLINE_TABLE_CLOSE) {
                pollExpected(TomlToken.INLINE_TABLE_CLOSE);
                return node;
            }
            parseKeyVal(node);
            TomlToken sepToken = peek();
            if (sepToken == TomlToken.INLINE_TABLE_CLOSE) {
                pollExpected(TomlToken.INLINE_TABLE_CLOSE);
                return node;
            } else if (sepToken == TomlToken.ARRAY_SEP) {
                lexer.yybegin(Lexer.EXPECT_KEY);
                pollExpected(TomlToken.ARRAY_SEP);
            } else {
                throw unexpectedToken(sepToken, "comma or table end");
            }
        }
    }

    private ArrayNode parseArray() throws IOException {
        // array = array-open [ array-values ] ws-comment-newline array-close
        // array-values =  ws-comment-newline val ws-comment-newline array-sep array-values
        // array-values =/ ws-comment-newline val ws-comment-newline [ array-sep ]
        pollExpected(TomlToken.ARRAY_OPEN);
        ArrayNode node = factory.arrayNode();
        while (true) {
            TomlToken token = peek();
            if (token == TomlToken.ARRAY_CLOSE) {
                pollExpected(TomlToken.ARRAY_CLOSE);
                return node;
            }
            if (token == TomlToken.ARRAY_WS_COMMENT_NEWLINE) {
                // whitespace permitted here
                pollExpected(TomlToken.ARRAY_WS_COMMENT_NEWLINE);
                continue;
            }
            JsonNode value = parseValue();
            node.add(value);
            // here, we're just after a value, and thus in the EXPECT_KEY state. In that state we don't need to worry
            // about whitespace, and the closing bracket is always a STD_TABLE_CLOSE.
            TomlToken sepToken = peek();
            if (sepToken == TomlToken.STD_TABLE_CLOSE) {
                pollExpected(TomlToken.STD_TABLE_CLOSE);
                return node;
            } else if (sepToken == TomlToken.ARRAY_SEP) {
                lexer.yybegin(Lexer.EXPECT_VALUE);
                pollExpected(TomlToken.ARRAY_SEP);
            } else {
                throw unexpectedToken(sepToken, "comma or array end");
            }
        }
    }

    private void parseKeyVal(ObjectNode target) throws IOException {
        // keyval = key keyval-sep val
        FieldRef fieldRef = parseAndEnterKey(target);
        pollExpected(TomlToken.KEY_VAL_SEP);
        JsonNode value = parseValue();
        if (fieldRef.object.has(fieldRef.key)) {
            throw parseException("Duplicate key");
        }
        fieldRef.object.set(fieldRef.key, value);
    }

    private ObjectNode getOrCreateObject(ObjectNode node, String field) throws IOException {
        JsonNode existing = node.get(field);
        if (existing == null) {
            return node.putObject(field);
        } else if (existing.isObject()) {
            return (ObjectNode) existing;
        } else {
            throw parseException("Path into existing non-object value at " + lexer.positionString() + ": " + node.getNodeType());
        }
    }

    private ArrayNode getOrCreateArray(ObjectNode node, String field) throws IOException {
        JsonNode existing = node.get(field);
        if (existing == null) {
            return node.putArray(field);
        } else if (existing.isArray()) {
            return (ArrayNode) existing;
        } else {
            throw parseException("Path into existing non-array value at " + lexer.positionString() + ": " + node.getNodeType());
        }
    }

    private static class FieldRef {
        final ObjectNode object;
        final String key;

        FieldRef(ObjectNode object, String key) {
            this.object = object;
            this.key = key;
        }
    }
}
