package com.example.zoldater.client.worker;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.enums.ArchitectureTypeEnum;
import com.example.zoldater.core.receiver.configuration.ArchitectureResponseReceiver;
import com.example.zoldater.core.sender.MessageSender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureRequest;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureResponse;

import java.io.IOException;
import java.net.Socket;
import java.text.MessageFormat;

public class InitialClientWorker implements Runnable {
    private final Socket socket;
    private final ArchitectureRequest request;
    private ArchitectureResponse response;
    private final MessageSender<ArchitectureRequest> sender;
    private final ArchitectureResponseReceiver receiver;

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SENDING_LOG_TEMPLATE = "Request with architecture ordinal {0} successfully sent!";
    private static final String RECEIVING_LOG_MSG = "Response successfully received! Port for connection - {0}";

    public InitialClientWorker(ArchitectureTypeEnum architectureType, Socket socket) {
        this.socket = socket;
        this.request = ArchitectureRequest.newBuilder()
                .setArchitectureOrdinal(architectureType.ordinal())
                .build();
        this.sender = new MessageSender<>(request);
        this.receiver = new ArchitectureResponseReceiver();
    }

    @Override
    public void run() {
        LOGGER.traceEntry();
        try {
            sender.send(socket);
            LOGGER.debug(MessageFormat.format(SENDING_LOG_TEMPLATE, request.getArchitectureOrdinal()));
            receiver.receive(socket);
            this.response = receiver.getReceivedMessage();
            LOGGER.debug(MessageFormat.format(RECEIVING_LOG_MSG, response.getConnectionPort()));
        } finally {
            Utils.closeResources(socket, null, null);
        }
    }

    public ArchitectureResponse getResponse() {
        return response;
    }
}
