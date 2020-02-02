package com.example.zoldater.core.configuration;


import com.example.zoldater.core.configuration.data.ValueArgumentData;
import com.example.zoldater.core.configuration.data.VariableArgumentData;
import com.example.zoldater.core.enums.ArchitectureTypeEnum;

import static com.example.zoldater.core.enums.ArchitectureTypeEnum.ONLY_THREADS_ARCH;
import static com.example.zoldater.core.enums.ArgumentTypeEnum.*;

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

    public static InitialConfiguration generateInitialConfig() {
        return new InitialConfigurationBuilder()
                .setArchitectureType(ONLY_THREADS_ARCH)
                .setServerAddress("192.168.1.64")
                .setVariableArgumentData(new VariableArgumentData(ARRAY_ELEMENTS, 1000, 10001, 1000))
                .setValueArgumentData1(new ValueArgumentData(CLIENTS_NUMBER, 50))
                .setValueArgumentData2(new ValueArgumentData(DELTA_MS, 25))
                .setRequestsPerClient(new ValueArgumentData(REQUESTS_PER_CLIENT, 100))
                .createInitialConfiguration();
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
