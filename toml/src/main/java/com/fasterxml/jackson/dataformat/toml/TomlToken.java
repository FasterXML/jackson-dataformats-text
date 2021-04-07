package com.fasterxml.jackson.dataformat.toml;

enum TomlToken {
    UNQUOTED_KEY,
    DOT_SEP,
    STRING,
    TRUE,
    FALSE,
    OFFSET_DATE_TIME,
    LOCAL_DATE_TIME,
    LOCAL_DATE,
    LOCAL_TIME,
    FLOAT,
    INTEGER,
    STD_TABLE_OPEN,
    STD_TABLE_CLOSE,
    INLINE_TABLE_OPEN,
    INLINE_TABLE_CLOSE,
    ARRAY_TABLE_OPEN,
    ARRAY_TABLE_CLOSE,
    ARRAY_OPEN,
    ARRAY_CLOSE,
    KEY_VAL_SEP,
    COMMA,
    /**
     * Whitespace token that is only permitted in arrays
     */
    ARRAY_WS_COMMENT_NEWLINE
}
