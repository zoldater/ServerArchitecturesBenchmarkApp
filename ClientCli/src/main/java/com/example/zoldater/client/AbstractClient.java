package com.example.zoldater.client;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.configuration.SingleIterationConfiguration;
import com.example.zoldater.core.enums.PortConstantEnum;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.stream.Collectors;

public abstract class AbstractClient implements Runnable {
    private final SingleIterationConfiguration configuration;

    public AbstractClient(SingleIterationConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void run() {
        Socket socket;
        InputStream is = null;
        OutputStream os = null;
        try {
            socket = new Socket(configuration.getServerAddress(), PortConstantEnum.SERVER_PROCESSING_PORT.getPort());
            is = socket.getInputStream();
            os = socket.getOutputStream();
            startScheduling(is, os);
        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        }
    }

    private void startScheduling(@NotNull InputStream is,
                                 @NotNull OutputStream os) throws IOException {

        for (int j = 0; j < configuration.getRequestsPerClient().getValue(); j++) {
            process(is, os);
            try {
                Thread.sleep(configuration.getDeltaMs().getValue());
            } catch (InterruptedException e) {
                Logger.error(e);
                throw new RuntimeException(e);
            }
        }
    }

    protected abstract void process(InputStream is, OutputStream os) throws IOException;

    protected SortingMessage generateMessage() {
        SortingMessage.Builder builder1 = SortingMessage.newBuilder();
        builder1.addAllElements(new Random().ints()
                .boxed()
                .limit(configuration.getArrayElements().getValue())
                .collect(Collectors.toList()));
        return builder1.build();
    }


}
