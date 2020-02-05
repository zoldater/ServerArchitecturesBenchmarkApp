package com.example.zoldater.core.enums;

public enum PortConstantEnum {
    SERVER_CONFIGURATION_PORT(19432),
    SERVER_PROCESSING_PORT(19433);

    private final int port;

    PortConstantEnum(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
