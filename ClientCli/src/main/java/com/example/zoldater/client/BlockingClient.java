package com.example.zoldater.client;

import com.example.zoldater.core.configuration.SingleIterationConfiguration;
import com.google.common.collect.Ordering;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class BlockingClient extends AbstractClient {
    public BlockingClient(SingleIterationConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected void process(InputStream is, OutputStream os) throws IOException {
        SortingMessage msg = generateMessage();
        msg.writeDelimitedTo(os);
        os.flush();
        SortingMessage receivedMessage = SortingMessage.parseDelimitedFrom(is);
        List<Integer> list = receivedMessage.getElementsList();
        boolean ordered = Ordering.natural().isOrdered(list);
        if (!ordered) {
            Logger.error("Response message not sorted!");
            throw new RuntimeException();
        }
    }
}
