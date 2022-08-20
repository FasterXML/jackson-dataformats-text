package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.NumberInput;
import com.fasterxml.jackson.core.util.VersionUtil;
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

    private final TomlStreamReadException.ErrorContext errorContext;
    private final int options;
    private final Lexer lexer;

    private TomlToken next;

    private Parser(
            IOContext ioContext,
            TomlStreamReadException.ErrorContext errorContext,
            int options,
            Reader reader
    ) throws IOException {
        this.errorContext = errorContext;
        this.options = options;
        this.lexer = new Lexer(reader, ioContext, errorContext);
        lexer.prohibitInternalBufferAllocate = (options & TomlWriteFeature.INTERNAL_PROHIBIT_INTERNAL_BUFFER_ALLOCATE) != 0;
        this.next = lexer.yylex();
    }

    public static ObjectNode parse(
            IOContext ioContext,
            int options,
            Reader reader
    ) throws IOException {
        Parser parser = new Parser(ioContext, new TomlStreamReadException.ErrorContext(ioContext.contentReference(), null), options, reader);
        ObjectNode node = parser.parse();
        parser.lexer.releaseBuffers();
        return node;
    }

    private TomlToken peek() throws TomlStreamReadException {
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
                part = lexer.textBuffer.contentsAsString();
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
                throw errorContext.atPosition(lexer).generic("Path into existing non-object value of type " + existing.getNodeType());
            }
        }
    }

    private JsonNode parseValue(int nextState) throws IOException {
        TomlToken firstToken = peek();
        switch (firstToken) {
            case STRING:
                String text = lexer.textBuffer.contentsAsString();
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

        if (TomlReadFeature.PARSE_JAVA_TIME.enabledIn(options)) {
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
                    VersionUtil.throwInternal();
                    throw new AssertionError();
                }
            }
            return factory.pojoNode(value);
        } else {
            return factory.textNode(text);
        }
    }

    private JsonNode parseInt(int nextState) throws IOException {
        char[] buffer = lexer.getTextBuffer();
        int start = lexer.getTextBufferStart();
        int length = lexer.getTextBufferEnd() - lexer.getTextBufferStart();
        for (int i = 0; i < length; i++) {
            if (buffer[start + i] == '_') {
                // slow path to remove underscores
                buffer = new String(buffer, start, length).replace("_", "").toCharArray();
                start = 0;
                length = buffer.length;
                break;
            }
        }

        pollExpected(TomlToken.INTEGER, nextState);
        if (length > 2) {
            char baseChar = buffer[start + 1];
            if (baseChar == 'x' || baseChar == 'o' || baseChar == 'b') {
                start += 2;
                length -= 2;
                String text = new String(buffer, start, length);
                // note: we parse all these as unsigned. Hence the weird int limits.
                // hex
                if (baseChar == 'x') {
                    if (length <= 31 / 4) {
                        return factory.numberNode(Integer.parseInt(text, 16));
                    } else if (length <= 63 / 4) {
                        return factory.numberNode(Long.parseLong(text, 16));
                    } else {
                        return factory.numberNode(new BigInteger(text, 16));
                    }
                }
                // octal
                if (baseChar == 'o') {
                    // this is a bit conservative, but who uses octal anyway?
                    if (length <= 31 / 3) {
                        return factory.numberNode(Integer.parseInt(text, 8));
                    } else if (text.length() <= 63 / 3) {
                        return factory.numberNode(Long.parseLong(text, 8));
                    } else {
                        return factory.numberNode(new BigInteger(text, 8));
                    }
                }
                // binary
                assert baseChar == 'b';
                if (length <= 31) {
                    return factory.numberNode(Integer.parseUnsignedInt(text, 2));
                } else if (length <= 63) {
                    return factory.numberNode(Long.parseUnsignedLong(text, 2));
                } else {
                    return factory.numberNode(new BigInteger(text, 2));
                }
            }
        }
        // decimal
        boolean negative;
        if (buffer[start] == '-') {
            start++;
            length--;
            negative = true;
        } else if (buffer[start] == '+') {
            start++;
            length--;
            negative = false;
        } else {
            negative = false;
        }
        // adapted from JsonParserBase
        if (length <= 9) {
            int v = NumberInput.parseInt(buffer, start, length);
            if (negative) v = -v;
            return factory.numberNode(v);
        }
        if (length <= 18 || NumberInput.inLongRange(buffer, start, length, negative)) {
            long v = NumberInput.parseLong(buffer, start, length);
            if (negative) v = -v;
            // Might still fit in int, need to check
            if ((int) v == v) {
                return factory.numberNode((int) v);
            } else {
                return factory.numberNode(v);
            }
        }
        return factory.numberNode(new BigInteger(new String(buffer, start, length)));
    }

    private JsonNode parseFloat(int nextState) throws IOException {
        String text = lexer.yytext().replace("_", "");
        pollExpected(TomlToken.FLOAT, nextState);
        if (text.endsWith("nan")) {
            return factory.numberNode(Double.NaN);
        } else if (text.endsWith("inf")) {
            return factory.numberNode(text.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
        } else {
            try {
                BigDecimal dec = NumberInput.parseBigDecimal(text);
                return factory.numberNode(dec);
            } catch (NumberFormatException e) {
                throw errorContext.atPosition(lexer).invalidNumber(e, text);
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

    private TomlObjectNode getOrCreateObject(ObjectNode node, String field) throws TomlStreamReadException {
        JsonNode existing = node.get(field);
        if (existing == null) {
            return (TomlObjectNode) node.putObject(field);
        } else if (existing.isObject()) {
            return (TomlObjectNode) existing;
        } else {
            throw errorContext.atPosition(lexer).generic("Path into existing non-object value of type " + existing.getNodeType());
        }
    }

    private TomlArrayNode getOrCreateArray(ObjectNode node, String field) throws TomlStreamReadException {
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

    @SuppressWarnings("serial") // only used internally, no need to be JDK serializable
    private static class TomlObjectNode extends ObjectNode {
        boolean closed = false;
        boolean defined = false;

        TomlObjectNode(JsonNodeFactory nc) {
            super(nc);
        }
    }

    @SuppressWarnings("serial") // only used internally, no need to be JDK serializable
    private static class TomlArrayNode extends ArrayNode {
        boolean closed = false;

        TomlArrayNode(JsonNodeFactory nf) {
            super(nf);
        }

        TomlArrayNode(JsonNodeFactory nf, int capacity) {
            super(nf, capacity);
        }
    }

    @SuppressWarnings("serial") // only used internally, no need to be JDK serializable
    private static class JsonNodeFactoryImpl extends JsonNodeFactory {
        public JsonNodeFactoryImpl() {
            super(true); // exact bigdecimals
        }

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
