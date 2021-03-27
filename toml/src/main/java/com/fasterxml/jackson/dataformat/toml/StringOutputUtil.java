package com.fasterxml.jackson.dataformat.toml;

class StringOutputUtil {
    public static final int UNQUOTED_KEY = 1;
    public static final int LITERAL_STRING = 2;
    public static final int BASIC_STRING = 4;
    public static final int BASIC_STRING_NO_ESCAPE = 8;
    public static final int ASCII_ONLY = 16;

    private static final int EMPTY_STRING_CATS = LITERAL_STRING | BASIC_STRING | BASIC_STRING_NO_ESCAPE | ASCII_ONLY;

    public static final int MASK_SIMPLE_KEY = -1; // Should exclude multi-line keys when/if we support them.
    public static final int MASK_STRING = ~UNQUOTED_KEY;

    static int categorize(String s) {
        if (s.isEmpty()) {
            return EMPTY_STRING_CATS;
        }
        int flags = -1;
        for (int i = 0; i < s.length();) {
            char hi = s.charAt(i++);
            if (Character.isHighSurrogate(hi) && i < s.length()) {
                char lo = s.charAt(i);
                if (Character.isLowSurrogate(lo)) {
                    i++;
                    flags &= categorize(Character.toCodePoint(hi, lo));
                } else {
                    return 0; // surrogates not allowed
                }
            } else {
                flags &= categorize(hi);
            }
        }
        return flags;
    }

    static int categorize(char[] text, int offset, int len) {
        if (len == 0) {
            return EMPTY_STRING_CATS;
        }
        int flags = -1;
        for (int i = 0; i < len;) {
            char hi = text[offset + i++];
            if (Character.isHighSurrogate(hi) && i < len) {
                char lo = text[offset + i];
                if (Character.isLowSurrogate(lo)) {
                    i++;
                    flags &= categorize(Character.toCodePoint(hi, lo));
                } else {
                    return 0; // surrogates not allowed
                }
            } else {
                flags &= categorize(hi);
            }
        }
        return flags;
    }

    static int categorize(int c) {
        if (c > Character.MAX_CODE_POINT || (c >= Character.MIN_SURROGATE && c <= Character.MAX_SURROGATE)) {
            // cannot write surrogates
            return 0;
        }

        // first, get the very restrictive unquoted keys out of the way.
        // unquoted-key = 1*( ALPHA / DIGIT / %x2D / %x5F ) ; A-Z / a-z / 0-9 / - / _
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
            return LITERAL_STRING | BASIC_STRING | UNQUOTED_KEY | BASIC_STRING_NO_ESCAPE | ASCII_ONLY;
        }

        // non-ascii is allowed everywhere.
        if (c >= 0x80) {
            return LITERAL_STRING | BASIC_STRING | BASIC_STRING_NO_ESCAPE;
        }

        // quotes need escaping.
        if (c == '"') {
            return LITERAL_STRING | BASIC_STRING | ASCII_ONLY;
        }
        // apostrophes can only be placed in basic strings.
        if (c == '\'') {
            return BASIC_STRING | BASIC_STRING_NO_ESCAPE | ASCII_ONLY;
        }
        // backslash needs escaping in basic strings.
        if (c == '\\') {
            return BASIC_STRING | LITERAL_STRING | ASCII_ONLY;
        }

        // now, for some "normal" characters:
        // basic-unescaped = wschar / %x21 / %x23-5B / %x5D-7E / non-ascii
        // literal-char = %x09 / %x20-26 / %x28-7E / non-ascii
        // these are basically identical except for " (0x22) and ' (0x27), which are already handled above,
        // and for \ (0x5c). This reduces the expression to the following
        if (c == '\t' || (c >= 0x20 && c <= 0x7e)) {
            return LITERAL_STRING | BASIC_STRING | BASIC_STRING_NO_ESCAPE | ASCII_ONLY;
        }

        // The rest is some ascii control chars. Those aren't allowed in literal strings, they need escapes. Could
        // potentially be put into multi-line literal strings, I guess, but we don't use those yet.
        return BASIC_STRING | ASCII_ONLY;
    }

    /**
     * Get the basic string escape sequence for a character, or {@code null} if the character does not need to be
     * escaped.
     */
    static String getBasicStringEscape(char c) {
        switch (c) {
            case '\b':
                return "\\b";
            case '\t':
                return("\\t");
            case '\n':
                return("\\n");
            case '\f':
                return("\\f");
            case '"':
                return("\\\"");
            case '\\':
                return("\\\\");
            default:
                if (c < 0x10) {
                    return "\\u000" + Integer.toHexString(c);
                } else if (c < 0x20 || c == 0x7f) {
                    return "\\u00" + Integer.toHexString(c);
                } else {
                    // note: this includes potential surrogates. We let the output handle those.
                    return null;
                }
        }
    }
}
