package com.fasterxml.jackson.dataformat.toml;

class ParserOptions {
    static final ParserOptions DEFAULT = new ParserOptions(false, false);

    final boolean parseTemporalAsJavaTime;
    final boolean bigNumericTypes;

    public ParserOptions(boolean parseTemporalAsJavaTime, boolean bigNumericTypes) {
        this.parseTemporalAsJavaTime = parseTemporalAsJavaTime;
        this.bigNumericTypes = bigNumericTypes;
    }
}
