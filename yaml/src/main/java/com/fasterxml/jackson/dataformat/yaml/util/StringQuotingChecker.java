package com.fasterxml.jackson.dataformat.yaml.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper class that defines API used by
 * {@link com.fasterxml.jackson.dataformat.yaml.YAMLGenerator}
 * to check whether property names and String values need to be quoted or not.
 * Also contains default logic implementation; may be sub-classes to provide
 * alternate implementation.
 *
 * @since 2.12
 */
public abstract class StringQuotingChecker
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /* As per <a href="https://yaml.org/type/bool.html">YAML Spec</a> there are a few
     * aliases for booleans, and we better quote such values as keys; although Jackson
     * itself has no problems dealing with them, some other tools do have.
     */
    // 02-Apr-2019, tatu: Some names will look funny if escaped: let's leave out
    //    single letter case (esp so 'y' won't get escaped)
    // 17-Sep-2020, tatu: [dataformats-text#226] No, let's be consistent w/ values
    private final static Set<String> DEFAULT_QUOTABLE_NAMES = new HashSet<>(Arrays.asList(
            "y", "Y", "n", "N",
            "yes", "Yes", "YES", "no", "No", "NO",
            "true", "True", "TRUE", "false", "False", "FALSE",
            "on", "On", "ON", "off", "Off", "OFF",
            "null", "Null", "NULL"
    ));

    /**
     * As per YAML <a href="https://yaml.org/type/null.html">null</a>
     * and <a href="https://yaml.org/type/bool.html">boolean</a> type specs,
     * better retain quoting for some values
     */
    private final static Set<String> DEFAULT_QUOTABLE_VALUES = new HashSet<>(Arrays.asList(
            "false", "False", "FALSE",
            "null", "Null", "NULL",
            "on", "On", "ON", "off", "Off", "OFF",
            "true", "True", "TRUE",
            "y", "Y", "n", "N",
            "yes", "Yes", "YES", "no", "No", "NO"
    ));

    /**
     * Method called by
     * {@link com.fasterxml.jackson.dataformat.yaml.YAMLGenerator}
     * to check whether given property name should be quoted: usually
     * to prevent it from being read as non-String key (boolean or number)
     */
    public boolean needToQuoteName(String name)
    {
        // empty String does indeed require quoting
        if (name.length() == 0) {
            return true;
        }
        switch (name.charAt(0)) {
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
            return isDefaultQuotableName(name);

        // And then numbers
        case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':
        case '-' : case '+': case '.':
            return true;
        }
        return false;
    }

    /**
     * Method called by
     * {@link com.fasterxml.jackson.dataformat.yaml.YAMLGenerator}
     * to check whether given String value should be quoted: usually
     * to prevent it from being value of different type (boolean or number)
     */
    public boolean needToQuoteValue(String value)
    {
        // empty String does indeed require quoting
        if (value.length() == 0) {
            return true;
        }
        switch (value.charAt(0)) {
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
            if (isDefaultQuotableValue(value)) {
                return true;
            }
            break;
        }
        return valueHasQuotableChar(value);
    }

    protected boolean isDefaultQuotableName(String name) {
        return DEFAULT_QUOTABLE_NAMES.contains(name);
    }

    protected boolean isDefaultQuotableValue(String name) {
        return DEFAULT_QUOTABLE_VALUES.contains(name);
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
                if (i > 0) {
                    char d = inputStr.charAt(i-1);
                    if (' ' == d || '\t' == d) {
                        return true;
                    }
                }
                break;
            case ':':
                // [dataformats-text#201]: limit quoting with MINIMIZE_QUOTES
                if (i < (end-1)) {
                    char d = inputStr.charAt(i + 1);
                    if (' ' == d || '\t' == d) {
                        return true;
                    }
                }
                break;
            default:
            }
        }
        return false;
    }
    
    /**
     * Default {@link StringQuotingChecker} implementation used unless
     * custom implementation registered.
     */
    public final static class Default
        extends StringQuotingChecker
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        private final static Default INSTANCE = new Default();

        public Default() { }

        public static Default instance() { return INSTANCE; }
    }
}
