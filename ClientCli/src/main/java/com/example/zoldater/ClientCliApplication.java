package com.example.zoldater;

import com.example.zoldater.client.ClientMaster;
import com.example.zoldater.core.configuration.InitialConfiguration;

public class ClientCliApplication {

    public static void main(String[] args) {
        InitialConfiguration initialConfiguration = InitialConfiguration.generateInitialConfig();
        ClientMaster clientMaster = new ClientMaster(initialConfiguration);
        clientMaster.start();
    }
}
