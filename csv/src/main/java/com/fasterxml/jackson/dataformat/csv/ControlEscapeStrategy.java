package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.core.io.CharacterEscapes;

public class ControlEscapeStrategy implements EscapeStrategy {
    @Override
    public int[] getEscapeCodesForAscii() {
        int[] escapes = CharacterEscapes.standardAsciiEscapesForJSON();
        escapes['"'] = 0; // Do not escape double quotes.
        return escapes;
    }
}
