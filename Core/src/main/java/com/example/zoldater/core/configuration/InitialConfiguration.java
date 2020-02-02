package com.example.zoldater.core.configuration;


import com.example.zoldater.core.configuration.data.ValueArgumentData;
import com.example.zoldater.core.configuration.data.VariableArgumentData;
import com.example.zoldater.core.enums.ArchitectureTypeEnum;

public class InitialConfiguration extends AbstractConfiguration {
    private final VariableArgumentData variableArgumentData;
    private final ValueArgumentData requestsPerClientSession;
    private final ValueArgumentData valueArgumentData1;
    private final ValueArgumentData valueArgumentData2;


    public InitialConfiguration(ArchitectureTypeEnum architectureType,
                                String serverAddress,
                                VariableArgumentData variableArgumentData,
                                ValueArgumentData requestsPerClientSession,
                                ValueArgumentData valueArgumentData1,
                                ValueArgumentData valueArgumentData2) {
        super(architectureType, serverAddress);
        this.variableArgumentData = variableArgumentData;
        this.requestsPerClientSession = requestsPerClientSession;
        this.valueArgumentData1 = valueArgumentData1;
        this.valueArgumentData2 = valueArgumentData2;
    }

    public VariableArgumentData getVariableArgumentData() {
        return variableArgumentData;
    }

    public ValueArgumentData getRequestsPerClientSession() {
        return requestsPerClientSession;
    }

    public ValueArgumentData getValueArgumentData1() {
        return valueArgumentData1;
    }

    public ValueArgumentData getValueArgumentData2() {
        return valueArgumentData2;
    }
}
