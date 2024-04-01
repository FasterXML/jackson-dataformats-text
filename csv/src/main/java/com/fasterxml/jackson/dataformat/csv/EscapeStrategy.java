package com.fasterxml.jackson.dataformat.csv;

public interface EscapeStrategy {
    int[] getEscapeCodesForAscii();
}
