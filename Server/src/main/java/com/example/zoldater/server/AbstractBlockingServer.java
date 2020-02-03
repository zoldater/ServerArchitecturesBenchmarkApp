package com.example.zoldater.server;

import com.example.zoldater.core.Utils;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public abstract class AbstractBlockingServer extends AbstractServer {
    private final Socket socket;

    protected AbstractBlockingServer(int requestsPerClient, Socket socket) {
        super(requestsPerClient);
        this.socket = socket;
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            long firstMetricsStart = System.currentTimeMillis();
            for (int i = 0; i < requestsPerClient; i++) {
                while (inputStream.available() == 0) {
                    Thread.sleep(10);
                }
                long secondMetricsStart = System.currentTimeMillis();
                SortingMessage message = SortingMessage.parseDelimitedFrom(inputStream);
                long thirdMetricsStart = System.currentTimeMillis();
                SortingMessage sortedMessage = handleSortingMessage(message);
                sortingTimes[i] = System.currentTimeMillis() - thirdMetricsStart;
                sendMessage(sortedMessage, outputStream);
                processingTimes[i] = System.currentTimeMillis() - secondMetricsStart;
            }
            clientTime = System.currentTimeMillis() - firstMetricsStart;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public abstract void sendMessage(SortingMessage sortedMessage, OutputStream outputStream) throws IOException;
}
