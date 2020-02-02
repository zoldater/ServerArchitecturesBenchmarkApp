package com.example.zoldater.core.configuration;

public final class AverageTime {
    private int statCount = 0;
    private long sumTime = 0;

    public AverageTime() {
    }

    public void addTime(long time) {
        statCount++;
        sumTime += time;
    }

    public long getAverageTime() {
        return statCount == 0 ? -1 : sumTime / statCount;
    }

}

