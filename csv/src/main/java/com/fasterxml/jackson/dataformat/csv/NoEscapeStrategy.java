package com.fasterxml.jackson.dataformat.csv;

public class NoEscapeStrategy implements EscapeStrategy
{
    @Override
    public int[] getEscapeCodesForAscii() {
        return new int[0];
    }
}
