package com.example.zoldater.client;

import com.example.zoldater.core.configuration.SingleIterationConfiguration;
import ru.spbau.mit.core.proto.SortingProtos;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
    }
}
