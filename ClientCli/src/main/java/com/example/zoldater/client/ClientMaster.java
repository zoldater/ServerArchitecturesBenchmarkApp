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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.zoldater.core.enums.ArchitectureTypeEnum.NON_BLOCKING_ARCH;

public class ClientMaster {
    private final InitialConfiguration initialConfiguration;
    private final ExecutorService sortingConnectionService = Executors.newCachedThreadPool();


    public ClientMaster(InitialConfiguration initialConfiguration) {
        this.initialConfiguration = initialConfiguration;
    }


    public InitialConfiguration getInitialConfiguration() {
        return initialConfiguration;
    }

    private static void saveResultsToCsvAndImage(List<BenchmarkBox> benchmarkBoxList, InitialConfiguration configuration) throws IOException {
        String directory = System.getProperty("user.dir") + "/" +
                configuration.getVariableArgumentData().getArgumentTypeEnum().getLiteral() +
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

        final File directoryFile = new File(directory);
        directoryFile.mkdirs();
        File csvResultFile = new File(directoryFile, "results.csv");
        CSVWriter writer = new CSVWriter(new FileWriter(csvResultFile));
        writer.writeNext(new String[]{configuration.getVariableArgumentData().getArgumentTypeEnum().getLiteral(), "M1", "M2", "M3"});
        benchmarkBoxList.forEach(box -> writer.writeNext(
                new String[]{
                        Integer.toString(box.getCurrentValue()),
                        Double.toString(box.getAverageTimePerClientSession()),
                        Double.toString(box.getAverageProcessingTime()),
                        Double.toString(box.getAverageSortingTime())
                }));
        writer.flush();

        String variableArgTypeLiteral = configuration.getVariableArgumentData().getArgumentTypeEnum().getLiteral();

        double[] xData = benchmarkBoxList.stream().mapToDouble(BenchmarkBox::getCurrentValue).toArray();

        XYChart chart1 = QuickChart.getChart("Average Sorting Time", variableArgTypeLiteral, "Time, ms", "Average Sorting Time",
                xData, benchmarkBoxList.stream().mapToDouble(BenchmarkBox::getAverageSortingTime).toArray());

        File chart1File = new File(directoryFile, "M3.png");
        try (FileOutputStream fileOutputStream = new FileOutputStream(chart1File)) {
            BitmapEncoder.saveBitmap(chart1, fileOutputStream, BitmapEncoder.BitmapFormat.PNG);
        }

        XYChart chart2 = QuickChart.getChart("Average Processing Time", variableArgTypeLiteral, "Time, ms", "Average Processing Time",
                xData, benchmarkBoxList.stream().mapToDouble(BenchmarkBox::getAverageProcessingTime).toArray());

        File chart2File = new File(directoryFile, "M2.png");
        try (FileOutputStream fileOutputStream = new FileOutputStream(chart2File)) {
            BitmapEncoder.saveBitmap(chart2, fileOutputStream, BitmapEncoder.BitmapFormat.PNG);
        }

        XYChart chart3 = QuickChart.getChart("Average Per Client Time", variableArgTypeLiteral, "Time, ms", "Average Per Client Time",
                xData, benchmarkBoxList.stream().mapToDouble(BenchmarkBox::getAverageTimePerClientSession).toArray());

        File chart3File = new File(directoryFile, "M1.png");
        try (FileOutputStream fileOutputStream = new FileOutputStream(chart3File)) {
            BitmapEncoder.saveBitmap(chart3, fileOutputStream, BitmapEncoder.BitmapFormat.PNG);
        }

    }

    public void start() {
        Socket socket = null;
        try {
            socket = new Socket(initialConfiguration.getServerAddress(), PortConstantEnum.SERVER_CONFIGURATION_PORT.getPort());
            VariableArgumentData variableArgumentData = initialConfiguration.getVariableArgumentData();
            int iterations = (variableArgumentData.getTo() - variableArgumentData.getFrom()) / variableArgumentData.getStep();
            InitialClientWorker initialClientWorker = new InitialClientWorker(initialConfiguration.getArchitectureType(), iterations, socket);
            initialClientWorker.run();
            ArchitectureResponse response = initialClientWorker.getResponse();
            List<SingleIterationConfiguration> iterationConfigurations = SingleIterationConfiguration.fromInitialConfiguration(initialConfiguration);
            List<BenchmarkBox> benchmarkBoxes = new ArrayList<>();
            Socket finalSocket = socket;
            iterationConfigurations.forEach(config -> {
                IterationOpenClientWorker iterationOpenClientWorker = new IterationOpenClientWorker(finalSocket, config.getClientsNumber(), config.getRequestsPerClient());
                iterationOpenClientWorker.run();
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
                iterationCloseClientWorker.run();
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

        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        } finally {
            sortingConnectionService.shutdownNow();
            Utils.closeResources(socket, null, null);
        }

    }


}
