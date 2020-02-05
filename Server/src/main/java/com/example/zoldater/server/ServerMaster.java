package com.example.zoldater.server;

import com.example.zoldater.core.BenchmarkBox;
import com.example.zoldater.core.Utils;
import com.example.zoldater.core.enums.PortConstantEnum;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos;
import ru.spbau.mit.core.proto.ResultsProtos;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ServerMaster {
    private AbstractServer server;


    public void start() {
        ServerSocket serverSocket = null;
        Socket socket = null;
        InputStream is = null;
        OutputStream os = null;
        try {
            Semaphore semaphoreSending = new Semaphore(1);
            semaphoreSending.acquire();
            serverSocket = new ServerSocket(PortConstantEnum.SERVER_CONFIGURATION_PORT.getPort());
            socket = serverSocket.accept();
            is = socket.getInputStream();
            os = socket.getOutputStream();
            ConfigurationProtos.ArchitectureRequest architectureRequest = Utils.readArchitectureRequest(is);
            int architectureCode = 0;
            if (architectureRequest != null) {
                architectureCode = architectureRequest.getArchitectureCode();
            }
            List<BenchmarkBox> benchmarkBoxes = new ArrayList<>();

            switch (architectureCode) {
                case 1:
                    server = new BlockingServerThread(benchmarkBoxes, semaphoreSending);
                    break;
                case 2:
                    server = new BlockingServerPool(benchmarkBoxes, semaphoreSending);
                    break;
                case 3:
                    server = new NonBlockingServer(benchmarkBoxes, semaphoreSending);
                    break;
                default:
                    throw new RuntimeException("Bad architecture code received from client: " + architectureCode);
            }

            Thread serverThread = new Thread(server);
            serverThread.start();
            semaphoreSending.acquire();
            ConfigurationProtos.ArchitectureResponse response = ConfigurationProtos.ArchitectureResponse.newBuilder()
                    .setConnectionPort(PortConstantEnum.SERVER_PROCESSING_PORT.getPort())
                    .build();
            Utils.writeToStream(response, os);

            ResultsProtos.Request request = Utils.readResultsRequest(is);
            server.shutdown();
            serverThread.interrupt();
            serverThread.join();

            List<Long> clientTimes = new ArrayList<>();
            List<Long> processingTimes = new ArrayList<>();
            List<Long> sortingTimes = new ArrayList<>();
            benchmarkBoxes.forEach(box -> clientTimes.add(box.getClientAvgTimes().get(0)));
            benchmarkBoxes.forEach(box -> processingTimes.add((long) box.getProcessingAvgTimes().stream().mapToLong(it -> it).average().orElse(0)));
            benchmarkBoxes.forEach(box -> sortingTimes.add((long) box.getSortingAvgTimes().stream().mapToLong(it -> it).average().orElse(0)));
            ResultsProtos.Response resultsResponse = ResultsProtos.Response.newBuilder()
                    .addAllClientTimes(clientTimes)
                    .addAllProcessingTimes(processingTimes)
                    .addAllSortingTimes(sortingTimes)
                    .build();
            Utils.writeToStream(resultsResponse, os);
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
