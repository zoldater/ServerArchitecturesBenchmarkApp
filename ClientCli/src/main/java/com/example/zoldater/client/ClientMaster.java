package com.example.zoldater.client;

import com.example.zoldater.client.worker.InitialClientWorker;
import com.example.zoldater.client.worker.IterationCloseClientWorker;
import com.example.zoldater.client.worker.IterationOpenClientWorker;
import com.example.zoldater.core.BenchmarkBox;
import com.example.zoldater.core.Utils;
import com.example.zoldater.core.configuration.InitialConfiguration;
import com.example.zoldater.core.configuration.SingleIterationConfiguration;
import com.example.zoldater.core.configuration.data.VariableArgumentData;
import com.example.zoldater.core.enums.ArgumentTypeEnum;
import com.example.zoldater.core.enums.PortConstantEnum;
import com.opencsv.CSVWriter;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureResponse;
import ru.spbau.mit.core.proto.IterationProtos;
import ru.spbau.mit.core.proto.IterationProtos.IterationCloseResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.zoldater.core.enums.ArchitectureTypeEnum.NON_BLOCKING_ARCH;

public class ClientMaster {
    private final InitialConfiguration initialConfiguration;
    private final ExecutorService configurationConnectionService = Executors.newSingleThreadExecutor();
    private final ExecutorService sortingConnectionService = Executors.newCachedThreadPool();


    public ClientMaster(InitialConfiguration initialConfiguration) {
        this.initialConfiguration = initialConfiguration;
    }


    public InitialConfiguration getInitialConfiguration() {
        return initialConfiguration;
    }

