package com.example.zoldater.core;

import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos;
import ru.spbau.mit.core.proto.ResultsProtos;
import ru.spbau.mit.core.proto.ResultsProtos.IterationResultsMessage;
import ru.spbau.mit.core.proto.SortingProtos;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ru.spbau.mit.core.proto.SortingProtos.*;

public class Utils {


    public static <T extends com.google.protobuf.GeneratedMessageV3> void writeToStream(T message, OutputStream outputStream) throws IOException {
        byte[] bytes = message.toByteArray();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(bytes.length);
        dataOutputStream.write(bytes);
        dataOutputStream.flush();
    }

    public static SortingMessage readSortingMessage(InputStream inputStream) throws IOException {
        try {
            byte[] bytes = receiveBytes(inputStream);
            return SortingMessage.parseFrom(bytes);
        } catch (EOFException e) {
            return null;
        }
    }

    public static ConfigurationProtos.ConfigurationRequest readConfigurationRequest(InputStream inputStream) throws IOException {
        try {
            byte[] bytes = receiveBytes(inputStream);
            return ConfigurationProtos.ConfigurationRequest.parseFrom(bytes);
        } catch (EOFException e) {
            return null;
        }
    }

    public static ConfigurationProtos.ConfigurationResponse readConfigurationResponse(InputStream inputStream) throws IOException {
        try {
            byte[] bytes = receiveBytes(inputStream);
            return ConfigurationProtos.ConfigurationResponse.parseFrom(bytes);
        } catch (EOFException e) {
            return null;
        }

    }

    public static IterationResultsMessage readResults(InputStream inputStream) throws IOException {
        try {
            byte[] bytes = receiveBytes(inputStream);
            return IterationResultsMessage.parseFrom(bytes);
        } catch (EOFException e) {
            return null;
        }
    }

    private static byte[] receiveBytes(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int size = dataInputStream.readInt();
        int alreadyRead = 0;
        byte[] messageBytes = new byte[size];
        while (alreadyRead < size) {
            alreadyRead += dataInputStream.read(messageBytes, alreadyRead, size - alreadyRead);
        }
        return messageBytes;
    }

    public static SortingMessage processSortingMessage(@Nullable SortingMessage message) {
        if (message == null) {
            return null;
        }
        int[] arr = message.getElementsList().stream().mapToInt(Integer::intValue).toArray();
        bubbleSort(arr);
        List<Integer> sortedElements = Arrays.stream(arr).boxed().collect(Collectors.toList());
        return SortingMessage.newBuilder()
                .addAllElements(sortedElements)
                .build();
    }

    private static void bubbleSort(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            for (int j = i; j < arr.length; j++) {
                if (arr[i] > arr[j]) {
                    int tmp = arr[j];
                    arr[j] = arr[i];
                    arr[i] = tmp;
                }
            }
        }
    }


    public static void closeResources(Socket socket, InputStream is, OutputStream os) {
        RuntimeException exception = new RuntimeException("Exception while closing resources!");
        if (os != null) {
            try {
                os.close();
            } catch (IOException e1) {
                exception.addSuppressed(e1);
            }
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException e1) {
                exception.addSuppressed(e1);
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e1) {
                exception.addSuppressed(e1);
            }
        }
        if (exception.getSuppressed().length > 0) {
            Logger.error(exception);
            throw exception;
        }
    }

}
