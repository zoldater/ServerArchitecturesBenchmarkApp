package com.example.zoldater.server;

import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.*;

public class BlockingServerPoolSending extends AbstractBlockingServer {
    private final ExecutorService sendingService = Executors.newSingleThreadExecutor();

    protected BlockingServerPoolSending(int requestsPerClient, Socket socket) {
        super(requestsPerClient, socket);
    }

    @Override
    public void sendMessage(SortingProtos.SortingMessage sortedMessage, OutputStream outputStream) {
        Future<?> future = sendingService.submit(() -> {
            try {
                sortedMessage.writeDelimitedTo(outputStream);
            } catch (IOException e) {
                Logger.error(e);
                throw new RuntimeException(e);
            }
        });
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        }
    }
}
