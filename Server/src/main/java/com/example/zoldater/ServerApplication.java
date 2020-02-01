package com.example.zoldater;

import com.example.zoldater.server.ServerMaster;

public class ServerApplication {
    public static void main(String[] args) {
        ServerMaster serverMaster = new ServerMaster();
        serverMaster.start();
    }
}
