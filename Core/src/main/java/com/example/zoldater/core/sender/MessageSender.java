package com.example.zoldater.core.sender;

import com.example.zoldater.core.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;

public class MessageSender<T extends com.google.protobuf.GeneratedMessageV3> {
    private final T sendingMessage;

    private static final Logger LOGGER = LogManager.getLogger();

    public MessageSender(T sendingMessage) {
        this.sendingMessage = sendingMessage;
    }

    public void send(Socket s) {
        OutputStream outputStream = null;
        try {
            outputStream = s.getOutputStream();
            byte[] sendingMessageBytes = Utils.serialize(sendingMessage);
            outputStream.write(sendingMessageBytes);
            outputStream.flush();
        } catch (IOException e) {
            Utils.closeResources(null, null, outputStream);
        }
    }


    public T getSendingMessage() {
        return sendingMessage;
    }
}
