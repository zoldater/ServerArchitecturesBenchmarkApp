package com.example.zoldater.core.benchmarks;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class ClientBenchmarkBox {
    protected final List<Pair<Long, Long>> clientIterationTimes;
    protected long tmpClientIterationStart;

    public ClientBenchmarkBox() {
        this.clientIterationTimes = new ArrayList<>();
    }

    public List<Pair<Long, Long>> getClientIterationTimes() {
        return clientIterationTimes;
    }


    public void startClientIteration() {
        tmpClientIterationStart = System.currentTimeMillis();
    }

    public void finishClientIteration() {
        long currTime = System.currentTimeMillis();
        clientIterationTimes.add(Pair.of(currTime, currTime - tmpClientIterationStart));
    }

}
