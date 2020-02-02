package com.example.zoldater;

import com.example.zoldater.client.ClientMaster;
import com.example.zoldater.core.configuration.InitialConfiguration;
import com.example.zoldater.core.configuration.InitialConfigurationBuilder;
import com.example.zoldater.core.configuration.data.ValueArgumentData;
import com.example.zoldater.core.configuration.data.VariableArgumentData;

import static com.example.zoldater.core.enums.ArchitectureTypeEnum.WITH_EXECUTORS_ARCH;
import static com.example.zoldater.core.enums.ArgumentTypeEnum.*;

public class ClientCliApplication {

    public static void main(String[] args) {
        InitialConfiguration initialConfiguration = generateInitialConfig();
        ClientMaster clientMaster = new ClientMaster(initialConfiguration);
        clientMaster.start();
    }

    private static InitialConfiguration generateInitialConfig() {
        return new InitialConfigurationBuilder()
                .setArchitectureType(WITH_EXECUTORS_ARCH)
                .setServerAddress("192.168.1.64")
                .setVariableArgumentData(new VariableArgumentData(DELTA_MS, 0, 501, 50))
                .setValueArgumentData1(new ValueArgumentData(CLIENTS_NUMBER, 10))
                .setValueArgumentData2(new ValueArgumentData(ARRAY_ELEMENTS, 2000))
                .setRequestsPerClient(new ValueArgumentData(REQUESTS_PER_CLIENT, 10))
                .createInitialConfiguration();
    }

}
