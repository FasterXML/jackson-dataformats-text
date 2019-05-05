package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator.Feature;

/**
 * Character escapes for CSV. There are multiple types of escapes.
 *
 * <ul>
 * <li>no escapes - return all characters the same way they are defined</li>
 * <li>quote escape - return all characters except the quote character which is escaped (backwards compat) </li>
 * <li>control escape - same as {@link CharTypes#get7BitOutputEscapes()}, escape all control characters</li>
 * <li> control and quote escape - do not double up quote, escape control characters and quote.</li>
 * </ul>
 */
public final class CsvCharacterEscapes extends CharacterEscapes
{

    private static final long serialVersionUID = 1L;

    // No character escapes, every character returned as is.
    private static final CsvCharacterEscapes sNoEscapesInstance = new CsvCharacterEscapes(new int[0]);

    // Only escape quotes, controlled by {@link Feature#ESCAPE_QUOTE_CHAR_WITH_ESCAPE_CHAR}.
    private static final CsvCharacterEscapes sQuoteEscapesInstance;

    // Only escape control chars, do *not* escape the quote char. See (@link Feature#ESCAPE_CONTROL_CHARS_WITH_ESCAPE_CHAR}.
    private static final CsvCharacterEscapes sControlEscapesInstance;

    // Escape control chars and the quote char.
    private static final CsvCharacterEscapes sControlQuoteEscapesInstance = new CsvCharacterEscapes(CharacterEscapes.standardAsciiEscapesForJSON());

    private static final CsvCharacterEscapes [] sEscapes;

    static {
        int[] quoteEscapes = new int[(int) '"' + 1];
        quoteEscapes[(int) '"'] = '"';
        sQuoteEscapesInstance = new CsvCharacterEscapes(quoteEscapes);

        int[] controlEscapes = CharacterEscapes.standardAsciiEscapesForJSON();
        controlEscapes['"'] = 0; // do not escape ", double it up.
        sControlEscapesInstance = new CsvCharacterEscapes(controlEscapes);

        sEscapes = new CsvCharacterEscapes[4];
        sEscapes[0] = sNoEscapesInstance;
        sEscapes[1] = sQuoteEscapesInstance;
        sEscapes[2] = sControlEscapesInstance;
        sEscapes[3] = sControlQuoteEscapesInstance;
    }


    private final int[] escapes;

    private CsvCharacterEscapes(int[] escapes)
    {
        this.escapes = escapes;
    }

    public static CsvCharacterEscapes noEscapesInstance()
    {
        return sNoEscapesInstance;
    }

    public static CsvCharacterEscapes quoteEscapesInstance()
    {
        return sQuoteEscapesInstance;
    }

    public static CsvCharacterEscapes controlEscapesInstance()
    {
        return sControlEscapesInstance;
    }

    public static CsvCharacterEscapes controlQuoteEscapesInstance()
    {
        return sControlQuoteEscapesInstance;
    }

    public static CsvCharacterEscapes fromCsvFeatures(int csvFeatures)
    {
        int idx = 0;
        idx |= CsvGenerator.Feature.ESCAPE_QUOTE_CHAR_WITH_ESCAPE_CHAR.enabledIn(csvFeatures) ? 1 : 0;
        idx |= Feature.ESCAPE_CONTROL_CHARS_WITH_ESCAPE_CHAR.enabledIn(csvFeatures) ? 2 : 0;

        return sEscapes[idx];
    }

    @Override
    public SerializableString getEscapeSequence(int ch)
    {
        return null; // unused for CSV escapes
    }

    @Override
    public int[] getEscapeCodesForAscii()
    {
        return escapes;
    }
}
