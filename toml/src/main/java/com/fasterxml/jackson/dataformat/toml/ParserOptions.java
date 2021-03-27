package com.fasterxml.jackson.dataformat.toml;

class ParserOptions {
    final boolean parseTemporalAsJavaTime;

    public ParserOptions(boolean parseTemporalAsJavaTime) {
        this.parseTemporalAsJavaTime = parseTemporalAsJavaTime;
    }
}
