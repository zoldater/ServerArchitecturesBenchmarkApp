package com.example.zoldater.client.worker;

import com.example.zoldater.core.configuration.data.ValueArgumentData;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.IterationProtos.IterationOpenRequest;
import ru.spbau.mit.core.proto.IterationProtos.IterationOpenResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class IterationOpenClientWorker implements Runnable {
    private final Socket socket;
    private final IterationOpenRequest request;
    private IterationOpenResponse response;

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
            this.response = IterationOpenResponse.parseDelimitedFrom(is);
        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        }
    }

    public IterationOpenResponse getResponse() {
        return response;
    }
}
