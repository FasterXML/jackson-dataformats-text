package com.fasterxml.jackson.dataformat.csv;

public class QuoteEscapeStrategy implements EscapeStrategy {
    @Override
    public int[] getEscapeCodesForAscii() {
        int[] escapes = new int['"' + 1];
        escapes['"'] = '"';
        return escapes;
    }
}
