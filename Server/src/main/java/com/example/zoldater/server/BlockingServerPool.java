package com.example.zoldater.server;

import com.example.zoldater.core.BenchmarkBox;
import com.example.zoldater.core.Utils;
import jdk.jshell.execution.Util;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class BlockingServerPool extends AbstractBlockingServer {
    private final ExecutorService sendingService = Executors.newSingleThreadExecutor();
    private final ExecutorService sortingService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    protected BlockingServerPool(List<BenchmarkBox> benchmarkBoxes, Semaphore semaphoreSending) {
        super(benchmarkBoxes, semaphoreSending);
    }


    @Override
    public SortingProtos.SortingMessage sort(SortingProtos.SortingMessage message) throws ExecutionException, InterruptedException {
        Future<SortingProtos.SortingMessage> messageFuture = sortingService.submit(() -> Utils.processSortingMessage(message));
        SortingProtos.SortingMessage sortedMessage = messageFuture.get();
        return sortedMessage;
    }

    @Override
    public void send(SortingProtos.SortingMessage message, OutputStream outputStream) {
        sendingService.submit(() -> {
            try {
                Utils.writeToStream(message, outputStream);
            } catch (IOException e) {
                Logger.error(e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected void stopWork() {
        sortingService.shutdownNow();
        sendingService.shutdownNow();
        super.stopWork();
    }
}
