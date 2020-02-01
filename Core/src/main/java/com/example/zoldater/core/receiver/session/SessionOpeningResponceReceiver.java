package com.example.zoldater.core.receiver.session;

import com.example.zoldater.core.receiver.AbstractMessageReceiver;
import ru.spbau.mit.core.proto.SessionProtos;

import java.io.DataInputStream;
import java.io.IOException;

public class SessionOpeningResponceReceiver extends AbstractMessageReceiver<SessionProtos.SessionOpeningResponce> {
    @Override
    public SessionProtos.SessionOpeningResponce deserialize(DataInputStream dis) throws IOException {
        return SessionProtos.SessionOpeningResponce.parseFrom(dis);
    }
}
