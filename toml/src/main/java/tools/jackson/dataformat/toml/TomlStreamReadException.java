package tools.jackson.dataformat.toml;

import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.JsonParser;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.ContentReference;

public class TomlStreamReadException
    extends StreamReadException
{
    private static final long serialVersionUID = 1L;

    TomlStreamReadException(JsonParser p, String msg, TokenStreamLocation loc) {
        super(p, msg, loc);
    }

    TomlStreamReadException(JsonParser p, String msg, TokenStreamLocation loc, Throwable rootCause) {
        super(p, msg, loc, rootCause);
    }

    @Override
    public TomlStreamReadException withParser(JsonParser p) {
        this._processor = p;
        return this;
    }

    static class ErrorContext {
        final ContentReference contentReference;
        final JsonParser parser;

        ErrorContext(ContentReference contentReference, JsonParser parser) {
            this.contentReference = contentReference;
            this.parser = parser;
        }

        ErrorBuilder atPosition(Lexer lexer) {
            return new ErrorBuilder(lexer);
        }

        class ErrorBuilder {
            private final TokenStreamLocation location;

            ErrorBuilder(Lexer lexer) {
                this.location = new TokenStreamLocation(
                        contentReference,
                        -1,
                        lexer.getCharPos(),
                        lexer.getLine() + 1,
                        lexer.getColumn() + 1
                );
            }

            TomlStreamReadException unexpectedToken(TomlToken actual, String expected) {
                return new TomlStreamReadException(
                        parser,
                        "Unexpected token: Got " + actual + ", expected " + expected,
                        location
                );
            }

            TomlStreamReadException generic(String message) {
                return new TomlStreamReadException(parser, message, location);
            }

            TomlStreamReadException outOfBounds(NumberFormatException cause) {
                return new TomlStreamReadException(parser,
                        "Number out of bounds", location, cause);
            }

            TomlStreamReadException invalidNumber(Exception cause, String value) {
                return new TomlStreamReadException(parser,
                        "Invalid number representation ('"+value+"'), problem: "+cause.getMessage(), location, cause);
            }
        }
    }
}
