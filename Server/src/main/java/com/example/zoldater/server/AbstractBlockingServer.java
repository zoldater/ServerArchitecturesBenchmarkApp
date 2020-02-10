package com.example.zoldater.server;

import com.example.zoldater.core.BenchmarkBox;
import com.example.zoldater.core.Utils;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public abstract class AbstractBlockingServer extends AbstractServer {


    protected AbstractBlockingServer(List<BenchmarkBox> benchmarkBoxes, Semaphore semaphoreSending) {
        super(benchmarkBoxes, semaphoreSending);
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        semaphoreSending.release();
        while (!getServerSocket().isClosed()) {
            Socket socket;
            try {
                socket = getServerSocket().accept();
                BenchmarkBox benchmarkBox = BenchmarkBox.create();
                benchmarkBoxes.add(benchmarkBox);
                benchmarkBox.startClientSession();
                Thread thread = new Thread(() -> processSingleClient(socket, benchmarkBox));
                clientThreads.add(thread);
                thread.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    public void processSingleClient(Socket socket, BenchmarkBox benchmarkBox) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            while (!socket.isClosed()) {
                benchmarkBox.startProcessing();
                SortingProtos.SortingMessage sortingMessage = Utils.readSortingMessage(inputStream);
                if (sortingMessage == null) {
                    break;
                }
                benchmarkBox.startSorting();
                SortingProtos.SortingMessage sortedMessage = sort(sortingMessage);
                benchmarkBox.finishSorting();
                send(sortedMessage, outputStream);
                benchmarkBox.finishProcessing();
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        } finally {
            benchmarkBox.finishClientSession();
        }
    }

    public abstract SortingMessage sort(SortingMessage message) throws ExecutionException, InterruptedException;

    public abstract void send(SortingMessage message, OutputStream outputStream) throws IOException;

    @Override
    public void shutdown() {
        try {
            serverSocket.close();
            stopWork();
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
