package com.example.zoldater.server.worker;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.enums.PortConstantEnum;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureRequest;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureResponse;
import ru.spbau.mit.core.proto.IterationProtos;
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

    private static final String RECEIVING_LOG_TEMPLATE = "Request with M = {0}, X = {1} successfully received!";
    private static final String SENDING_LOG_TEMPLATE = "Response successfully sent! Answer is {0}";

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
                    Logger.info(MessageFormat.format(RECEIVING_LOG_TEMPLATE, request.getClientsNumber(), request.getRequestPerClient()));
                    response.writeDelimitedTo(os);
                    Logger.info(MessageFormat.format(SENDING_LOG_TEMPLATE, response.getAnswer()));
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
