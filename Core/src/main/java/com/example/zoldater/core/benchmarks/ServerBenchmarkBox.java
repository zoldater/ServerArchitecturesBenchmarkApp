package com.example.zoldater.core.benchmarks;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServerBenchmarkBox {
    protected final List<Pair<Long, Long>> processingTimes;
    protected long tmpProcessingStart;
    protected final List<Pair<Long, Long>> sortingTimes;
    protected long tmpSortingStart;

    public ServerBenchmarkBox() {
        this.processingTimes = new ArrayList<>();
        this.sortingTimes = new ArrayList<>();
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

    public List<Pair<Long, Long>> getProcessingTimes() {
        return processingTimes;
    }

    public List<Pair<Long, Long>> getSortingTimes() {
        return sortingTimes;
    }


}
