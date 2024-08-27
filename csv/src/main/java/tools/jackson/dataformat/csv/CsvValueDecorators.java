package tools.jackson.dataformat.csv;

import java.util.Objects;

import tools.jackson.core.JacksonException;

/**
 * Container class for default {@link CsvValueDecorator} implementations
 *
 * @since 2.18
 */
public class CsvValueDecorators
{
    /**
     * {@link StringPrefixSuffixDecorator} that uses square brackets ({@code []})
     * around decorated value, but does not require their use (removes if used,
     * ignores it not).
     */
    public final static CsvValueDecorator OPTIONAL_BRACKETS_DECORATOR
         = new StringPrefixSuffixDecorator("[", "]", true);

    /**
     * {@link StringPrefixSuffixDecorator} that uses square brackets ({@code []})
     * around decorated value, and requires their use (if value has no matching
     * decoration, an exception is thrown when attempting to read the value).
     */
    public final static CsvValueDecorator STRICT_BRACKETS_DECORATOR
        = new StringPrefixSuffixDecorator("[", "]", false);

    /**
     * Factory method for constructing a {@link StringPrefixSuffixDecorator} with
     * given prefix and suffix, both optional.
     */
    public static CsvValueDecorator optionalPrefixSuffixDecorator(String prefix, String suffix) {
        return new StringPrefixSuffixDecorator(prefix, suffix, true);
    }

    /**
     * Factory method for constructing a {@link StringPrefixSuffixDecorator} with
     * given prefix and suffix, both required.
     */
    public static CsvValueDecorator requiredPrefixSuffixDecorator(String prefix, String suffix) {
        return new StringPrefixSuffixDecorator(prefix, suffix, false);
    }

    /**
     * Decorated that adds static prefix and suffix around value to decorate value;
     * removes the same when un-decorating. Handling of the case where decoration
     * is missing on deserialization (reading) depends on where decorator is
     * created with "optional" or "strict" setting
     * (see {@link StringPrefixSuffixDecorator#StringPrefixSuffixDecorator}).
     */
    public static class StringPrefixSuffixDecorator
        implements CsvValueDecorator
    {
        /**
         * Decoration added before value being decorated: for example, if decorating
         * with brackets, this would be opening bracket {@code [ }.
         */
        protected final String _prefix;

        /**
         * Decoration added after value being decorated: for example, if decorating
         * with brackets, this would be closing bracket {@code ] }.
         */
        protected final String _suffix;

        /**
         * Whether existence of prefix and suffix decoration is optional
         * ({@code true}) or required ({@code false}): if required
         * and value does does not have decorations, deserialization (reading)
         * will fail with an exception; if optional value is exposed as is.
         */
        protected final boolean _optional;

        public StringPrefixSuffixDecorator(String prefix, String suffix, boolean optional) {
            _prefix = Objects.requireNonNull(prefix);
            _suffix = Objects.requireNonNull(suffix);
            _optional = optional;
        }

        @Override
        public String decorateValue(CsvGenerator gen, String plainValue) throws JacksonException {
            return new StringBuilder(plainValue.length() + _prefix.length() + _suffix.length())
                    .append(_prefix)
                    .append(plainValue)
                    .append(_suffix)
                    .toString()
                    ;
        }

        @Override
        public String undecorateValue(CsvParser parser, String decoratedValue) throws JacksonException {
            if (!decoratedValue.startsWith(_prefix)) {
                if (!_optional) {
                    parser._reportCsvReadError(String.format(
                            "Decorated value of column '%s' does not start with expected prefix (\"%s\"); value: \"%s\"",
                            parser.currentName(), _prefix, decoratedValue));
                }
                return decoratedValue;
            }
            if (!decoratedValue.endsWith(_suffix)) {
                if (!_optional) {
                    parser._reportCsvReadError(String.format(
                            "Decorated value of column '%s' does not end with expected suffix (\"%s\"); value: \"%s\"",
                            parser.currentName(), _suffix, decoratedValue));
                }
                return decoratedValue;
            }
            int start = _prefix.length();
            int end = decoratedValue.length() - _suffix.length();
            // One minor complication: suffix and prefix could overlap
            if (start >= end) {
                return "";
            }
            return decoratedValue.substring(start, end);
        }
    }
}
