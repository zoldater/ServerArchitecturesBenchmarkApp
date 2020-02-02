package com.example.zoldater.server.worker;

import com.example.zoldater.core.Utils;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.IterationProtos;
import ru.spbau.mit.core.proto.IterationProtos.IterationCloseRequest;
import ru.spbau.mit.core.proto.IterationProtos.IterationCloseResponse;
import ru.spbau.mit.core.proto.IterationProtos.IterationOpenRequest;
import ru.spbau.mit.core.proto.IterationProtos.IterationOpenResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.MessageFormat;

public class IterationCloseServerWorker implements Runnable {
    private final Socket socket;
    private IterationCloseRequest request;
    private IterationCloseResponse response;

    private static final String RECEIVING_LOG_TEMPLATE = "Request with question {0} successfully received!";
    private static final String SENDING_LOG_TEMPLATE = "Response successfully sent: m1 = {0}, m2 = {1}, m3 = {2}";

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
