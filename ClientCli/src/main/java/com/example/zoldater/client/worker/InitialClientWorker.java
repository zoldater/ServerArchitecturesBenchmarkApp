package com.example.zoldater.client.worker;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.enums.ArchitectureTypeEnum;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureRequest;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.MessageFormat;

public class InitialClientWorker implements Runnable {
    private final Socket socket;
    private final ArchitectureRequest request;
    private ArchitectureResponse response;

    private static final String SENDING_LOG_TEMPLATE = "Request with architecture code {0} successfully sent!";
    private static final String RECEIVING_LOG_TEMPLATE = "Response successfully received! Port for connection - {0}";

    public InitialClientWorker(ArchitectureTypeEnum architectureType, Socket socket) {
        this.socket = socket;
        this.request = ArchitectureRequest.newBuilder()
                .setArchitectureCode(architectureType.getCode())
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
            Logger.debug(MessageFormat.format(SENDING_LOG_TEMPLATE, request.getArchitectureCode()));
            this.response = ArchitectureResponse.parseDelimitedFrom(is);
            Logger.debug(MessageFormat.format(RECEIVING_LOG_TEMPLATE, response.getConnectionPort()));
        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        }
    }

    public ArchitectureResponse getResponse() {
        return response;
    }
}
