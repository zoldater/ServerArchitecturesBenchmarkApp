package com.example.zoldater.core;

import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.ConfigurationProtos;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureRequest;
import ru.spbau.mit.core.proto.ConfigurationProtos.ArchitectureResponse;
import ru.spbau.mit.core.proto.ResultsProtos;
import ru.spbau.mit.core.proto.SortingProtos;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {


    public static <T extends com.google.protobuf.GeneratedMessageV3> void writeToStream(T message, OutputStream outputStream) throws IOException {
        byte[] bytes = message.toByteArray();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(bytes.length);
        dataOutputStream.write(bytes);
        dataOutputStream.flush();
    }

    public static SortingProtos.SortingMessage readSortingMessage(InputStream inputStream) throws IOException {
        try {
            byte[] bytes = receiveBytes(inputStream);
            return SortingProtos.SortingMessage.parseFrom(bytes);
        } catch (EOFException e) {
            return null;
        }
    }

    public static ArchitectureRequest readArchitectureRequest(InputStream inputStream) throws IOException {
        try {
            byte[] bytes = receiveBytes(inputStream);
            return ArchitectureRequest.parseFrom(bytes);
        } catch (EOFException e) {
            return null;
        }
    }

    public static ResultsProtos.Request readResultsRequest(InputStream inputStream) throws IOException {
        try {
            byte[] bytes = receiveBytes(inputStream);
            return ResultsProtos.Request.parseFrom(bytes);
        } catch (EOFException e) {
            return null;
        }
    }

    public static ArchitectureResponse readArchitectureResponse(InputStream inputStream) throws IOException {
        try {
            byte[] bytes = receiveBytes(inputStream);
            return ArchitectureResponse.parseFrom(bytes);
        } catch (EOFException e) {
            return null;
        }

    }

    public static ResultsProtos.Response readResultsResponse(InputStream inputStream) throws IOException {
        try {
            byte[] bytes = receiveBytes(inputStream);
            return ResultsProtos.Response.parseFrom(bytes);
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

    public static SortingProtos.SortingMessage processSortingMessage(@Nullable SortingProtos.SortingMessage message) {
        if (message == null) {
            return null;
        }
        int[] arr = message.getElementsList().stream().mapToInt(Integer::intValue).toArray();
        bubbleSort(arr);
        SortingProtos.SortingMessage.Builder builder = SortingProtos.SortingMessage.newBuilder();
        List<Integer> sortedElements = Arrays.stream(arr).boxed().collect(Collectors.toList());
        builder.addAllElements(sortedElements);
        return builder.build();
    }

    private static void bubbleSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n; i++) {
            for (int j = 1; j < (n - i); j++) {
                if (arr[j - 1] > arr[j]) {
                    int tmp = arr[j - 1];
                    arr[j - 1] = arr[j];
                    arr[j] = tmp;
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
