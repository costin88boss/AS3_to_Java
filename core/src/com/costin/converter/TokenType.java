package com.costin.converter;

public enum TokenType {
    IDENTIFIER,

    NUMBER,
    STRING,

    KEYWORD,
    OPERATOR, // apparently also separator
    COMMENT,
    IGNORE // special for -1 and size+1
}
