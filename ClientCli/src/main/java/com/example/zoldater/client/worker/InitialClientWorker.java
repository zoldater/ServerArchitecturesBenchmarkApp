package com.example.zoldater.client.worker;

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

    public InitialClientWorker(ArchitectureTypeEnum architectureType, int iterationsNumber, Socket socket) {
        this.socket = socket;
        this.request = ArchitectureRequest.newBuilder()
                .setArchitectureCode(architectureType.getCode())
                .setIterationsNumber(iterationsNumber)
                .build();
    }

    @Override
    public void run() {
        InputStream is;
        OutputStream os;
        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            request.writeDelimitedTo(os);
            this.response = ArchitectureResponse.parseDelimitedFrom(is);
        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        }
    }

    public ArchitectureResponse getResponse() {
        return response;
    }
}
