package com.example.zoldater.core.configuration;


import com.example.zoldater.core.configuration.data.ValueArgumentData;
import com.example.zoldater.core.configuration.data.VariableArgumentData;
import com.example.zoldater.core.enums.ArchitectureTypeEnum;

public class InitialConfigurationBuilder {
    private ArchitectureTypeEnum architectureType;
    private String serverAddress;
    private VariableArgumentData variableArgumentData;
    private ValueArgumentData requestsPerClient;
    private ValueArgumentData valueArgumentData1;
    private ValueArgumentData valueArgumentData2;

    public InitialConfigurationBuilder setArchitectureType(ArchitectureTypeEnum architectureType) {
        this.architectureType = architectureType;
        return this;
    }

    public InitialConfigurationBuilder setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
        return this;
    }

    public InitialConfigurationBuilder setVariableArgumentData(VariableArgumentData variableArgumentData) {
        this.variableArgumentData = variableArgumentData;
        return this;
    }

    public InitialConfigurationBuilder setRequestsPerClient(ValueArgumentData requestsPerClient) {
        this.requestsPerClient = requestsPerClient;
        return this;
    }

    public InitialConfigurationBuilder setValueArgumentData1(ValueArgumentData valueArgumentData1) {
        this.valueArgumentData1 = valueArgumentData1;
        return this;
    }

    public InitialConfigurationBuilder setValueArgumentData2(ValueArgumentData valueArgumentData2) {
        this.valueArgumentData2 = valueArgumentData2;
        return this;
    }

    public InitialConfiguration createInitialConfiguration() {
        return new InitialConfiguration(architectureType, serverAddress, variableArgumentData, requestsPerClient, valueArgumentData1, valueArgumentData2);
    }
}