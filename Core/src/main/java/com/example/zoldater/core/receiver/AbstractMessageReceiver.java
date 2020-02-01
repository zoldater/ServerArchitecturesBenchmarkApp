package com.example.zoldater.core.receiver;

import com.example.zoldater.core.Utils;
import org.tinylog.Logger;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public abstract class AbstractMessageReceiver<T extends com.google.protobuf.GeneratedMessageV3> {
    private T receivedMessage;


    public T getReceivedMessage() {
        return receivedMessage;
    }

    public void receive(Socket s) {
        InputStream inputStream = null;
        try {
            inputStream = s.getInputStream();
            DataInputStream dis = new DataInputStream(inputStream);
            int serializedSize = dis.readInt();
            receivedMessage = deserialize(dis);
        } catch (IOException e) {
            Logger.error("Error during receive response!", e);
            throw new RuntimeException(e);
        } finally {
            Utils.closeResources(null, inputStream, null);
        }
    }

    public abstract T deserialize(DataInputStream dis) throws IOException;

}
