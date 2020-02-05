package com.example.zoldater.core;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkBox {
    private final List<Long> clientAvgTimes;
    private long tmpClientStart;
    private final List<Long> processingAvgTimes;
    private long tmpProcessingStart;
    private final List<Long> sortingAvgTimes;
    private long tmpSortingStart;

    public static BenchmarkBox create() {
        return new BenchmarkBox(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    private BenchmarkBox(List<Long> clientAvgTimes, List<Long> processingAvgTimes, List<Long> sortingAvgTimes) {
        this.clientAvgTimes = clientAvgTimes;
        this.processingAvgTimes = processingAvgTimes;
        this.sortingAvgTimes = sortingAvgTimes;
    }

    public List<Long> getClientAvgTimes() {
        return clientAvgTimes;
    }

    public List<Long> getProcessingAvgTimes() {
        return processingAvgTimes;
    }

    public List<Long> getSortingAvgTimes() {
        return sortingAvgTimes;
    }

    public void startClientSession() {
        tmpClientStart = System.currentTimeMillis();
    }

    public void finishClientSession() {
        clientAvgTimes.add(System.currentTimeMillis() - tmpClientStart);
    }

    public void startProcessing() {
        tmpProcessingStart = System.currentTimeMillis();
    }

    public void finishProcessing() {
        processingAvgTimes.add(System.currentTimeMillis() - tmpProcessingStart);
    }

    public void startSorting() {
        tmpSortingStart = System.currentTimeMillis();
    }

    public void finishSorting() {
        sortingAvgTimes.add(System.currentTimeMillis() - tmpSortingStart);
    }


}