    public void start() {
        Socket socket = null;
        try {
            socket = new Socket(initialConfiguration.getServerAddress(), PortConstantEnum.SERVER_CONFIGURATION_PORT.getPort());
            VariableArgumentData variableArgumentData = initialConfiguration.getVariableArgumentData();
            int iterations = (variableArgumentData.getTo() - variableArgumentData.getFrom()) / variableArgumentData.getStep();
            InitialClientWorker initialClientWorker = new InitialClientWorker(initialConfiguration.getArchitectureType(), iterations, socket);
            Future<?> submit = configurationConnectionService.submit(initialClientWorker);
            submit.get();
            ArchitectureResponse response = initialClientWorker.getResponse();
            List<SingleIterationConfiguration> iterationConfigurations = SingleIterationConfiguration.fromInitialConfiguration(initialConfiguration);
            List<BenchmarkBox> benchmarkBoxes = new ArrayList<>();
            Socket finalSocket = socket;
            iterationConfigurations.forEach(config -> {
                IterationOpenClientWorker iterationOpenClientWorker = new IterationOpenClientWorker(finalSocket, config.getClientsNumber(), config.getRequestsPerClient());
                Future<?> future = configurationConnectionService.submit(iterationOpenClientWorker);
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    Logger.error(e);
                    throw new RuntimeException(e);
                }
                IterationProtos.IterationOpenResponse openResponse = iterationOpenClientWorker.getResponse();

                int clientsNumber = config.getClientsNumber().getValue();
                List<AbstractClient> clients = IntStream.range(0, clientsNumber)
                        .mapToObj(it -> NON_BLOCKING_ARCH.equals(config.getArchitectureType())
                                ? new NonBlockingClient(config)
                                : new BlockingClient(config))
                        .collect(Collectors.toList());
                List<Thread> threads = clients.stream().map(Thread::new).collect(Collectors.toList());
                threads.forEach(Thread::start);
                threads.forEach(it -> {
                    try {
                        it.join();
                    } catch (InterruptedException e) {
                        Logger.error(e);
                        throw new RuntimeException(e);
                    }
                });

                IterationCloseClientWorker iterationCloseClientWorker = new IterationCloseClientWorker(finalSocket);
                future = configurationConnectionService.submit(iterationCloseClientWorker);
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    Logger.error(e);
                    throw new RuntimeException(e);
                }
                IterationCloseResponse closeResponse = iterationCloseClientWorker.getResponse();
                long firstMetric = closeResponse.getAveragePerClientTime();
                long secondMetric = closeResponse.getAverageProcessingTime();
                long thirdMetric = closeResponse.getAverageSortingTime();
                ArgumentTypeEnum varType = variableArgumentData.getArgumentTypeEnum();
                switch (varType) {
                    case ARRAY_ELEMENTS:
                        benchmarkBoxes.add(new BenchmarkBox(config.getArrayElements().getValue(), firstMetric, secondMetric, thirdMetric));
                        break;
                    case CLIENTS_NUMBER:
                        benchmarkBoxes.add(new BenchmarkBox(config.getClientsNumber().getValue(), firstMetric, secondMetric, thirdMetric));
                        break;
                    case DELTA_MS:
                        benchmarkBoxes.add(new BenchmarkBox(config.getDeltaMs().getValue(), firstMetric, secondMetric, thirdMetric));
                        break;
                    default:
                        Logger.warn("Some shit with received results");
                }
            });
            saveResultsToCsvAndImage(benchmarkBoxes, initialConfiguration);

        } catch (IOException | InterruptedException | ExecutionException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        } finally {
            configurationConnectionService.shutdownNow();
            sortingConnectionService.shutdownNow();
            Utils.closeResources(socket, null, null);
        }

    }

    private static void saveResultsToCsvAndImage(List<BenchmarkBox> benchmarkBoxList, InitialConfiguration configuration) throws IOException {
        String fileName = configuration.getVariableArgumentData().getArgumentTypeEnum().getLiteral() +
                "_" +
                configuration.getArchitectureType().toString() +
                "_" +
                configuration.getValueArgumentData1().getArgumentTypeEnum().getLiteral() +
                "=" +
                configuration.getValueArgumentData1().getValue() +
                "_" +
                configuration.getValueArgumentData2().getArgumentTypeEnum().getLiteral() +
                "=" +
                configuration.getValueArgumentData2().getValue() +
                "_" +
                configuration.getRequestsPerClientSession().getArgumentTypeEnum().getLiteral() +
                "=" +
                configuration.getRequestsPerClientSession().getValue();

        File csvResultFile = new File(System.getProperty("user.dir") + "/" + fileName + ".csv");
        CSVWriter writer = new CSVWriter(new FileWriter(csvResultFile));
        writer.writeNext(new String[]{configuration.getVariableArgumentData().getArgumentTypeEnum().getLiteral(), "M1", "M2", "M3"});
        benchmarkBoxList.forEach(box -> writer.writeNext(
                new String[]{
                        Integer.toString(box.getCurrentValue()),
                        Double.toString(box.getAverageSortingTime()),
                        Double.toString(box.getAverageProcessingTime()),
                        Double.toString(box.getAverageTimePerClientSession())
                }));
        writer.flush();
        Logger.debug(MessageFormat.format("Results wrote into file {0}", csvResultFile.getAbsolutePath()));

        String variableArgTypeLiteral = configuration.getVariableArgumentData().getArgumentTypeEnum().getLiteral();

        double[] xData = benchmarkBoxList.stream().mapToDouble(BenchmarkBox::getCurrentValue).toArray();
        double[][] yData = new double[][]{
                benchmarkBoxList.stream().mapToDouble(BenchmarkBox::getAverageSortingTime).toArray(),
                benchmarkBoxList.stream().mapToDouble(BenchmarkBox::getAverageProcessingTime).toArray(),
                benchmarkBoxList.stream().mapToDouble(BenchmarkBox::getAverageTimePerClientSession).toArray()
        };

        XYChart chart = QuickChart.getChart(fileName, "Time, ms", variableArgTypeLiteral,
                new String[]{
                        "Average Sorting Time",
                        "Average Processing Time",
                        "Average Time Per Client"
                }, xData, yData);

        File imageResultFile = new File(fileName + ".png");
        try (FileOutputStream fileOutputStream = new FileOutputStream(imageResultFile)) {
            BitmapEncoder.saveBitmap(chart, fileOutputStream, BitmapEncoder.BitmapFormat.PNG);
        }
        Logger.info(MessageFormat.format("Results wrote into file {0}", imageResultFile.getAbsolutePath()));

    }


}
