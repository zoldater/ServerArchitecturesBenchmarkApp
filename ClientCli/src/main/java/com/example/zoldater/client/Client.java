package com.example.zoldater.client;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.benchmarks.ClientBenchmarkBox;
import com.example.zoldater.core.configuration.SingleIterationConfiguration;
import com.example.zoldater.core.enums.PortConstantEnum;
import com.example.zoldater.core.exception.UnexpectedResponseException;
import com.google.common.collect.Ordering;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class Client implements Runnable {
    private Socket socket;
    private InputStream is;
    private OutputStream os;
    List<ClientBenchmarkBox> clientBenchmarkBoxes = new ArrayList<>();
    private long finishTime;
    private final CountDownLatch startLatch;

    protected final SingleIterationConfiguration configuration;

    public Client(CountDownLatch startLatch, SingleIterationConfiguration configuration) {
        this.startLatch = startLatch;
        this.configuration = configuration;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(configuration.getServerAddress(), PortConstantEnum.SERVER_PROCESSING_PORT.getPort());
            is = socket.getInputStream();
            os = socket.getOutputStream();
            startLatch.countDown();
            startLatch.await();
            for (int j = 0; j < configuration.getRequestsPerClient().getValue(); j++) {
                ClientBenchmarkBox clientBenchmarkBox = new ClientBenchmarkBox();
                clientBenchmarkBoxes.add(clientBenchmarkBox);
                clientBenchmarkBox.startClientIteration();
                process();
                clientBenchmarkBox.finishClientIteration();
                Thread.sleep(configuration.getDeltaMs().getValue());
            }
            finishTime = System.currentTimeMillis();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void process() throws IOException {
        SortingMessage msg = generateMessage();
        Utils.writeToStream(msg, os);
        SortingMessage receivedMessage = Utils.readSortingMessage(is);
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


    public long getFinishTime() {
        return finishTime;
    }
}
