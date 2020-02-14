package com.example.zoldater.server;

import com.example.zoldater.core.benchmarks.BenchmarkBox;
import com.example.zoldater.core.Utils;
import ru.spbau.mit.core.proto.SortingProtos;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

public class BlockingServerThread extends AbstractBlockingServer {


    protected BlockingServerThread(Semaphore semaphoreSending, CountDownLatch resultsSendingLatch, int clientsCount, int requestsPerClient) {
        super(semaphoreSending, resultsSendingLatch, clientsCount, requestsPerClient);
    }

    @Override
    public SortingProtos.SortingMessage sort(SortingProtos.SortingMessage message, BenchmarkBox benchmarkBox) throws ExecutionException, InterruptedException {
        benchmarkBox.startSorting();
        final SortingProtos.SortingMessage sortedMessage = Utils.processSortingMessage(message);
        benchmarkBox.finishSorting();
        return sortedMessage;
    }

    @Override
    public void send(SortingProtos.SortingMessage message, OutputStream outputStream, BenchmarkBox benchmarkBox) throws IOException {
        Utils.writeToStream(message, outputStream);
        benchmarkBox.finishProcessing();
    }
}
