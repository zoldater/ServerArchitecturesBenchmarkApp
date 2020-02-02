package com.example.zoldater.client;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.configuration.SingleIterationConfiguration;
import com.example.zoldater.core.enums.PortConstantEnum;
import com.google.common.collect.Ordering;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class NonBlockingClient extends AbstractClient {
    public NonBlockingClient(SingleIterationConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected Socket initSocket() throws IOException {
        return new Socket(configuration.getServerAddress(), PortConstantEnum.SERVER_NONBLOCKING_PORT.getPort());
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
        List<Integer> list = receivedMessage.getElementsList();
        boolean ordered = Ordering.natural().isOrdered(list);
        if (!ordered) {
            Logger.error("Response message not sorted!");
            throw new RuntimeException();
        }

    }
}
