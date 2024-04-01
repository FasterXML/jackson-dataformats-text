package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.core.io.CharacterEscapes;

public class ControlQuoteEscapeStrategy implements EscapeStrategy {
    @Override
    public int[] getEscapeCodesForAscii() {
        return CharacterEscapes.standardAsciiEscapesForJSON();
    }
}
