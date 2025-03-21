package com.fasterxml.jackson.dataformat.yaml.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper class that defines API used by
 * {@link com.fasterxml.jackson.dataformat.yaml.YAMLGenerator}
 * to check whether property names and String values need to be quoted or not.
 * Also contains default logic implementation; may be sub-classed to provide
 * alternate implementation.
 *
 * @since 2.12
 */
public abstract class StringQuotingChecker
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;
    private static final int SPACE_CODE_POINT = 0x0020;

    /**
     * As per YAML <a href="https://yaml.org/type/null.html">null</a>
     * and <a href="https://yaml.org/type/bool.html">boolean</a> type specs,
     * better retain quoting for some keys (property names) and values.
     */
    private final static Set<String> RESERVED_KEYWORDS = new HashSet<>(Arrays.asList(
            // 02-Apr-2019, tatu: Some names will look funny if escaped: let's leave out
            //    single letter case (esp so 'y' won't get escaped)
            // 17-Sep-2020, tatu: [dataformats-text#226] No, let's be consistent w/ values
            "false", "False", "FALSE",
            "n", "N",
            "no", "No", "NO",
            "null", "Null", "NULL",
            "on", "On", "ON",
            "off", "Off", "OFF",
            "true", "True", "TRUE",
            "y", "Y",
            "yes", "Yes", "YES"
    ));

    /**
     * Method called by
     * {@link com.fasterxml.jackson.dataformat.yaml.YAMLGenerator}
     * to check whether given property name should be quoted: usually
     * to prevent it from being read as non-String key (boolean or number)
     */
    public abstract boolean needToQuoteName(String name);

    /**
     * Method called by
     * {@link com.fasterxml.jackson.dataformat.yaml.YAMLGenerator}
     * to check whether given String value should be quoted: usually
     * to prevent it from being value of different type (boolean or number).
     */
    public abstract boolean needToQuoteValue(String value);

    /**
     * Helper method that sub-classes may use to see if given String value is
     * one of:
     *<ul>
     * <li>YAML 1.1 keyword representing
     *  <a href="https://yaml.org/type/bool.html">boolean</a>
     *  </li>
     * <li>YAML 1.1 keyword representing
     *  <a href="https://yaml.org/type/null.html">null</a> value
     *   </li>
     * <li>empty String (length 0)
     *   </li>
     *</li>
     * and returns {@code true} if so.
     *
     * @param value String to check
     *
     * @return {@code true} if given value is a Boolean or Null representation
     *   (as per YAML 1.1 specification) or empty String
     */
    protected boolean isReservedKeyword(String value) {
        if (value.length() == 0) {
            return true;
        }
        return _isReservedKeyword(value.charAt(0), value);
    }

    protected boolean _isReservedKeyword(int firstChar, String name) {
        switch (firstChar) {
        // First, reserved name starting chars:
        case 'f': // false
        case 'n': // no/n/null
        case 'o': // on/off
        case 't': // true
        case 'y': // yes/y
        case 'F': // False
        case 'N': // No/N/Null
        case 'O': // On/Off
        case 'T': // True
        case 'Y': // Yes/Y
            return RESERVED_KEYWORDS.contains(name);
        case '~': // null alias (see [dataformats-text#274])
            return true;
        }
        return false;
    }

    /**
     * Helper method that sub-classes may use to see if given String value
     * looks like a YAML 1.1 numeric value and would likely be considered
     * a number when parsing unless quoting is used.
     */
    protected boolean looksLikeYAMLNumber(String name) {
        if (name.length() > 0) {
            return _looksLikeYAMLNumber(name.charAt(0), name);
        }
        return false;
    }

    protected boolean _looksLikeYAMLNumber(int firstChar, String name) {
        switch (firstChar) {
        // And then numbers
        case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':
        case '-' : case '+': case '.':
            return true;
        }
        return false;
    }

    /**
     * As per YAML <a href="https://yaml.org/spec/1.2/spec.html#id2788859">Plain Style</a>unquoted
     * strings are restricted to a reduced charset and must be quoted in case they contain
     * one of the following characters or character combinations.
     */
    protected boolean valueHasQuotableChar(String inputStr)
    {
        final int end = inputStr.length();
        for (int i = 0; i < end; ++i) {
            switch (inputStr.charAt(i)) {
            case '[':
            case ']':
            case '{':
            case '}':
            case ',':
                return true;
            case '#':
                // [dataformats-text#201]: limit quoting with MINIMIZE_QUOTES
                // (but not recognized as comment unless starts line or preceded by whitespace)
                if (precededOnlyByBlank(inputStr, i)) {
                    return true;
                }
                break;
            case ':':
                // [dataformats-text#201]: limit quoting with MINIMIZE_QUOTES
                // (but recognized as separator only if end-of-line or followed by whitespace)
                if (followedOnlyByBlank(inputStr, i)) {
                    return true;
                }
                break;
            default:
            }
        }
        return false;
    }

    // @since 2.17
    protected boolean precededOnlyByBlank(String inputStr, int offset) {
        if (offset == 0) {
            return true;
        }
        return isBlank(inputStr.charAt(offset - 1));
    }

    // @since 2.17
    protected boolean followedOnlyByBlank(String inputStr, int offset) {
        if (offset == inputStr.length() - 1) {
            return true;
        }
        return isBlank(inputStr.charAt(offset + 1));
    }

    // @since 2.17
    protected boolean isBlank(char value) {
        return (' ' == value || '\t' == value);
    }

    /**
     * Looks like we may get names with "funny characters" so.
     *
     * @since 2.13.2
     */
    protected boolean nameHasQuotableChar(String inputStr)
    {
        // 31-Jan-2022, tatu: As per [dataformats-text#306] linefeed is
        //   problematic. I'm sure there are likely other cases, but let's
        //   start with the obvious ones, control characters
        final int end = inputStr.length();
        for (int i = 0; i < end; ++i) {
            int ch = inputStr.charAt(i);
            if (ch < SPACE_CODE_POINT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Default {@link StringQuotingChecker} implementation used unless
     * custom implementation registered.
     */
    public static class Default
        extends StringQuotingChecker
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        private final static Default INSTANCE = new Default();

        public Default() { }

        public static Default instance() { return INSTANCE; }

        /**
         * Default implementation will call
         * {@link #isReservedKeyword(String)} and
         * {@link #looksLikeYAMLNumber(String)} to determine
         * if quoting should be applied.
         */
        @Override
        public boolean needToQuoteName(String name)
        {
            return isReservedKeyword(name) || looksLikeYAMLNumber(name)
                    // 31-Jan-2022, tatu: as per [dataformats-text#306] may also
                    //   have other characters requiring quoting...
                    || nameHasQuotableChar(name);
        }

        /**
         * Default implementation will call
         * {@link #isReservedKeyword(String)}
         * and {@link #valueHasQuotableChar(String)} to determine
         * if quoting should be applied.
         */
        @Override
        public boolean needToQuoteValue(String value)
        {
            // Only consider reserved keywords but not numbers?
            return isReservedKeyword(value) || valueHasQuotableChar(value);
        }
    }
}
