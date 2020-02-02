package com.example.zoldater.client;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.configuration.SingleIterationConfiguration;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.*;

public class NonBlockingClient extends AbstractClient {
    public NonBlockingClient(SingleIterationConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected void process(InputStream is, OutputStream os) throws IOException {
        SortingMessage msg = generateMessage();
        byte[] messageBytes = Utils.serialize(msg);
        os.write(messageBytes);
        os.flush();
        int readCount = is.read(messageBytes);
        while (readCount < messageBytes.length) {
            readCount += is.read(messageBytes, readCount, messageBytes.length - readCount);
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(messageBytes);
        DataInputStream dis = new DataInputStream(byteArrayInputStream);
        int serializedSize = dis.readInt();
        SortingMessage receivedMessage = SortingMessage.parseFrom(dis);
    }
}
