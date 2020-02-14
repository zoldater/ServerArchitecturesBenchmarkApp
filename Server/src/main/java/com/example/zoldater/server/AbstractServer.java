package com.example.zoldater.server;

import com.example.zoldater.core.benchmarks.BenchmarkBox;
import com.example.zoldater.core.benchmarks.BenchmarkBoxContainer;
import com.example.zoldater.core.enums.PortConstantEnum;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public abstract class AbstractServer implements Runnable {
    protected final int port;
    protected ServerSocket serverSocket;
    protected final Semaphore semaphoreSending;
    protected final CountDownLatch resultsSendingLatch;
    protected final List<Thread> clientThreads = new ArrayList<>();
    protected final int clientsCount;
    protected final int requestsPerClient;
    protected final BenchmarkBoxContainer benchmarkBoxContainer = new BenchmarkBoxContainer();


    protected AbstractServer(Semaphore semaphoreSending, CountDownLatch resultsSendingLatch, int clientsCount, int requestsPerClient) {
        this.resultsSendingLatch = resultsSendingLatch;
        this.requestsPerClient = requestsPerClient;
        this.port = PortConstantEnum.SERVER_PROCESSING_PORT.getPort();
        this.semaphoreSending = semaphoreSending;
        this.clientsCount = clientsCount;
    }


    public BenchmarkBoxContainer getBenchmarkBoxContainer() {
        return benchmarkBoxContainer;
    }

    public abstract void shutdown();

}
