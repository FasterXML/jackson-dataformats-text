package com.fasterxml.jackson.dataformat.javaprop.io;

import java.util.Arrays;

/**
 * Container class for definitions of characters to escape.
 */
public class JPropEscapes
{
    private final static char[] HEX = "0123456789ABCDEF".toCharArray();

    private final static int UNICODE_ESCAPE = -1;
    
    private final static int[] sValueEscapes;
    static {
        final int[] table = new int[256];
        // For values, fewer escapes needed, but most control chars need them
        for (int i = 0; i < 32; ++i) {
            table[i] = UNICODE_ESCAPE;
            // also high-bit ones
            table[128+i] = UNICODE_ESCAPE;
        }
        // also, that one weird character needs escaping:
        table[0x7F] = UNICODE_ESCAPE;
        
        // except for "well-known" ones
        table['\t'] = 't';
        table['\r'] = 'r';
        table['\n'] = 'n';
        
        // Beyond that, just backslash
        table['\\'] = '\\';
        sValueEscapes = table;
    }

    private final static int[] sKeyEscapes;
    static {
        // with keys, start with value escapes, and add the rest
        final int[] table = Arrays.copyOf(sValueEscapes, 256);

        // comment line starters (could get by with just start char but whatever)
        table['#'] = '#';
        table['!'] = '!';
        // and then equals (and equivalents) that mark end of key
        table['='] = '=';
        table[':'] = ':';
        // plus space chars are escapes too
        table[' '] = ' ';

        sKeyEscapes = table;
    }

    public static void appendKey(StringBuilder sb, String key) {
        final int end = key.length();
        if (end == 0) {
            return;
        }
        final int[] esc = sKeyEscapes;
        // first quick loop for common case of no escapes
        int i = 0;

        while (true) {
            char c = key.charAt(i);
            if ((c > 0xFF) || esc[c] != 0) {
                break;
            }
            sb.append(c);
            if (++i == end) {
                return;
            }
        }
        _appendWithEscapes(sb, key, esc, i);
    }

    public static StringBuilder appendValue(String value) {
        final int end = value.length();
        if (end == 0) {
            return null; // nothing to write, but "write as is"
        }
        final int[] esc = sValueEscapes;
        int i = 0;

        // little bit different here; we scan to ensure nothing to escape
        while (true) {
            char c = value.charAt(i);
            if ((c > 0xFF) || esc[c] != 0) {
                break;
            }
            if (++i == end) {
                return null; // write as-is
            }
        }
        // if not real work
        StringBuilder sb = new StringBuilder(end + 5 + (end>>3));
        for (int j = 0; j < i; ++j) {
            sb.append(value.charAt(j));
        }
        _appendWithEscapes(sb, value, esc, i);
        return sb;
    }
    
    private static void _appendWithEscapes(StringBuilder sb, String key,
            int[] esc, int i)
    {
        final int end = key.length();
        do {
            char c = key.charAt(i);
            int type = (c > 0xFF) ? UNICODE_ESCAPE : esc[c];
            if (type == 0) {
                sb.append(c);
                continue;
            }
            if (type == UNICODE_ESCAPE) {
                sb.append('\\');
                sb.append('u');
                sb.append(HEX[c >>> 12]);
                sb.append(HEX[(c >> 8) & 0xF]);
                sb.append(HEX[(c >> 4) & 0xF]);
                sb.append(HEX[c & 0xF]);
            } else {
                sb.append('\\');
                sb.append((char) type);
            }
        } while (++i < end);
    }
}
