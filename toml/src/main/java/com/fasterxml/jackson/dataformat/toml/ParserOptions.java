package com.fasterxml.jackson.dataformat.toml;

class ParserOptions {
    final boolean parseTemporalAsJavaTime;
    final boolean bigInteger;
    final boolean bigDecimal;

    public ParserOptions(boolean parseTemporalAsJavaTime, boolean bigInteger, boolean bigDecimal) {
        this.parseTemporalAsJavaTime = parseTemporalAsJavaTime;
        this.bigInteger = bigInteger;
        this.bigDecimal = bigDecimal;
    }
}
