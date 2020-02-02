package com.example.zoldater.client.worker;

import com.example.zoldater.core.configuration.data.ValueArgumentData;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.IterationProtos.IterationOpenRequest;
import ru.spbau.mit.core.proto.IterationProtos.IterationOpenResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.MessageFormat;

public class IterationOpenClientWorker implements Runnable {
    private final Socket socket;
    private final IterationOpenRequest request;
    private IterationOpenResponse response;

    private static final String SENDING_LOG_TEMPLATE = "Request with clientsNumber = {0} and requestsPerClient = {1} successfully sent!";
    private static final String RECEIVING_LOG_TEMPLATE = "Response successfully received with answer - {0}";

    public IterationOpenClientWorker(Socket socket, ValueArgumentData clientsNumber, ValueArgumentData requestsPerClient) {
        this.socket = socket;
        this.request = IterationOpenRequest.newBuilder()
                .setClientsNumber(clientsNumber.getValue())
                .setRequestPerClient(requestsPerClient.getValue())
                .build();
    }

    @Override
    public void run() {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            request.writeDelimitedTo(os);
            Logger.debug(MessageFormat.format(SENDING_LOG_TEMPLATE, request.getClientsNumber(), request.getRequestPerClient()));
            this.response = IterationOpenResponse.parseDelimitedFrom(is);
            Logger.debug(MessageFormat.format(RECEIVING_LOG_TEMPLATE, response.getAnswer()));
        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        }
    }

    public IterationOpenResponse getResponse() {
        return response;
    }
}
