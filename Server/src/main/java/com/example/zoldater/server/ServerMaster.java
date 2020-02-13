package com.example.zoldater.server;

import com.example.zoldater.core.BenchmarkBox;
import com.example.zoldater.core.Utils;
import com.example.zoldater.core.enums.PortConstantEnum;
import com.example.zoldater.core.exception.UnexpectedResponseException;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos;
import ru.spbau.mit.core.proto.ConfigurationProtos.ConfigurationResponse;
import ru.spbau.mit.core.proto.ResultsProtos.IterationResultsMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ServerMaster {
    private AbstractServer server;
    private Thread serverThread;
    private final Object shutdownLock = new Object();


    public void start() {
        ServerSocket serverSocket = null;
        Socket socket = null;
        InputStream is = null;
        OutputStream os = null;
        while (true) {
            try {
                Semaphore semaphoreSending = new Semaphore(1);
                semaphoreSending.acquire();
                serverSocket = new ServerSocket(PortConstantEnum.SERVER_CONFIGURATION_PORT.getPort());
                socket = serverSocket.accept();
                is = socket.getInputStream();
                os = socket.getOutputStream();
                ConfigurationProtos.ConfigurationRequest configurationRequest = Utils.readConfigurationRequest(is);
                if (configurationRequest == null) {
                    throw new UnexpectedResponseException("Configuration request from client is null!");
                }
                server = ServerArchitectureFactory.generate(configurationRequest, semaphoreSending);
                serverThread = new Thread(server);
                serverThread.start();
                semaphoreSending.acquire();
                ConfigurationResponse response = ConfigurationResponse.newBuilder()
                        .setIsSuccessful(true)
                        .build();
                Utils.writeToStream(response, os);

                server.resultsSendingLatch.await();

                server.shutdown();
                serverThread.interrupt();
                final List<BenchmarkBox> benchmarkBoxes = server.getBenchmarkBoxes();
                server = null;
                serverThread = null;

                final double avgClientTime = benchmarkBoxes.stream()
                        .map(BenchmarkBox::getClientTimes)
                        .flatMap(Collection::stream)
                        .mapToLong(it -> it)
                        .average()
                        .orElse(0);
                final double avgProcessingTime = benchmarkBoxes.stream()
                        .map(BenchmarkBox::getProcessingTimes)
                        .flatMap(Collection::stream)
                        .mapToLong(it -> it)
                        .average()
                        .orElse(0);
                final double avgSortingTime = benchmarkBoxes.stream()
                        .map(BenchmarkBox::getSortingTimes)
                        .flatMap(Collection::stream)
                        .mapToLong(it -> it)
                        .average()
                        .orElse(0);

                IterationResultsMessage resultsMessage = IterationResultsMessage.newBuilder()
                        .setAverageClientTime(avgClientTime / configurationRequest.getRequestsPerClient())
                        .setAverageProcessingTime(avgProcessingTime)
                        .setAverageSortingTime(avgSortingTime)
                        .build();
                Utils.writeToStream(resultsMessage, os);
                Logger.info("resultsMessage sent!");

            } catch (IOException | InterruptedException e) {
                Logger.error(e);
                throw new RuntimeException(e);
            } finally {
                Utils.closeResources(socket, is, os);
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
}
