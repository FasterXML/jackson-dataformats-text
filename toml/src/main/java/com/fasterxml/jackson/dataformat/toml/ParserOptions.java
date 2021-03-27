package com.fasterxml.jackson.dataformat.toml;

class ParserOptions {
    final boolean parseTemporalAsJavaTime;
    final boolean bigDecimal;

    public ParserOptions(boolean parseTemporalAsJavaTime, boolean bigDecimal) {
        this.parseTemporalAsJavaTime = parseTemporalAsJavaTime;
        this.bigDecimal = bigDecimal;
    }
}
