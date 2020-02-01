package com.example.zoldater.core.receiver.results;

import com.example.zoldater.core.receiver.AbstractMessageReceiver;
import ru.spbau.mit.core.proto.ResultProtos;

import java.io.DataInputStream;
import java.io.IOException;

public class ResultRequestReceiver extends AbstractMessageReceiver<ResultProtos.ResultRequest> {
    @Override
    public ResultProtos.ResultRequest deserialize(DataInputStream dis) throws IOException {
        return ResultProtos.ResultRequest.parseFrom(dis);
    }
}
