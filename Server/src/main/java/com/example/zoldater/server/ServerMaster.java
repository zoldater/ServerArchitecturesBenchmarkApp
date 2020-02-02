package com.example.zoldater.server;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.enums.ArchitectureTypeEnum;
import com.example.zoldater.core.enums.PortConstantEnum;
import com.example.zoldater.server.worker.InitialServerWorker;
import com.example.zoldater.server.worker.IterationCloseServerWorker;
import com.example.zoldater.server.worker.IterationOpenServerWorker;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos;
import ru.spbau.mit.core.proto.IterationProtos.IterationCloseRequest;
import ru.spbau.mit.core.proto.IterationProtos.IterationOpenRequest;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ServerMaster {
    private final ExecutorService configurationService = Executors.newSingleThreadExecutor();
    private final ExecutorService dataProcessService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


    public void start() {
        ServerSocket serverSocket = null;
        Socket socket = null;
        try {
            serverSocket = new ServerSocket(PortConstantEnum.SERVER_CONFIGURATION_PORT.getPort());
            socket = serverSocket.accept();
            InitialServerWorker initialServerWorker = new InitialServerWorker(socket);
            Future<?> future = configurationService.submit(initialServerWorker);
            future.get();
            ConfigurationProtos.ArchitectureRequest request = initialServerWorker.getRequest();
            int architectureCode = request.getArchitectureCode();
            int iterationsNumber = request.getIterationsNumber();
            Socket finalSocket = socket;
            for (int it = 0; it < iterationsNumber && !socket.isClosed(); it++) {
                Logger.info("Iteration #" + it + " begins!");
                IterationOpenServerWorker iterationOpenServerWorker = new IterationOpenServerWorker(finalSocket);
                iterationOpenServerWorker.run();
                IterationOpenRequest openRequest = iterationOpenServerWorker.getRequest();
                Logger.info("openRequest received!");
                List<AbstractServer> serverList = new ArrayList<>();

                if (architectureCode == ArchitectureTypeEnum.NON_BLOCKING_ARCH.code) {
                    serverList.addAll(IntStream.range(0, openRequest.getClientsNumber())
                            .mapToObj(it1 -> new NonBlockingServer(openRequest.getRequestPerClient()))
                            .collect(Collectors.toList()));
                    serverList.forEach(dataProcessService::submit);
                    dataProcessService.shutdown();
                    while (!dataProcessService.isTerminated()) {
                        Thread.sleep(10);
                    }
                } else {
                    ServerSocket processingServerSocket = null;
                    try {
                        processingServerSocket = new ServerSocket(PortConstantEnum.SERVER_PROCESSING_PORT.getPort());
                        for (int i = 0; i < openRequest.getClientsNumber(); i++) {
                            Socket processingSocket = processingServerSocket.accept();
                            Logger.info("Connected client #" + i);
                            serverList.add(architectureCode == ArchitectureTypeEnum.ONLY_THREADS_ARCH.code
                                    ? new BlockingServerDirectSending(openRequest.getRequestPerClient(), processingSocket)
                                    : new BlockingServerPoolSending(openRequest.getRequestPerClient(), processingSocket));
                        }
                        if (architectureCode == ArchitectureTypeEnum.ONLY_THREADS_ARCH.code) {
                            List<Thread> threads = serverList.stream().map(Thread::new).collect(Collectors.toList());
                            threads.forEach(Thread::start);
                            for (Thread thread : threads) {
                                try {
                                    thread.join();
                                } catch (InterruptedException e) {
                                    Logger.error(e);
                                    throw new RuntimeException(e);
                                }
                            }
                            Logger.info("Clients finished!");
                        } else {
                            serverList.forEach(dataProcessService::submit);
                            dataProcessService.shutdown();
                            while (!dataProcessService.isTerminated()) {
                                Thread.sleep(10);
                            }
                        }
                    } catch (IOException e) {
                        Logger.error(e);
                        throw new RuntimeException(e);
                    } finally {
                        if (processingServerSocket != null) {
                            try {
                                processingServerSocket.close();
                            } catch (IOException e) {
                                Logger.error(e);
                            }
                        }

                    }
                }
                long averageClientTime = (long) serverList.stream()
                        .mapToLong(srv -> srv.clientTime)
                        .average().orElse(0) / openRequest.getRequestPerClient();
                long averageProcessingTime = (long) serverList.stream()
                        .mapToLong(AbstractServer::getProcessingTimes)
                        .average().orElse(0);
                long averageSortingTime = (long) serverList.stream()
                        .mapToLong(AbstractServer::getSortingTimes)
                        .average().orElse(0);

                IterationCloseServerWorker iterationCloseClientWorker =
                        new IterationCloseServerWorker(finalSocket, averageClientTime, averageProcessingTime, averageSortingTime);
                iterationCloseClientWorker.run();
                IterationCloseRequest closeRequest = iterationCloseClientWorker.getRequest();
                Logger.info("Iteration #" + it + " ends!");
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        } finally {
            configurationService.shutdownNow();
            dataProcessService.shutdownNow();
            Utils.closeResources(socket, null, null);
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    Logger.error(e);
                }
            }
        }
    }
}
