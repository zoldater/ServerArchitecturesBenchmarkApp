package com.example.zoldater.server;

import com.example.zoldater.core.benchmarks.ServerBenchmarkBox;
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
            serverSocket = new ServerSocket(port, Short.MAX_VALUE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        semaphoreSending.release();
        while (!serverSocket.isClosed()) {
            Socket socket;
            try {
                socket = serverSocket.accept();
                ServerBenchmarkBox serverBenchmarkBox = new ServerBenchmarkBox();
                serverBenchmarkBoxes.add(serverBenchmarkBox);
                Thread thread = new Thread(() -> processSingleClient(socket, serverBenchmarkBox));
                clientThreads.add(thread);
                thread.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    public void processSingleClient(Socket socket, ServerBenchmarkBox serverBenchmarkBox) {
        try {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            for (int i = 0; i < requestsPerClient && !isUnlocked; i++) {
                SortingProtos.SortingMessage sortingMessage = Utils.readSortingMessage(inputStream);
                serverBenchmarkBox.startProcessing();
                if (sortingMessage == null) {
                    throw new RuntimeException("Sorting message is null");
                }
                SortingProtos.SortingMessage sortedMessage = sort(sortingMessage, serverBenchmarkBox);
                if (sortedMessage == null) {
                    return;
                }
                send(sortedMessage, outputStream, serverBenchmarkBox);
            }
            resultsSendingLatch.countDown();
        } catch (IOException | ExecutionException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        } catch (InterruptedException ignored) {
            // If any thread finished session, another threads will be interrupted
            // for collecting stats
        }
    }

    public abstract SortingMessage sort(SortingMessage message, ServerBenchmarkBox serverBenchmarkBox) throws ExecutionException, InterruptedException;

    public abstract void send(SortingMessage message, OutputStream outputStream, ServerBenchmarkBox serverBenchmarkBox) throws IOException, ExecutionException, InterruptedException;

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
