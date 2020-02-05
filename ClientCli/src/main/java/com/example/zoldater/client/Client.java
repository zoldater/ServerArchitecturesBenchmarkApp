package com.example.zoldater.client;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.configuration.SingleIterationConfiguration;
import com.example.zoldater.core.enums.PortConstantEnum;
import com.google.common.collect.Ordering;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Client implements Runnable {
    private Socket socket;
    private InputStream is;
    private OutputStream os;

    protected final SingleIterationConfiguration configuration;

    public Client(SingleIterationConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(configuration.getServerAddress(), PortConstantEnum.SERVER_PROCESSING_PORT.getPort());
            is = socket.getInputStream();
            os = socket.getOutputStream();
            startScheduling();
        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        } finally {
            Utils.closeResources(socket, is, os);
        }
    }

    private void startScheduling() throws IOException {

        for (int j = 0; j < configuration.getRequestsPerClient().getValue(); j++) {
            process();
            try {
                Thread.sleep(configuration.getDeltaMs().getValue());
            } catch (InterruptedException e) {
                Logger.error(e);
                throw new RuntimeException(e);
            }
        }
    }

    protected void process() throws IOException {
        SortingMessage msg = generateMessage();
        Utils.writeToStream(msg, os);
        SortingMessage receivedMessage = Utils.readSortingMessage(is);
        List<Integer> list = receivedMessage != null ? receivedMessage.getElementsList() : null;
        boolean ordered = Ordering.natural().isOrdered(list);
        if (!ordered) {
            Logger.error("Response message not sorted!");
        }
    }

    protected SortingMessage generateMessage() {
        SortingMessage.Builder builder1 = SortingMessage.newBuilder();
        builder1.addAllElements(new Random().ints()
                .boxed()
                .limit(configuration.getArrayElements().getValue())
                .collect(Collectors.toList()));
        return builder1.build();
    }


}
