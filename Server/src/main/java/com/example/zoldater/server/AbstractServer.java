package com.example.zoldater.server;

import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractServer implements Runnable {
    protected final int requestsPerClient;
    protected long clientTime;
    protected final long[] processingTimes;
    protected final long[] sortingTimes;


    protected AbstractServer(int requestsPerClient) {
        this.requestsPerClient = requestsPerClient;
        processingTimes = new long[requestsPerClient];
        sortingTimes = new long[requestsPerClient];
    }

    public long[] getProcessingTimes() {
        return processingTimes;
    }

    public long[] getSortingTimes() {
        return sortingTimes;
    }

    protected static SortingMessage handleSortingMessage(@Nullable SortingMessage message) {
        Logger.info("Start to handle SortingMessage!");
        if (message != null) {
            int[] arr = message.getElementsList().stream().mapToInt(Integer::intValue).toArray();
            bubbleSort(arr);
            SortingMessage.Builder builder = SortingMessage.newBuilder();
            List<Integer> sortedElements = Arrays.stream(arr).boxed().collect(Collectors.toList());
            builder.addAllElements(sortedElements);
            return builder.build();
        }
        return null;
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


}
