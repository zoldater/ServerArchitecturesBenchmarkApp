package com.example.zoldater.client;

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
import ru.spbau.mit.core.proto.ConfigurationProtos;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureResponse;
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

public class ClientMaster {
    private final InitialConfiguration initialConfiguration;
    private final ExecutorService sortingConnectionService = Executors.newCachedThreadPool();


    private final List<XYChart> charts = new ArrayList<>();


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
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            ConfigurationProtos.ArchitectureRequest request = ConfigurationProtos.ArchitectureRequest.newBuilder()
                    .setArchitectureCode(initialConfiguration.getArchitectureType().getCode())
                    .build();
            Utils.writeToStream(request, outputStream);

            ArchitectureResponse response = Utils.readArchitectureResponse(inputStream);
            List<SingleIterationConfiguration> iterationConfigurations = SingleIterationConfiguration.fromInitialConfiguration(initialConfiguration);

            iterationConfigurations.forEach(config -> {
                Logger.info("Started new config!");
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
            });
            ResultsProtos.Request resultRequest = ResultsProtos.Request.getDefaultInstance();
            Utils.writeToStream(resultRequest, outputStream);
            ResultsProtos.Response resultsResponse = Utils.readResultsResponse(inputStream);
            saveResultsToCsvAndImage(resultsResponse, initialConfiguration);

        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        } finally {
            sortingConnectionService.shutdownNow();
            Utils.closeResources(socket, null, null);
        }

    }

    private void saveResultsToCsvAndImage(ResultsProtos.Response response, InitialConfiguration configuration) throws IOException {
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
        int[] bunchSizes;
        int[] values = IntStream.iterate(variableArgumentData.getFrom(), it -> it < variableArgumentData.getTo(), it -> it + variableArgumentData.getStep()).toArray();
        int iterations = values.length;

        if (variableArgumentData.getArgumentTypeEnum() != ArgumentTypeEnum.CLIENTS_NUMBER) {
            bunchSizes = IntStream.range(0, iterations)
                    .map(it -> configuration.getValueArgumentData1().getArgumentTypeEnum() == ArgumentTypeEnum.CLIENTS_NUMBER
                            ? configuration.getValueArgumentData1().getValue()
                            : configuration.getValueArgumentData2().getValue())
                    .toArray();
        } else {
            bunchSizes = values;
        }


        double[] clientTimesPerIteration = processSingleList(response.getClientTimesList(), bunchSizes);
        double[] processingTimesPerIteration = processSingleList(response.getProcessingTimesList(), bunchSizes);
        double[] sortingTimesPerIteration = processSingleList(response.getSortingTimesList(), bunchSizes);

        for (int i = 0; i < values.length; i++) {
            writer.writeNext(
                    new String[]{
                            Integer.toString(values[i]),
                            Double.toString(clientTimesPerIteration[i]),
                            Double.toString(processingTimesPerIteration[i]),
                            Double.toString(sortingTimesPerIteration[i])
                    });
        }
        writer.flush();

        String variableArgTypeLiteral = variableArgumentData.getArgumentTypeEnum().getLiteral();
        double[] xData = Arrays.stream(values).mapToDouble(it -> it).toArray();

        double[][] yData = new double[][]{clientTimesPerIteration, processingTimesPerIteration, sortingTimesPerIteration};

        XYChart chart = QuickChart.getChart("Statistics", variableArgTypeLiteral, "Time, ms",
                new String[]{"Average Per Client Time", "Average Processing Time", "Average Sorting Time"},
                xData, yData);
        this.charts.add(chart);

        File chartFile = new File(directoryFile, "Chart.png");
        try (FileOutputStream fileOutputStream = new FileOutputStream(chartFile)) {
            BitmapEncoder.saveBitmap(chart, fileOutputStream, BitmapEncoder.BitmapFormat.PNG);
        }
    }

    private static double[] processSingleList(List<Long> list, int[] bunchSizes) {
        double[] result = new double[bunchSizes.length];
        int sublistIndex = 0;
        for (int i = 0; i < bunchSizes.length - 1; i++) {
            result[i] = list.subList(sublistIndex, sublistIndex + bunchSizes[i]).stream().mapToLong(it -> it).average().orElse(0);
            sublistIndex += bunchSizes[i];
        }
        result[result.length - 1] = list.subList(sublistIndex, list.size()).stream().mapToLong(it -> it).average().orElse(0);
        return result;
    }

    public List<XYChart> getCharts() {
        return charts;
    }
}
