package com.costin.converter;

public class Token {
    private TokenType type;
    private String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) { // for transforming to java
        this.value = value;
    }

    public void delete() {
        this.value = "";
        this.type = TokenType.IGNORE;
    }
}
