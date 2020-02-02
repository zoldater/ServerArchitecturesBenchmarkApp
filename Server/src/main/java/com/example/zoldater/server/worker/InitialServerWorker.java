package com.example.zoldater.server.worker;

import com.example.zoldater.core.enums.PortConstantEnum;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureRequest;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.MessageFormat;

public class InitialServerWorker implements Runnable {
    private final Socket socket;
    private ArchitectureRequest request;
    private final ArchitectureResponse response;

    public InitialServerWorker(Socket socket) {
        this.socket = socket;
        this.response = ArchitectureResponse.newBuilder()
                .setConnectionPort(PortConstantEnum.SERVER_PROCESSING_PORT.getPort())
                .build();
    }

    @Override
    public void run() {
        Logger.info("InitialServerWorker starts!");
        InputStream is;
        OutputStream os;
        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            while (!socket.isClosed()) {
                if (is.available() != 0) {
                    request = ArchitectureRequest.parseDelimitedFrom(is);
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

    public ArchitectureRequest getRequest() {
        return request;
    }
}
