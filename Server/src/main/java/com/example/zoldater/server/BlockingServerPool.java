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

    protected BlockingServerPool(Semaphore semaphoreSending, CountDownLatch resultsSendingLatch, int clientsCount, int requestsPerClient) {
        super(semaphoreSending, resultsSendingLatch, clientsCount, requestsPerClient);
    }


    @Override
    public SortingProtos.SortingMessage sort(SortingProtos.SortingMessage message, BenchmarkBox benchmarkBox) throws ExecutionException, InterruptedException {
        Future<SortingProtos.SortingMessage> messageFuture = sortingService.submit(() -> {
            benchmarkBox.startSorting();
            final SortingProtos.SortingMessage message1 = Utils.processSortingMessage(message);
            return message1;
        });
        while (!messageFuture.isDone()) {
            Thread.yield();
        }
        SortingProtos.SortingMessage sortedMessage = messageFuture.get();
        benchmarkBox.finishSorting();
        return sortedMessage;
    }

    @Override
    public void send(SortingProtos.SortingMessage message, OutputStream outputStream, BenchmarkBox benchmarkBox) throws ExecutionException, InterruptedException {
        sendingService.submit(() -> {
            try {
                Utils.writeToStream(message, outputStream);
                benchmarkBox.finishProcessing();
            } catch (IOException e) {
                Logger.error(e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected void stopWork() {
        super.stopWork();
        sortingService.shutdown();
        sendingService.shutdown();
        try {
            while (!sortingService.awaitTermination(1, TimeUnit.SECONDS)) {
            }
            while (!sendingService.awaitTermination(1, TimeUnit.SECONDS)) {
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
