package com.example.zoldater.client.worker;

import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.MessageFormat;

import static ru.spbau.mit.core.proto.IterationProtos.*;

public class IterationCloseClientWorker implements Runnable {
    private final Socket socket;
    private final IterationCloseRequest request;
    private IterationCloseResponse response;

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
            this.response = IterationCloseResponse.parseDelimitedFrom(is);
        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        }
    }

    public IterationCloseResponse getResponse() {
        return response;
    }
}
