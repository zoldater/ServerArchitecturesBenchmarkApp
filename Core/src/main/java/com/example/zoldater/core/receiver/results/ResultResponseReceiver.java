package com.example.zoldater.core.receiver.results;

import com.example.zoldater.core.receiver.AbstractMessageReceiver;
import ru.spbau.mit.core.proto.ResultProtos;

import java.io.DataInputStream;
import java.io.IOException;

public class ResultResponseReceiver extends AbstractMessageReceiver<ResultProtos.ResultResponse> {
    @Override
    public ResultProtos.ResultResponse deserialize(DataInputStream dis) throws IOException {
        return ResultProtos.ResultResponse.parseFrom(dis);
    }
}
