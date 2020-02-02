package com.example.zoldater;

import org.tinylog.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Simulation {
    public static void main(String[] args) throws InterruptedException {
        Logger.info("Simulation started!");
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(() -> ServerApplication.main(new String[] {}));
        Thread.sleep(1000);
        executorService.submit(() -> ClientCliApplication.main(new String[] {}));
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            Thread.sleep(100);
        }
        Logger.info("Simulation finished!");
    }
}
