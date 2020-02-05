package com.example.zoldater.server;

import com.example.zoldater.core.BenchmarkBox;
import com.example.zoldater.core.enums.PortConstantEnum;
import org.jetbrains.annotations.Nullable;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public abstract class AbstractServer implements Runnable {
    protected final int port;
    protected ServerSocket serverSocket;
    protected final List<BenchmarkBox> benchmarkBoxes;
    protected final Semaphore semaphoreSending;
    protected final List<Thread> clientThreads = new ArrayList<>();

    protected AbstractServer(List<BenchmarkBox> benchmarkBoxes, Semaphore semaphoreSending) {
        this.semaphoreSending = semaphoreSending;
        this.port = PortConstantEnum.SERVER_PROCESSING_PORT.getPort();
        this.benchmarkBoxes = benchmarkBoxes;
    }

    protected static SortingMessage processSortingMessage(@Nullable SortingMessage message) {
        if (message == null) {
            return null;
        }
        int[] arr = message.getElementsList().stream().mapToInt(Integer::intValue).toArray();
        bubbleSort(arr);
        SortingMessage.Builder builder = SortingMessage.newBuilder();
        List<Integer> sortedElements = Arrays.stream(arr).boxed().collect(Collectors.toList());
        builder.addAllElements(sortedElements);
        return builder.build();
    }

    private static void bubbleSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n; i++) {
            for (int j = 1; j < (n - i); j++) {
                if (arr[j - 1] > arr[j]) {
                    int tmp = arr[j - 1];
                    arr[j - 1] = arr[j];
                    arr[j] = tmp;
                }
            }
        }
    }

    public abstract void shutdown();

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

}
