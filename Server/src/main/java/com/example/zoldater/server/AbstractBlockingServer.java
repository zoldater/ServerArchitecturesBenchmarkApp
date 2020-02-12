package com.example.zoldater.server;

import com.example.zoldater.core.BenchmarkBox;
import com.example.zoldater.core.Utils;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

public abstract class AbstractBlockingServer extends AbstractServer {
    protected volatile boolean isUnlocked = false;

    protected AbstractBlockingServer(Semaphore semaphoreSending, CountDownLatch resultsSendingLatch, int clientsCount, int requestsPerClient) {
        super(semaphoreSending, resultsSendingLatch, clientsCount, requestsPerClient);
    }


    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        semaphoreSending.release();
        while (!serverSocket.isClosed()) {
            Socket socket;
            try {
                socket = serverSocket.accept();
                BenchmarkBox benchmarkBox = BenchmarkBox.create();
                benchmarkBoxes.add(benchmarkBox);
                Thread thread = new Thread(() -> processSingleClient(socket, benchmarkBox));
                clientThreads.add(thread);
                thread.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    public void processSingleClient(Socket socket, BenchmarkBox benchmarkBox) {
        try {
            countDownLatch.countDown();
            countDownLatch.await();
            benchmarkBox.startClientSession();
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            for (int i = 0; i < requestsPerClient && !isUnlocked; i++) {
                benchmarkBox.startProcessing();
                SortingProtos.SortingMessage sortingMessage = Utils.readSortingMessage(inputStream);
                if (sortingMessage == null) {
                    throw new RuntimeException("Sorting message is null");
                }
                benchmarkBox.startSorting();
                SortingProtos.SortingMessage sortedMessage = sort(sortingMessage, benchmarkBox);
                if (sortedMessage == null) {
                    return;
                }
                send(sortedMessage, outputStream, benchmarkBox);
            }
            resultsSendingLatch.countDown();
            benchmarkBox.finishClientSession();
        } catch (IOException | ExecutionException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        } catch (InterruptedException ignored) {
            // If any thread finished session, another threads will be interrupted
            // for collecting stats
        }
    }

    public abstract SortingMessage sort(SortingMessage message, BenchmarkBox benchmarkBox) throws ExecutionException, InterruptedException;

    public abstract void send(SortingMessage message, OutputStream outputStream, BenchmarkBox benchmarkBox) throws IOException, ExecutionException, InterruptedException;

    @Override
    public void shutdown() {
        try {
            stopWork();
            serverSocket.close();
        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        }
    }

    protected void stopWork() {
        for (Thread thread : clientThreads) {
            thread.interrupt();
        }
    }
}
