package com.example.zoldater.core;

public class BenchmarkBox {
    private final int currentValue;
    private final long averageTimePerClientSession;
    private final long averageProcessingTime;
    private final long averageSortingTime;

    public BenchmarkBox(int currentValue,
                        long averageTimePerClientSession,
                        long averageProcessingTime,
                        long averageSortingTime) {
        this.currentValue = currentValue;
        this.averageTimePerClientSession = averageTimePerClientSession;
        this.averageProcessingTime = averageProcessingTime;
        this.averageSortingTime = averageSortingTime;
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public long getAverageTimePerClientSession() {
        return averageTimePerClientSession;
    }

    public long getAverageProcessingTime() {
        return averageProcessingTime;
    }

    public long getAverageSortingTime() {
        return averageSortingTime;
    }
}
