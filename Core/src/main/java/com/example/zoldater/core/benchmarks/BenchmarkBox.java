package com.example.zoldater.core.benchmarks;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BenchmarkBox {
    protected final List<Pair<Long, Long>> clientTimes;
    protected long tmpClientStart;
    protected long firstClientFinishTime = -1;

    protected long lastClientStartTime = -1;
    protected final List<Pair<Long, Long>> processingTimes;
    protected long tmpProcessingStart;
    protected final List<Pair<Long, Long>> sortingTimes;
    protected long tmpSortingStart;

    public static BenchmarkBox create() {
        return new BenchmarkBox(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    private BenchmarkBox(List<Pair<Long, Long>> clientTimes, List<Pair<Long, Long>> processingTimes, List<Pair<Long, Long>> sortingTimes) {
        this.clientTimes = clientTimes;
        this.processingTimes = processingTimes;
        this.sortingTimes = sortingTimes;
    }

    public void startClientSession() {
        lastClientStartTime = System.currentTimeMillis();
        tmpClientStart = lastClientStartTime;
    }

    public void finishClientSession() {
        long currTime = System.currentTimeMillis();
        clientTimes.add(Pair.of(currTime, currTime - tmpClientStart));
        if (firstClientFinishTime == -1) firstClientFinishTime = currTime;

    }

    public void startProcessing() {
        tmpProcessingStart = System.currentTimeMillis();
    }

    public void finishProcessing() {
        long currTime = System.currentTimeMillis();
        processingTimes.add(Pair.of(currTime, currTime - tmpProcessingStart));
    }

    public void startSorting() {
        tmpSortingStart = System.currentTimeMillis();
    }

    public void finishSorting() {
        long currTime = System.currentTimeMillis();
        sortingTimes.add(Pair.of(currTime, currTime - tmpSortingStart));
    }
}
