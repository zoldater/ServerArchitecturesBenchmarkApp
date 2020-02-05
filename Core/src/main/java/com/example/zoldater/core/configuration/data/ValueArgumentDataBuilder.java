package com.example.zoldater.core.configuration.data;

import com.example.zoldater.core.enums.ArgumentTypeEnum;

public class ValueArgumentDataBuilder {
    private ArgumentTypeEnum argumentTypeEnum;
    private int value;

    public ValueArgumentDataBuilder setArgumentTypeEnum(ArgumentTypeEnum argumentTypeEnum) {
        this.argumentTypeEnum = argumentTypeEnum;
        return this;
    }

    public ValueArgumentDataBuilder setValue(int value) {
        this.value = value;
        return this;
    }

    public ValueArgumentData createValueArgumentData() {
        return new ValueArgumentData(argumentTypeEnum, value);
    }
}