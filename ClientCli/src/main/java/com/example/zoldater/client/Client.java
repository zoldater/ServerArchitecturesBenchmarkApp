package com.example.zoldater.client;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.configuration.SingleIterationConfiguration;
import com.example.zoldater.core.enums.PortConstantEnum;
import com.example.zoldater.core.exception.UnexpectedResponseException;
import com.google.common.collect.Ordering;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.*;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
            for (int j = 0; j < configuration.getRequestsPerClient().getValue(); j++) {
                process();
            }
        } catch (IOException | InterruptedException e) {
            Logger.debug("Client thread #" + Thread.currentThread().getId() + " is returned!");
            return;
        }
    }

    protected void process() throws IOException, InterruptedException {
        SortingMessage msg = generateMessage();
        Utils.writeToStream(msg, os);
        SortingMessage receivedMessage = Utils.readSortingMessage(is);
        Thread.sleep(configuration.getDeltaMs().getValue());
        if (receivedMessage == null) {
            throw new UnexpectedResponseException("Received sortingMessage is null!");
        }
        List<Integer> list = receivedMessage.getElementsList();
        boolean ordered = Ordering.natural().isOrdered(list);
        if (!ordered) {
            throw new UnexpectedResponseException("Received sortingMessage is not sorted!");
        }
    }

    protected SortingMessage generateMessage() {
        return SortingMessage.newBuilder()
                .addAllElements(
                        new Random().ints()
                                .boxed()
                                .limit(configuration.getArrayElements().getValue())
                                .collect(Collectors.toList())
                )
                .build();
    }

    protected void shutdown() {
        Utils.closeResources(socket, is, os);
    }


}
