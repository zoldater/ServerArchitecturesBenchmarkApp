package com.example.zoldater.core.configuration;

import com.example.zoldater.core.configuration.data.ValueArgumentData;
import com.example.zoldater.core.configuration.data.ValueArgumentDataBuilder;
import com.example.zoldater.core.configuration.data.VariableArgumentData;
import com.example.zoldater.core.enums.ArchitectureTypeEnum;
import com.example.zoldater.core.exception.InvalidConfigurationException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.zoldater.core.enums.ArgumentTypeEnum.*;


public class SingleIterationConfiguration extends AbstractConfiguration {

    private final ValueArgumentData arrayElements;
    private final ValueArgumentData clientsNumber;
    private final ValueArgumentData deltaMs;
    private final ValueArgumentData requestsPerClient;

    private SingleIterationConfiguration(ArchitectureTypeEnum architectureType,
                                         String serverAddress,
                                         ValueArgumentData arrayElements,
                                         ValueArgumentData clientsNumber,
                                         ValueArgumentData deltaMs,
                                         ValueArgumentData requestsPerClient) {
        super(architectureType, serverAddress);
        this.arrayElements = arrayElements;
        this.clientsNumber = clientsNumber;
        this.deltaMs = deltaMs;
        this.requestsPerClient = requestsPerClient;
    }

    public static List<SingleIterationConfiguration> fromInitialConfiguration(InitialConfiguration initialConfiguration) {
        VariableArgumentData variableArgumentData = initialConfiguration.getVariableArgumentData();
        ValueArgumentData firstArgument = initialConfiguration.getValueArgumentData1();
        ValueArgumentData secondArgument = initialConfiguration.getValueArgumentData2();
        ValueArgumentData requestsPerClientSession = initialConfiguration.getRequestsPerClientSession();
        ValueArgumentData arrayElements;
        ValueArgumentData clientsNumber;
        ValueArgumentData deltaMs;
        switch (variableArgumentData.getArgumentTypeEnum()) {
            case CLIENTS_NUMBER:
                arrayElements = new ValueArgumentDataBuilder().setArgumentTypeEnum(ARRAY_ELEMENTS).setValue(ARRAY_ELEMENTS.equals(firstArgument.getArgumentTypeEnum())
                        ? firstArgument.getValue()
                        : secondArgument.getValue()).createValueArgumentData();
                deltaMs = new ValueArgumentDataBuilder().setArgumentTypeEnum(DELTA_MS).setValue(DELTA_MS.equals(firstArgument.getArgumentTypeEnum())
                        ? firstArgument.getValue()
                        : secondArgument.getValue()).createValueArgumentData();
                return Stream.iterate(variableArgumentData.getFrom(),
                        i -> i < variableArgumentData.getTo(),
                        i -> i + variableArgumentData.getStep())
                        .map(it ->
                                new SingleIterationConfiguration(initialConfiguration.getArchitectureType(),
                                        initialConfiguration.getServerAddress(),
                                        arrayElements,
                                        new ValueArgumentDataBuilder().setArgumentTypeEnum(CLIENTS_NUMBER).setValue(it).createValueArgumentData(),
                                        deltaMs,
                                        requestsPerClientSession)
                        ).collect(Collectors.toList());
            case ARRAY_ELEMENTS:
                clientsNumber = new ValueArgumentDataBuilder().setArgumentTypeEnum(CLIENTS_NUMBER).setValue(CLIENTS_NUMBER.equals(firstArgument.getArgumentTypeEnum())
                        ? firstArgument.getValue() :
                        secondArgument.getValue()).createValueArgumentData();
                deltaMs = new ValueArgumentDataBuilder().setArgumentTypeEnum(DELTA_MS).setValue(DELTA_MS.equals(firstArgument.getArgumentTypeEnum())
                        ? firstArgument.getValue()
                        : secondArgument.getValue()).createValueArgumentData();
                return Stream.iterate(variableArgumentData.getFrom(),
                        i -> i < variableArgumentData.getTo(),
                        i -> i + variableArgumentData.getStep())
                        .map(it ->
                                new SingleIterationConfiguration(initialConfiguration.getArchitectureType(),
                                        initialConfiguration.getServerAddress(),
                                        new ValueArgumentDataBuilder().setArgumentTypeEnum(ARRAY_ELEMENTS).setValue(it).createValueArgumentData(),
                                        clientsNumber,
                                        deltaMs,
                                        requestsPerClientSession)
                        ).collect(Collectors.toList());

            case DELTA_MS:
                arrayElements = new ValueArgumentDataBuilder().setArgumentTypeEnum(ARRAY_ELEMENTS).setValue(ARRAY_ELEMENTS.equals(firstArgument.getArgumentTypeEnum())
                        ? firstArgument.getValue()
                        : secondArgument.getValue()).createValueArgumentData();
                clientsNumber = new ValueArgumentDataBuilder().setArgumentTypeEnum(CLIENTS_NUMBER).setValue(CLIENTS_NUMBER.equals(firstArgument.getArgumentTypeEnum())
                        ? firstArgument.getValue()
                        : secondArgument.getValue()).createValueArgumentData();
                return Stream.iterate(variableArgumentData.getFrom(),
                        i -> i < variableArgumentData.getTo(),
                        i -> i + variableArgumentData.getStep())
                        .map(it ->
                                new SingleIterationConfiguration(initialConfiguration.getArchitectureType(),
                                        initialConfiguration.getServerAddress(),
                                        arrayElements,
                                        clientsNumber,
                                        new ValueArgumentDataBuilder().setArgumentTypeEnum(DELTA_MS).setValue(it).createValueArgumentData(),
                                        requestsPerClientSession)
                        ).collect(Collectors.toList());
            default:
                throw new InvalidConfigurationException("Unexpected variable parameter type "
                        + variableArgumentData.getArgumentTypeEnum());
        }
    }


    public ValueArgumentData getRequestsPerClient() {
        return requestsPerClient;
    }

    public ValueArgumentData getDeltaMs() {
        return deltaMs;
    }

    public ValueArgumentData getClientsNumber() {
        return clientsNumber;
    }

    public ValueArgumentData getArrayElements() {
        return arrayElements;
    }
}
