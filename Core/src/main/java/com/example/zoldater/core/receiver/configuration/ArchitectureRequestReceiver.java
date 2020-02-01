package com.example.zoldater.core.receiver.configuration;

import com.example.zoldater.core.receiver.AbstractMessageReceiver;
import ru.spbau.mit.core.proto.ConfigurationProtos;

import java.io.DataInputStream;
import java.io.IOException;

public class ArchitectureRequestReceiver extends AbstractMessageReceiver<ConfigurationProtos.ArchitectureRequest> {
    @Override
    public ConfigurationProtos.ArchitectureRequest deserialize(DataInputStream dis) throws IOException {
        return ConfigurationProtos.ArchitectureRequest.parseFrom(dis);
    }
}
