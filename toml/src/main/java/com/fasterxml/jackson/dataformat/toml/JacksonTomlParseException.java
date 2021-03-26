package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.io.InputSourceReference;

public class JacksonTomlParseException extends StreamReadException {
    private JacksonTomlParseException(JsonParser p, String msg, JsonLocation loc) {
        super(p, msg, loc);
    }

    static class ErrorContext {
        private final InputSourceReference inputSourceReference;
        private final JsonParser parser;

        ErrorContext(InputSourceReference inputSourceReference, JsonParser parser) {
            this.inputSourceReference = inputSourceReference;
            this.parser = parser;
        }

        ErrorBuilder atPosition(Lexer lexer) {
            return new ErrorBuilder(lexer);
        }

        class ErrorBuilder {
            private final JsonLocation location;

            private ErrorBuilder(Lexer lexer) {
                this.location = new JsonLocation(
                        inputSourceReference,
                        -1,
                        lexer.getCharPos(),
                        lexer.getLine() + 1,
                        lexer.getColumn() + 1
                );
            }

            JacksonTomlParseException unknownToken() {
                return generic("Unknown token");
            }

            JacksonTomlParseException unexpectedToken(TomlToken actual, String expected) {
                return new JacksonTomlParseException(
                        parser,
                        "Unexpected token: Got " + actual + ", expected " + expected,
                        location
                );
            }

            JacksonTomlParseException generic(String message) {
                return new JacksonTomlParseException(parser, message, location);
            }
        }
    }

}
