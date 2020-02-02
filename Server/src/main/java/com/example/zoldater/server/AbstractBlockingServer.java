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
                    Thread.yield();
                }
                long secondMetricsStart = System.currentTimeMillis();
                SortingMessage message = SortingMessage.parseDelimitedFrom(inputStream);
                long thirdMetricsStart = System.currentTimeMillis();
                SortingMessage sortedMessage = handleSortingMessage(message);
                sortingTimes[i] = System.currentTimeMillis() - thirdMetricsStart;
                sendMessage(sortedMessage, outputStream);
                processingTimes[i] = System.currentTimeMillis() - secondMetricsStart;
                Logger.info("Finish handling request #" + i);
            }
            clientTime = System.currentTimeMillis() - firstMetricsStart;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.closeResources(null, inputStream, outputStream);
            close();
        }
    }

    public abstract void sendMessage(SortingMessage sortedMessage, OutputStream outputStream) throws IOException;

    public abstract void close();
}
