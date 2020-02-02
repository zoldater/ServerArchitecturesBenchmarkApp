package com.example.zoldater.server;

import java.util.concurrent.ExecutorService;

public class NonBlockingServer extends AbstractServer {
    private final ExecutorService sendingService;

    protected NonBlockingServer(int requestsPerClient, ExecutorService sendingService) {
        super(requestsPerClient);
        this.sendingService = sendingService;
    }

    @Override
    public void run() {

    }
}
