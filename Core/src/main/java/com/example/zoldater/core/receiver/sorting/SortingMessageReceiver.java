package com.example.zoldater.core.receiver.sorting;

import com.example.zoldater.core.receiver.AbstractMessageReceiver;
import ru.spbau.mit.core.proto.SortingProtos;

import java.io.DataInputStream;
import java.io.IOException;

public class SortingMessageReceiver extends AbstractMessageReceiver<SortingProtos.SortingMessage> {
    @Override
    public SortingProtos.SortingMessage deserialize(DataInputStream dis) throws IOException {
        return SortingProtos.SortingMessage.parseFrom(dis);
    }
}
