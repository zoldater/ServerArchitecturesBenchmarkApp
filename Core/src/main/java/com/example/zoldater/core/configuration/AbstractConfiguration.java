package com.example.zoldater.core.configuration;


import com.example.zoldater.core.enums.ArchitectureTypeEnum;

public abstract class AbstractConfiguration {
    private final ArchitectureTypeEnum architectureType;
    private final String serverAddress;

    public AbstractConfiguration(ArchitectureTypeEnum architectureType,
                                 String serverAddress) {
        this.architectureType = architectureType;
        this.serverAddress = serverAddress;
    }

    public ArchitectureTypeEnum getArchitectureType() {
        return architectureType;
    }

    public String getServerAddress() {
        return serverAddress;
    }

}
