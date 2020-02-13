package com.example.zoldater.server;

import com.example.zoldater.core.configuration.SingleIterationConfiguration;
import ru.spbau.mit.core.proto.ConfigurationProtos;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class ServerArchitectureFactory {
    public static AbstractServer generate(ConfigurationProtos.ConfigurationRequest configurationRequest, Semaphore semaphoreSending) {
        final int architectureCode = configurationRequest.getArchitectureCode();
        final int clientsCount = configurationRequest.getClientsCount();
        final int requestsPerClient = configurationRequest.getRequestsPerClient();

        CountDownLatch resultsSendingLatch = new CountDownLatch(clientsCount);
        AbstractServer server;
        switch (architectureCode) {
            case 1:
                server = new BlockingServerThread(semaphoreSending, resultsSendingLatch, clientsCount, requestsPerClient);
                break;
            case 2:
                server = new BlockingServerPool(semaphoreSending, resultsSendingLatch, clientsCount, requestsPerClient);
                break;
            case 3:
                server = new NonBlockingServer(semaphoreSending, resultsSendingLatch, clientsCount, requestsPerClient);
                break;
            default:
                throw new RuntimeException("Bad architecture code received from client: " + architectureCode);
        }
        return server;
    }
}
