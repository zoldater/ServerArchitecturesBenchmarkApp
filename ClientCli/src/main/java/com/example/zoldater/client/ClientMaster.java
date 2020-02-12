package com.example.zoldater.client;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.configuration.InitialConfiguration;
import com.example.zoldater.core.configuration.SingleIterationConfiguration;
import com.example.zoldater.core.configuration.data.VariableArgumentData;
import com.example.zoldater.core.exception.InvalidConfigurationException;
import com.opencsv.CSVWriter;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos.ConfigurationResponse;
import ru.spbau.mit.core.proto.ResultsProtos;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.zoldater.core.enums.PortConstantEnum.SERVER_CONFIGURATION_PORT;
import static ru.spbau.mit.core.proto.ConfigurationProtos.ConfigurationRequest;

public class ClientMaster {
    private final InitialConfiguration initialConfiguration;
    private final ExecutorService sortingConnectionService = Executors.newCachedThreadPool();
    private final List<IterationBenchmarkResults> resultsList = new ArrayList<>();

    private final List<XYChart> charts = new ArrayList<>();


    public ClientMaster(InitialConfiguration initialConfiguration) {
        this.initialConfiguration = initialConfiguration;
    }


    public InitialConfiguration getInitialConfiguration() {
        return initialConfiguration;
    }


    public void start() {
        List<SingleIterationConfiguration> iterationConfigurations = SingleIterationConfiguration.fromInitialConfiguration(initialConfiguration);

        iterationConfigurations.forEach(config -> {
            Socket socket = null;
            try {
                socket = new Socket(initialConfiguration.getServerAddress(), SERVER_CONFIGURATION_PORT.getPort());
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();
                Logger.info("Started new config!");

                final ConfigurationRequest configurationRequest = ConfigurationRequest.newBuilder()
                        .setArchitectureCode(config.getArchitectureType().code)
                        .setClientsCount(config.getClientsNumber().getValue())
                        .setRequestsPerClient(config.getRequestsPerClient().getValue())
                        .build();
                Utils.writeToStream(configurationRequest, outputStream);
                final ConfigurationResponse configurationResponse = Utils.readConfigurationResponse(inputStream);
                if (configurationResponse == null || !configurationResponse.getIsSuccessful()) {
                    throw new RuntimeException("Bad response on configuration request!");
                }
                int clientsNumber = config.getClientsNumber().getValue();
                List<Client> clients = IntStream.range(0, clientsNumber)
                        .mapToObj(it -> new Client(config))
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

                final ResultsProtos.IterationResultsMessage resultsResponse = Utils.readResults(inputStream);
                Logger.info("results received!");
                if (resultsResponse == null) {
                    throw new RuntimeException("Bad response on results request!");
                }
                int variableValue = 0;
                switch (initialConfiguration.getVariableArgumentData().getArgumentTypeEnum()) {
                    case ARRAY_ELEMENTS:
                        variableValue = config.getArrayElements().getValue();
                        break;
                    case CLIENTS_NUMBER:
                        variableValue = config.getClientsNumber().getValue();
                        break;
                    case DELTA_MS:
                        variableValue = config.getDeltaMs().getValue();
                        break;
                    default:
                        throw new InvalidConfigurationException("Unexpected variable type - " +
                                initialConfiguration.getVariableArgumentData().getArgumentTypeEnum());
                }
                resultsList.add(new IterationBenchmarkResults(variableValue,
                        resultsResponse.getAverageClientTime(), resultsResponse.getAverageProcessingTime(), resultsResponse.getAverageSortingTime()));

            saveResultsToCsvAndImage(resultsList, initialConfiguration);

            } catch (IOException e) {
                Logger.error(e);
                throw new RuntimeException(e);
            } finally {
                sortingConnectionService.shutdownNow();
                Utils.closeResources(socket, null, null);
            }
        });

    }


    private void saveResultsToCsvAndImage(List<IterationBenchmarkResults> resultsList, InitialConfiguration configuration) throws IOException {
        String fileName = System.getProperty("user.dir") + "/Statistics/" +
                "A=" +
                configuration.getArchitectureType().code +
                "_" +
                configuration.getVariableArgumentData().getArgumentTypeEnum().getLiteral() +
                "-" +
                configuration.getVariableArgumentData().getFrom() +
                "-" +
                configuration.getVariableArgumentData().getTo() +
                "-" +
                configuration.getVariableArgumentData().getStep() +
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

        final File directoryFile = new File(fileName);
        directoryFile.mkdirs();
        File csvResultFile = new File(directoryFile, "results.csv");
        CSVWriter writer = new CSVWriter(new FileWriter(csvResultFile));
        writer.writeNext(new String[]{configuration.getVariableArgumentData().getArgumentTypeEnum().getLiteral(), "M1", "M2", "M3"});
        VariableArgumentData variableArgumentData = configuration.getVariableArgumentData();

        int[] values = resultsList.stream().map(it -> it.variableValue).mapToInt(it -> it).toArray();
        double[] averageClientTimes = resultsList.stream().map(it -> it.averageClientTime).mapToDouble(it -> it).toArray();
        double[] averageProcessingTimes = resultsList.stream().map(it -> it.averageProcessingTime).mapToDouble(it -> it).toArray();
        double[] averageSortingTimes = resultsList.stream().map(it -> it.averageSortingTime).mapToDouble(it -> it).toArray();

        for (int i = 0; i < values.length; i++) {
            writer.writeNext(
                    new String[]{
                            Integer.toString(values[i]),
                            Double.toString(averageClientTimes[i]),
                            Double.toString(averageProcessingTimes[i]),
                            Double.toString(averageSortingTimes[i])
                    });
        }
        writer.flush();

        String variableArgTypeLiteral = variableArgumentData.getArgumentTypeEnum().getLiteral();
        double[] xData = Arrays.stream(values).mapToDouble(it -> it).toArray();

        double[][] yData = new double[][]{averageClientTimes, averageProcessingTimes, averageSortingTimes};

        XYChart chart = QuickChart.getChart("Statistics", variableArgTypeLiteral, "Time, ms",
                new String[]{"Average Per Client Time", "Average Processing Time", "Average Sorting Time"},
                xData, yData);
        this.charts.add(chart);

        File chartFile = new File(directoryFile, "Chart.png");
        try (FileOutputStream fileOutputStream = new FileOutputStream(chartFile)) {
            BitmapEncoder.saveBitmap(chart, fileOutputStream, BitmapEncoder.BitmapFormat.PNG);
        }
    }

    public List<XYChart> getCharts() {
        return charts;
    }

    private static final class IterationBenchmarkResults {
        private final int variableValue;
        private final double averageClientTime;
        private final double averageProcessingTime;
        private final double averageSortingTime;

        private IterationBenchmarkResults(int variableValue, double averageClientTime, double averageProcessingTime, double averageSortingTime) {
            this.variableValue = variableValue;
            this.averageClientTime = averageClientTime;
            this.averageProcessingTime = averageProcessingTime;
            this.averageSortingTime = averageSortingTime;
        }
    }
}

