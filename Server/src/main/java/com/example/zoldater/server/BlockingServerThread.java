package com.example.zoldater.server;

import com.example.zoldater.core.BenchmarkBox;
import com.example.zoldater.core.Utils;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class BlockingServerThread extends AbstractBlockingServer {


    protected BlockingServerThread(List<BenchmarkBox> benchmarkBoxes, Semaphore semaphoreSending) {
        super(benchmarkBoxes, semaphoreSending);
    }

    @Override
    public SortingProtos.SortingMessage sort(SortingProtos.SortingMessage message) throws ExecutionException, InterruptedException {
        return processSortingMessage(message);
    }

    @Override
    public void send(SortingProtos.SortingMessage message, OutputStream outputStream) throws IOException {
        Utils.writeToStream(message, outputStream);

    }
}
