package com.example.zoldater.server;

import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class BlockingServerPoolSending extends AbstractBlockingServer {
    private final ExecutorService sendingService;

    protected BlockingServerPoolSending(int requestsPerClient, Socket socket, ExecutorService sendingService) {
        super(requestsPerClient, socket);
        this.sendingService = sendingService;
    }

    @Override
    public void sendMessage(SortingProtos.SortingMessage sortedMessage, OutputStream outputStream) {
        sendingService.submit(() -> {
            try {
                sortedMessage.writeDelimitedTo(outputStream);
            } catch (IOException e) {
                Logger.error(e);
                throw new RuntimeException(e);
            }
        });
    }
}
