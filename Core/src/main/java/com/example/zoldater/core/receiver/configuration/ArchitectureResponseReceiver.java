package com.example.zoldater.core.receiver.configuration;

import com.example.zoldater.core.receiver.AbstractMessageReceiver;
import ru.spbau.mit.core.proto.ConfigurationProtos;

import java.io.DataInputStream;
import java.io.IOException;

public class ArchitectureResponseReceiver extends AbstractMessageReceiver<ConfigurationProtos.ArchitectureResponse> {
    @Override
    public ConfigurationProtos.ArchitectureResponse deserialize(DataInputStream dis) throws IOException {
        return ConfigurationProtos.ArchitectureResponse.parseFrom(dis);
    }
}
