package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.io.ContentReference;

public class JacksonTomlParseException extends StreamReadException {
    private JacksonTomlParseException(JsonParser p, String msg, JsonLocation loc) {
        super(p, msg, loc);
    }

    private JacksonTomlParseException(String msg, JsonLocation loc, Throwable rootCause) {
        super(msg, loc, rootCause);
    }

    static class ErrorContext {
        private final ContentReference ContentReference;
        private final JsonParser parser;

        ErrorContext(ContentReference ContentReference, JsonParser parser) {
            this.ContentReference = ContentReference;
            this.parser = parser;
        }

        ErrorBuilder atPosition(Lexer lexer) {
            return new ErrorBuilder(lexer);
        }

        class ErrorBuilder {
            private final JsonLocation location;

            private ErrorBuilder(Lexer lexer) {
                this.location = new JsonLocation(
                        ContentReference,
                        -1,
                        lexer.getCharPos(),
                        lexer.getLine() + 1,
                        lexer.getColumn() + 1
                );
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

            JacksonTomlParseException outOfBounds(NumberFormatException cause) {
                JacksonTomlParseException parseException = new JacksonTomlParseException("Number out of bounds", location, cause);
                parseException._processor = parser;
                return parseException;
            }
        }
    }

}
