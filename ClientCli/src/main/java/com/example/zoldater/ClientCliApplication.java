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
//                .setArchitectureType(ONLY_THREADS_ARCH)
//                .setArchitectureType(WITH_EXECUTORS_ARCH)
                .setArchitectureType(NON_BLOCKING_ARCH)
                .setServerAddress("localhost")
                .setVariableArgumentData(new VariableArgumentDataBuilder().setArgumentTypeEnum(ARRAY_ELEMENTS)
                        .setFrom(1000)
                        .setTo(5000)
                        .setStep(100)
                        .createVariableArgumentData())
                .setValueArgumentData1(new ValueArgumentDataBuilder().setArgumentTypeEnum(DELTA_MS)
                        .setValue(200)
                        .createValueArgumentData())
                .setValueArgumentData2(new ValueArgumentDataBuilder().setArgumentTypeEnum(CLIENTS_NUMBER)
                        .setValue(30)
                        .createValueArgumentData())
                .setRequestsPerClient(new ValueArgumentDataBuilder().setArgumentTypeEnum(REQUESTS_PER_CLIENT)
                        .setValue(30)
                        .createValueArgumentData())
                .createInitialConfiguration();
    }

}
