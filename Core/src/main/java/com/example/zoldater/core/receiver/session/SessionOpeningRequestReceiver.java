package com.example.zoldater.core.receiver.session;

import com.example.zoldater.core.receiver.AbstractMessageReceiver;
import ru.spbau.mit.core.proto.ResultProtos;
import ru.spbau.mit.core.proto.SessionProtos;

import java.io.DataInputStream;
import java.io.IOException;

public class SessionOpeningRequestReceiver extends AbstractMessageReceiver<SessionProtos.SessionOpeningRequest> {
    @Override
    public SessionProtos.SessionOpeningRequest deserialize(DataInputStream dis) throws IOException {
        return SessionProtos.SessionOpeningRequest.parseFrom(dis);
    }
}
