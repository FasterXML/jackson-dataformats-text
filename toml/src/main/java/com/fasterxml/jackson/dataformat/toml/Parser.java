package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    private static final JsonNodeFactory factory = new JsonNodeFactoryImpl();

    private final JacksonTomlParseException.ErrorContext errorContext;
    private final ParserOptions options;
    private final Lexer lexer;

    private TomlToken next;

    private Parser(JacksonTomlParseException.ErrorContext errorContext, ParserOptions options, Reader reader) throws IOException {
        this.errorContext = errorContext;
        this.options = options;
        this.lexer = new Lexer(reader, errorContext);
        this.next = lexer.yylex();
    }

    public static ObjectNode parse(JacksonTomlParseException.ErrorContext errorContext, ParserOptions options, Reader reader) throws IOException {
        return new Parser(errorContext, options, reader).parse();
    }

    private TomlToken peek() {
        TomlToken here = this.next;
        if (here == null) throw errorContext.atPosition(lexer).generic("Premature end of file");
        return here;
    }

    /**
     * Note: Polling also lexes the next token, so methods like {@link Lexer#yytext()} will not work afterwards
     */
    private TomlToken poll(int nextState) throws IOException {
        TomlToken here = peek();
        lexer.yybegin(nextState);
        next = lexer.yylex();
        return here;
    }

    private void pollExpected(TomlToken expected, int nextState) throws IOException {
        TomlToken actual = poll(nextState);
        if (actual != expected) {
            throw errorContext.atPosition(lexer).unexpectedToken(actual, expected.toString());
        }
    }

    public ObjectNode parse() throws IOException {
        TomlObjectNode root = (TomlObjectNode) factory.objectNode();
        TomlObjectNode currentTable = root;
        while (next != null) {
            TomlToken token = peek();
            if (token == TomlToken.UNQUOTED_KEY || token == TomlToken.STRING) {
                parseKeyVal(currentTable, Lexer.EXPECT_EOL);
            } else if (token == TomlToken.STD_TABLE_OPEN) {
                pollExpected(TomlToken.STD_TABLE_OPEN, Lexer.EXPECT_INLINE_KEY);
                FieldRef fieldRef = parseAndEnterKey(root, true);
                currentTable = getOrCreateObject(fieldRef.object, fieldRef.key);
                if (currentTable.defined) {
                    throw errorContext.atPosition(lexer).generic("Table redefined");
                }
                currentTable.defined = true;
                pollExpected(TomlToken.STD_TABLE_CLOSE, Lexer.EXPECT_EOL);
            } else if (token == TomlToken.ARRAY_TABLE_OPEN) {
                pollExpected(TomlToken.ARRAY_TABLE_OPEN, Lexer.EXPECT_INLINE_KEY);
                FieldRef fieldRef = parseAndEnterKey(root, true);
                TomlArrayNode array = getOrCreateArray(fieldRef.object, fieldRef.key);
                if (array.closed) {
                    throw errorContext.atPosition(lexer).generic("Array already finished");
                }
                currentTable = (TomlObjectNode) array.addObject();
                pollExpected(TomlToken.ARRAY_TABLE_CLOSE, Lexer.EXPECT_EOL);
            } else {
                throw errorContext.atPosition(lexer).unexpectedToken(token, "key or table");
            }
        }
        assert lexer.yyatEOF();
        int eofState = lexer.yystate();
        if (eofState != Lexer.EXPECT_EXPRESSION && eofState != Lexer.EXPECT_EOL) {
            throw errorContext.atPosition(lexer).generic("EOF in wrong state");
        }
        return root;
    }

    private FieldRef parseAndEnterKey(
            TomlObjectNode outer,
            boolean forTable
    ) throws IOException {
        TomlObjectNode node = outer;
        while (true) {
            if (node.closed) {
                throw errorContext.atPosition(lexer).generic("Object already closed");
            }
            if (!forTable) {
                /* "Dotted keys create and define a table for each key part before the last one, provided that such
                 * tables were not previously created." */
                node.defined = true;
            }

            TomlToken partToken = peek();
            String part;
            if (partToken == TomlToken.STRING) {
                part = lexer.stringBuilder.toString();
            } else if (partToken == TomlToken.UNQUOTED_KEY) {
                part = lexer.yytext();
            } else {
                throw errorContext.atPosition(lexer).unexpectedToken(partToken, "quoted or unquoted key");
            }
            pollExpected(partToken, Lexer.EXPECT_INLINE_KEY);
            if (peek() != TomlToken.DOT_SEP) {
                return new FieldRef(node, part);
            }
            pollExpected(TomlToken.DOT_SEP, Lexer.EXPECT_INLINE_KEY);

            JsonNode existing = node.get(part);
            if (existing == null) {
                node = (TomlObjectNode) node.putObject(part);
            } else if (existing.isObject()) {
                node = (TomlObjectNode) existing;
            } else if (existing.isArray()) {
                /* "Any reference to an array of tables points to the most recently defined table element of the array.
                 * This allows you to define sub-tables, and even sub-arrays of tables, inside the most recent table."
                 *
                 * I interpret this somewhat broadly: I accept such references even if there were unrelated tables
                 * in between, and I accept them for simple dotted keys as well (not just for tables). These cases don't
                 * seem to be covered by the specification.
                 */
                TomlArrayNode array = (TomlArrayNode) existing;
                if (array.closed) {
                    throw errorContext.atPosition(lexer).generic("Array already closed");
                }
                // Only arrays declared by array tables are not closed, and those are always arrays of objects.
                node = (TomlObjectNode) array.get(array.size() - 1);
            } else {
                throw errorContext.atPosition(lexer).generic("Path into existing non-object value of type " + node.getNodeType());
            }
        }
    }

    private JsonNode parseValue(int nextState) throws IOException {
        TomlToken firstToken = peek();
        switch (firstToken) {
            case STRING:
                String text = lexer.stringBuilder.toString();
                pollExpected(TomlToken.STRING, nextState);
                return factory.textNode(text);
            case TRUE:
                pollExpected(TomlToken.TRUE, nextState);
                return factory.booleanNode(true);
            case FALSE:
                pollExpected(TomlToken.FALSE, nextState);
                return factory.booleanNode(false);
            case OFFSET_DATE_TIME:
            case LOCAL_DATE_TIME:
            case LOCAL_DATE:
            case LOCAL_TIME:
                return parseDateTime(nextState);
            case FLOAT:
                return parseFloat(nextState);
            case INTEGER:
                return parseInt(nextState);
            case ARRAY_OPEN:
                return parseArray(nextState);
            case INLINE_TABLE_OPEN:
                return parseInlineTable(nextState);
            default:
                throw errorContext.atPosition(lexer).unexpectedToken(firstToken, "value");
        }
    }

    private JsonNode parseDateTime(int nextState) throws IOException {
        String text = lexer.yytext();
        TomlToken token = poll(nextState);
        // the time-delim index can be [Tt ]. java.time supports only [Tt]
        if ((token == TomlToken.LOCAL_DATE_TIME || token == TomlToken.OFFSET_DATE_TIME) && text.charAt(10) == ' ') {
            text = text.substring(0, 10) + 'T' + text.substring(11);
        }

        if (options.parseTemporalAsJavaTime) {
            Temporal value;
            if (token == TomlToken.LOCAL_DATE) {
                value = LocalDate.parse(text);
            } else if (token == TomlToken.LOCAL_TIME) {
                value = LocalTime.parse(text);
            } else {
                if (token == TomlToken.LOCAL_DATE_TIME) {
                    value = LocalDateTime.parse(text);
                } else if (token == TomlToken.OFFSET_DATE_TIME) {
                    value = OffsetDateTime.parse(text);
                } else {
                    throw new AssertionError();
                }
            }
            // todo: is pojoNode right here, or can we use rawValueNode?
            return factory.pojoNode(value);
        } else {
            return factory.textNode(text);
        }
    }

    private JsonNode parseInt(int nextState) throws IOException {
        String text = lexer.yytext().replace("_", "");
        pollExpected(TomlToken.INTEGER, nextState);
        if (options.bigInteger) {
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
            return factory.numberNode(v);
        } else {
            // "Arbitrary 64-bit signed integers (from −2^63 to 2^63−1) should be accepted and handled losslessly."
            long v;
            try {
                if (text.startsWith("0x") || text.startsWith("0X")) {
                    v = Long.parseLong(text.substring(2), 16);
                } else if (text.startsWith("0o") || text.startsWith("0O")) {
                    v = Long.parseLong(text.substring(2), 8);
                } else if (text.startsWith("0b") || text.startsWith("0B")) {
                    v = Long.parseLong(text.substring(2), 2);
                } else {
                    v = Long.parseLong(text);
                }
            } catch (NumberFormatException e) {
                // *should* only happen for out of bounds, any other errors *should* be caught by the lexer
                throw errorContext.atPosition(lexer).outOfBounds(e);
            }
            // todo: should we use smaller int types where possible?
            return factory.numberNode(v);
        }
    }

    private JsonNode parseFloat(int nextState) throws IOException {
        String text = lexer.yytext().replace("_", "");
        pollExpected(TomlToken.FLOAT, nextState);
        if (text.endsWith("nan")) {
            return factory.numberNode(Double.NaN);
        } else if (text.endsWith("inf")) {
            return factory.numberNode(text.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
        } else {
            if (options.bigDecimal) {
                return factory.numberNode(new BigDecimal(text));
            } else {
                return factory.numberNode(Double.parseDouble(text));
            }
        }
    }

    private ObjectNode parseInlineTable(int nextState) throws IOException {
        // inline-table = inline-table-open [ inline-table-keyvals ] inline-table-close
        // inline-table-keyvals = keyval [ inline-table-sep inline-table-keyvals ]
        pollExpected(TomlToken.INLINE_TABLE_OPEN, Lexer.EXPECT_INLINE_KEY);
        TomlObjectNode node = (TomlObjectNode) factory.objectNode();
        while (true) {
            TomlToken token = peek();
            if (token == TomlToken.INLINE_TABLE_CLOSE) {
                if (node.isEmpty()) {
                    break;
                } else {
                    // "A terminating comma (also called trailing comma) is not permitted after the last key/value pair
                    // in an inline table."
                    throw errorContext.atPosition(lexer).generic("Trailing comma not permitted for inline tables");
                }
            }
            parseKeyVal(node, Lexer.EXPECT_TABLE_SEP);
            TomlToken sepToken = peek();
            if (sepToken == TomlToken.INLINE_TABLE_CLOSE) {
                break;
            } else if (sepToken == TomlToken.COMMA) {
                pollExpected(TomlToken.COMMA, Lexer.EXPECT_INLINE_KEY);
            } else {
                throw errorContext.atPosition(lexer).unexpectedToken(sepToken, "comma or table end");
            }
        }
        pollExpected(TomlToken.INLINE_TABLE_CLOSE, nextState);
        node.closed = true;
        node.defined = true;
        return node;
    }

    private ArrayNode parseArray(int nextState) throws IOException {
        // array = array-open [ array-values ] ws-comment-newline array-close
        // array-values =  ws-comment-newline val ws-comment-newline array-sep array-values
        // array-values =/ ws-comment-newline val ws-comment-newline [ array-sep ]
        pollExpected(TomlToken.ARRAY_OPEN, Lexer.EXPECT_VALUE);
        TomlArrayNode node = (TomlArrayNode) factory.arrayNode();
        while (true) {
            TomlToken token = peek();
            if (token == TomlToken.ARRAY_CLOSE) {
                break;
            }
            JsonNode value = parseValue(Lexer.EXPECT_ARRAY_SEP);
            node.add(value);
            TomlToken sepToken = peek();
            if (sepToken == TomlToken.ARRAY_CLOSE) {
                break;
            } else if (sepToken == TomlToken.COMMA) {
                pollExpected(TomlToken.COMMA, Lexer.EXPECT_VALUE);
            } else {
                throw errorContext.atPosition(lexer).unexpectedToken(sepToken, "comma or array end");
            }
        }
        pollExpected(TomlToken.ARRAY_CLOSE, nextState);
        node.closed = true;
        return node;
    }

    private void parseKeyVal(TomlObjectNode target, int nextState) throws IOException {
        // keyval = key keyval-sep val
        FieldRef fieldRef = parseAndEnterKey(target, false);
        pollExpected(TomlToken.KEY_VAL_SEP, Lexer.EXPECT_VALUE);
        JsonNode value = parseValue(nextState);
        if (fieldRef.object.has(fieldRef.key)) {
            throw errorContext.atPosition(lexer).generic("Duplicate key");
        }
        fieldRef.object.set(fieldRef.key, value);
    }

    private TomlObjectNode getOrCreateObject(ObjectNode node, String field) {
        JsonNode existing = node.get(field);
        if (existing == null) {
            return (TomlObjectNode) node.putObject(field);
        } else if (existing.isObject()) {
            return (TomlObjectNode) existing;
        } else {
            throw errorContext.atPosition(lexer).generic("Path into existing non-object value of type " + node.getNodeType());
        }
    }

    private TomlArrayNode getOrCreateArray(ObjectNode node, String field) {
        JsonNode existing = node.get(field);
        if (existing == null) {
            return (TomlArrayNode) node.putArray(field);
        } else if (existing.isArray()) {
            return (TomlArrayNode) existing;
        } else {
            throw errorContext.atPosition(lexer).generic("Path into existing non-array value of type " + node.getNodeType());
        }
    }

    private static class FieldRef {
        final TomlObjectNode object;
        final String key;

        FieldRef(TomlObjectNode object, String key) {
            this.object = object;
            this.key = key;
        }
    }

    private static class TomlObjectNode extends ObjectNode {
        boolean closed = false;
        boolean defined = false;

        TomlObjectNode(JsonNodeFactory nc) {
            super(nc);
        }
    }

    private static class TomlArrayNode extends ArrayNode {
        boolean closed = false;

        TomlArrayNode(JsonNodeFactory nf) {
            super(nf);
        }

        TomlArrayNode(JsonNodeFactory nf, int capacity) {
            super(nf, capacity);
        }
    }

    private static class JsonNodeFactoryImpl extends JsonNodeFactory {
        @Override
        public ArrayNode arrayNode() {
            return new TomlArrayNode(this);
        }

        @Override
        public ArrayNode arrayNode(int capacity) {
            return new TomlArrayNode(this, capacity);
        }

        @Override
        public ObjectNode objectNode() {
            return new TomlObjectNode(this);
        }
    }
}
