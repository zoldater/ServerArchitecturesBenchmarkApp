package com.example.zoldater.server.worker;

import org.tinylog.Logger;
import ru.spbau.mit.core.proto.IterationProtos.IterationCloseRequest;
import ru.spbau.mit.core.proto.IterationProtos.IterationCloseResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class IterationCloseServerWorker implements Runnable {
    private final Socket socket;
    private IterationCloseRequest request;
    private final IterationCloseResponse response;

    public IterationCloseServerWorker(Socket socket, long averageClientTime, long averageProcessingTime, long averageSortingTime) {
        this.socket = socket;
        this.response = IterationCloseResponse.newBuilder()
                .setAveragePerClientTime(averageClientTime)
                .setAverageProcessingTime(averageProcessingTime)
                .setAverageSortingTime(averageSortingTime)
                .build();
    }

    @Override
    public void run() {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            while (!socket.isClosed()) {
                if (is.available() != 0) {
                    request = IterationCloseRequest.parseDelimitedFrom(is);
                    response.writeDelimitedTo(os);
                    break;
                } else {
                    Thread.yield();
                }
            }
        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        }
    }

    public IterationCloseRequest getRequest() {
        return request;
    }
}
