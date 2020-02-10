package com.example.zoldater.server;

import com.example.zoldater.core.BenchmarkBox;
import com.example.zoldater.core.enums.PortConstantEnum;
import org.jetbrains.annotations.Nullable;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public abstract class AbstractServer implements Runnable {
    protected final int port;
    protected ServerSocket serverSocket;
    protected final List<BenchmarkBox> benchmarkBoxes;
    protected final Semaphore semaphoreSending;
    protected final List<Thread> clientThreads = new ArrayList<>();

    protected AbstractServer(List<BenchmarkBox> benchmarkBoxes, Semaphore semaphoreSending) {
        this.semaphoreSending = semaphoreSending;
        this.port = PortConstantEnum.SERVER_PROCESSING_PORT.getPort();
        this.benchmarkBoxes = benchmarkBoxes;
    }


    public abstract void shutdown();

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

}
