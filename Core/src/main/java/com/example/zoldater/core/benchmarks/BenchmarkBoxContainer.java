package com.example.zoldater.core.benchmarks;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BenchmarkBoxContainer {
    private final List<BenchmarkBox> benchmarkBoxList = new ArrayList<>();
    private long latestStartClientSessionTime = -1;
    private long earliestFinishClientSessionTime = -1;
    private boolean isUpToDate = false;

    public void add(BenchmarkBox benchmarkBox) {
        benchmarkBoxList.add(benchmarkBox);
    }

    public double getAverageClientTime() {
        if (!isUpToDate) update();
        return benchmarkBoxList.stream()
                .flatMap(it -> it.clientTimes.stream())
                .map(Pair::getRight)
                .mapToLong(it -> it)
                .average().orElse(0);
    }

    public double getAverageProcessingTime() {
        if (!isUpToDate) update();
        return benchmarkBoxList.stream()
                .flatMap(it -> it.processingTimes.stream())
                .filter(it -> it.getLeft() >= latestStartClientSessionTime && it.getLeft() <= earliestFinishClientSessionTime)
                .map(Pair::getRight)
                .mapToLong(it -> it)
                .average().orElse(0);
    }

    public double getAverageSortingTime() {
        if (!isUpToDate) update();
        return benchmarkBoxList.stream()
                .flatMap(it -> it.sortingTimes.stream())
                .filter(it -> it.getLeft() >= latestStartClientSessionTime && it.getLeft() <= earliestFinishClientSessionTime)
                .map(Pair::getRight)
                .mapToLong(it -> it)
                .average().orElse(0);
    }

    private void updateLatestStartClientSessionTime() {
        latestStartClientSessionTime = benchmarkBoxList.stream()
                .map(it -> it.lastClientStartTime)
                .max(Comparator.naturalOrder())
                .get();

    }

    private void updateEarliestFinishClientSessionTime() {
        earliestFinishClientSessionTime = benchmarkBoxList.stream()
                .map(it -> it.firstClientFinishTime)
                .min(Comparator.naturalOrder())
                .get();
    }

    private void update() {
        updateEarliestFinishClientSessionTime();
        updateLatestStartClientSessionTime();
        isUpToDate = true;
    }

}
