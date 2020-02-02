package com.example.zoldater.server.worker;

import org.tinylog.Logger;
import ru.spbau.mit.core.proto.IterationProtos.IterationOpenRequest;
import ru.spbau.mit.core.proto.IterationProtos.IterationOpenResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.MessageFormat;

public class IterationOpenServerWorker implements Runnable {
    private final Socket socket;
    private IterationOpenRequest request;
    private final IterationOpenResponse response;

    public IterationOpenServerWorker(Socket socket) {
        this.socket = socket;
        this.response = IterationOpenResponse.newBuilder()
                .setAnswer("OK")
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
                    request = IterationOpenRequest.parseDelimitedFrom(is);
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

    public IterationOpenRequest getRequest() {
        return request;
    }
}
