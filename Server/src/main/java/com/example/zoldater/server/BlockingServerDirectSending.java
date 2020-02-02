package com.example.zoldater.server;

import ru.spbau.mit.core.proto.SortingProtos;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class BlockingServerDirectSending extends AbstractBlockingServer {
    protected BlockingServerDirectSending(int requestsPerClient, Socket socket) {
        super(requestsPerClient, socket);
    }

    @Override
    public void sendMessage(SortingProtos.SortingMessage sortedMessage, OutputStream outputStream) throws IOException {
        sortedMessage.writeDelimitedTo(outputStream);
    }

    @Override
    public void close() {
    }
}
