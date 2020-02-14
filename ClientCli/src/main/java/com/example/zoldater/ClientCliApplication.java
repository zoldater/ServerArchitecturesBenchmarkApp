package com.example.zoldater;

import com.example.zoldater.client.ClientMaster;
import com.example.zoldater.core.configuration.InitialConfiguration;
import com.example.zoldater.core.configuration.InitialConfigurationBuilder;
import com.example.zoldater.core.configuration.data.ValueArgumentDataBuilder;
import com.example.zoldater.core.configuration.data.VariableArgumentDataBuilder;
import org.knowm.xchart.XYChart;

import java.util.List;

import static com.example.zoldater.core.enums.ArchitectureTypeEnum.*;
import static com.example.zoldater.core.enums.ArgumentTypeEnum.*;

public class ClientCliApplication {

    public static void main(String[] args) {
        InitialConfiguration initialConfiguration = generateInitialConfig();
        startAndCollectCharts(initialConfiguration);
    }

    public static List<XYChart> startAndCollectCharts(InitialConfiguration initialConfiguration) {
        ClientMaster clientMaster = new ClientMaster(initialConfiguration);
        clientMaster.start();
        return clientMaster.getCharts();
    }

    private static InitialConfiguration generateInitialConfig() {
        return new InitialConfigurationBuilder()
                .setArchitectureType(ONLY_THREADS_ARCH)
//                .setArchitectureType(WITH_EXECUTORS_ARCH)
//                .setArchitectureType(NON_BLOCKING_ARCH)
                .setServerAddress("localhost")
                .setVariableArgumentData(new VariableArgumentDataBuilder().setArgumentTypeEnum(CLIENTS_NUMBER)
                        .setFrom(10)
                        .setTo(200)
                        .setStep(10)
                        .createVariableArgumentData())
                .setValueArgumentData1(new ValueArgumentDataBuilder().setArgumentTypeEnum(ARRAY_ELEMENTS)
                        .setValue(5000)
                        .createValueArgumentData())
                .setValueArgumentData2(new ValueArgumentDataBuilder().setArgumentTypeEnum(CLIENTS_NUMBER)
                        .setValue(50)
                        .createValueArgumentData())
                .setRequestsPerClient(new ValueArgumentDataBuilder().setArgumentTypeEnum(REQUESTS_PER_CLIENT)
                        .setValue(20)
                        .createValueArgumentData())
                .createInitialConfiguration();
    }

}
