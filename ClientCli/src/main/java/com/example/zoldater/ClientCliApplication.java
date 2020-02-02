package com.example.zoldater;

import com.example.zoldater.client.ClientMaster;
import com.example.zoldater.core.configuration.InitialConfiguration;
import com.example.zoldater.core.configuration.InitialConfigurationBuilder;
import com.example.zoldater.core.configuration.data.ValueArgumentData;
import com.example.zoldater.core.configuration.data.VariableArgumentData;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static com.example.zoldater.core.enums.ArchitectureTypeEnum.*;
import static com.example.zoldater.core.enums.ArgumentTypeEnum.*;

public class ClientCliApplication {

    public static void main(String[] args) {
        InitialConfiguration initialConfiguration = generateInitialConfig();
        ClientMaster clientMaster = new ClientMaster(initialConfiguration);
        clientMaster.start();
    }

    private static InitialConfiguration generateInitialConfig() {
        return new InitialConfigurationBuilder()
                .setArchitectureType(ONLY_THREADS_ARCH)
//                .setArchitectureType(WITH_EXECUTORS_ARCH)
//                .setArchitectureType(NON_BLOCKING_ARCH)
                .setServerAddress("localhost")
                .setVariableArgumentData(new VariableArgumentData(CLIENTS_NUMBER, 2, 21, 6))
                .setValueArgumentData1(new ValueArgumentData(DELTA_MS, 20))
                .setValueArgumentData2(new ValueArgumentData(ARRAY_ELEMENTS, 100))
                .setRequestsPerClient(new ValueArgumentData(REQUESTS_PER_CLIENT, 5))
                .createInitialConfiguration();
    }

}
