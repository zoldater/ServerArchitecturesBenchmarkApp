package com.example.zoldater.core.enums;

public enum ArgumentTypeEnum {
    ARRAY_ELEMENTS("N"),
    CLIENTS_NUMBER("M"),
    DELTA_MS("D"),
    REQUESTS_PER_CLIENT("X");

    private final String literal;

    ArgumentTypeEnum(String literal) {
        this.literal = literal;
    }

    public String getLiteral() {
        return literal;
    }
}