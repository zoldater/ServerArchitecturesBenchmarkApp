package com.example.zoldater.client;

import com.example.zoldater.client.worker.InitialClientWorker;
import com.example.zoldater.core.Utils;
import com.example.zoldater.core.configuration.InitialConfiguration;
import com.example.zoldater.core.enums.PortConstantEnum;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureResponse;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

public class ClientMaster {
    private final InitialConfiguration initialConfiguration;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();


    public ClientMaster(InitialConfiguration initialConfiguration) {
        this.initialConfiguration = initialConfiguration;
    }

    public void start() {
        Logger.info("ClientMaster starts!");
        Socket socket = null;
        try {
            socket = new Socket(initialConfiguration.getServerAddress(), PortConstantEnum.SERVER_CONFIGURATION_PORT.getPort());
            InitialClientWorker initialClientWorker = new InitialClientWorker(initialConfiguration.getArchitectureType(), socket);
            Future<?> future = executorService.submit(initialClientWorker);
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                Logger.error(e);
                throw new RuntimeException(e);
            }
            ArchitectureResponse response = initialClientWorker.getResponse();
        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        } finally {
            executorService.shutdownNow();
            Utils.closeResources(socket, null, null);
        }

    }

    public InitialConfiguration getInitialConfiguration() {
        return initialConfiguration;
    }
}
