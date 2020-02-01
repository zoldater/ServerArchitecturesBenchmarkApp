package com.example.zoldater.server;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.enums.PortConstantEnum;
import com.example.zoldater.server.worker.InitialServerWorker;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class ServerMaster {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void start() {
        Logger.info("ServerMaster starts!");
        ServerSocket serverSocket = null;
        Socket socket = null;
        try {
            serverSocket = new ServerSocket(PortConstantEnum.SERVER_CONFIGURATION_PORT.getPort());
            socket = serverSocket.accept();
            InitialServerWorker initialServerWorker = new InitialServerWorker(socket);
            Future<?> future = executorService.submit(initialServerWorker);
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                Logger.error(e);
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        } finally {
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
