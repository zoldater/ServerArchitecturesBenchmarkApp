package com.example.zoldater.core.configuration.data;


import com.example.zoldater.core.enums.ArgumentTypeEnum;

public class ValueArgumentData extends AbstractArgumentData {
    private final int value;

    public ValueArgumentData(ArgumentTypeEnum argumentTypeEnum, int value) {
        super(argumentTypeEnum);
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
