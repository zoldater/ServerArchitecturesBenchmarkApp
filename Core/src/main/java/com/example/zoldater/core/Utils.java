package com.example.zoldater.core;

import org.tinylog.Logger;

import java.io.*;
import java.net.Socket;

public class Utils {


    public static <T extends com.google.protobuf.GeneratedMessageV3> byte[] serialize(T message) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(message.getSerializedSize() + 4);
        new DataOutputStream(byteArrayOutputStream).writeInt(message.getSerializedSize());
        message.writeTo(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
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
