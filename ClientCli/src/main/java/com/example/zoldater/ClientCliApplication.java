package com.example.zoldater;

import com.example.zoldater.client.ClientMaster;
import com.example.zoldater.core.configuration.InitialConfiguration;
import com.example.zoldater.core.configuration.InitialConfigurationBuilder;
import com.example.zoldater.core.configuration.data.ValueArgumentData;
import com.example.zoldater.core.configuration.data.VariableArgumentData;

import static com.example.zoldater.core.enums.ArchitectureTypeEnum.NON_BLOCKING_ARCH;
import static com.example.zoldater.core.enums.ArgumentTypeEnum.*;

public class ClientCliApplication {

    public static void main(String[] args) {
        InitialConfiguration initialConfiguration = generateInitialConfig();
        ClientMaster clientMaster = new ClientMaster(initialConfiguration);
        clientMaster.start();
    }

    private static InitialConfiguration generateInitialConfig() {
        return new InitialConfigurationBuilder()
//                .setArchitectureType(ONLY_THREADS_ARCH)
//                .setArchitectureType(WITH_EXECUTORS_ARCH)
                .setArchitectureType(NON_BLOCKING_ARCH)
                .setServerAddress("192.168.1.64")
                .setVariableArgumentData(new VariableArgumentData(ARRAY_ELEMENTS, 1000, 10000, 500))
                .setValueArgumentData1(new ValueArgumentData(DELTA_MS, 10))
                .setValueArgumentData2(new ValueArgumentData(CLIENTS_NUMBER, 30))
                .setRequestsPerClient(new ValueArgumentData(REQUESTS_PER_CLIENT, 100))
                .createInitialConfiguration();
    }

}
