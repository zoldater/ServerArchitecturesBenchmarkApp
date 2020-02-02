package com.example.zoldater.client.worker;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.configuration.data.ValueArgumentData;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.IterationProtos;
import ru.spbau.mit.core.proto.IterationProtos.IterationCloseResponse;
import ru.spbau.mit.core.proto.IterationProtos.IterationOpenRequest;
import ru.spbau.mit.core.proto.IterationProtos.IterationOpenResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.MessageFormat;

import static ru.spbau.mit.core.proto.IterationProtos.*;

public class IterationCloseClientWorker implements Runnable {
    private final Socket socket;
    private IterationCloseRequest request;
    private IterationCloseResponse response;

    private static final String SENDING_LOG_TEMPLATE = "Request with question = {0} successfully sent!";
    private static final String RECEIVING_LOG_TEMPLATE = "Response successfully received: m1 = {0}, m2 = {1}, m3 = {2}";

    public IterationCloseClientWorker(Socket socket) {
        this.socket = socket;
        this.request = IterationCloseRequest.newBuilder()
                .setQuestion("OK")
                .build();
    }

    @Override
    public void run() {
        Logger.info("InitialClientWorker starts!");
        InputStream is = null;
        OutputStream os = null;
        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            request.writeDelimitedTo(os);
            Logger.debug(MessageFormat.format(SENDING_LOG_TEMPLATE, request.getQuestion()));
            this.response = IterationCloseResponse.parseDelimitedFrom(is);
            Logger.debug(MessageFormat.format(RECEIVING_LOG_TEMPLATE, response.getAveragePerClientTime(),
                    response.getAverageProcessingTime(),
                    response.getAverageSortingTime()));
        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        }
    }

    public IterationCloseResponse getResponse() {
        return response;
    }
}
