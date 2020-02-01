package com.example.zoldater.server.worker;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.enums.PortConstantEnum;
import com.example.zoldater.core.receiver.configuration.ArchitectureRequestReceiver;
import com.example.zoldater.core.sender.MessageSender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureRequest;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureResponse;

import java.net.Socket;
import java.text.MessageFormat;

public class InitialServerWorker implements Runnable {
    private final Socket socket;
    private ArchitectureRequest request;
    private ArchitectureResponse response;

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String RECEIVING_LOG_TEMPLATE = "Request with architecture ordinal {0} successfully received!";
    private static final String SENDING_LOG_TEMPLATE = "Response successfully sent! Processing port - {0}";

    public InitialServerWorker(Socket socket) {
        this.socket = socket;
        this.response = ArchitectureResponse.newBuilder()
                .setConnectionPort(PortConstantEnum.SERVER_PROCESSING_PORT.getPort())
                .build();
    }

    @Override
    public void run() {
        LOGGER.traceEntry();
        ArchitectureRequestReceiver receiver = new ArchitectureRequestReceiver();
        receiver.receive(socket);
        request = receiver.getReceivedMessage();
        LOGGER.debug(MessageFormat.format(RECEIVING_LOG_TEMPLATE, request.getArchitectureOrdinal()));
        MessageSender<ArchitectureResponse> sender = new MessageSender<>(response);
        sender.send(socket);
        LOGGER.debug(MessageFormat.format(SENDING_LOG_TEMPLATE, response.getConnectionPort()));
    }

    public ArchitectureRequest getRequest() {
        return request;
    }
}
