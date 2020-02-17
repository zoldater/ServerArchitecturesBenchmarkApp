package com.example.zoldater.server;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.benchmarks.ServerBenchmarkBox;
import com.example.zoldater.core.enums.PortConstantEnum;
import com.example.zoldater.core.exception.UnexpectedResponseException;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos;
import ru.spbau.mit.core.proto.ConfigurationProtos.ConfigurationResponse;
import ru.spbau.mit.core.proto.ResultsProtos.IterationResultsMessage;
import ru.spbau.mit.core.proto.ResultsProtos.ResultsPair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

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
                serverThread.join();
                List<ServerBenchmarkBox> serverBenchmarkBoxes = server.getServerBenchmarkBoxes();
                server = null;
                serverThread = null;

                List<ResultsPair> processingTimeslist = serverBenchmarkBoxes.stream()
                        .map(it -> it.getProcessingTimes())
                        .flatMap(Collection::stream)
                        .map(it -> ResultsPair.newBuilder()
                                .setResultTimestamp(it.getLeft())
                                .setResultsData(it.getRight())
                                .build())
                        .collect(Collectors.toList());

                List<ResultsPair> sortingTimeslist = serverBenchmarkBoxes.stream()
                        .map(it -> it.getSortingTimes())
                        .flatMap(Collection::stream)
                        .map(it -> ResultsPair.newBuilder()
                                .setResultTimestamp(it.getLeft())
                                .setResultsData(it.getRight())
                                .build())
                        .collect(Collectors.toList());
                IterationResultsMessage resultsMessage = IterationResultsMessage.newBuilder()
                        .addAllProcessingTimePairs(processingTimeslist)
                        .addAllSortingTimePairs(sortingTimeslist)
                        .build();
                Utils.writeToStream(resultsMessage, os);

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
