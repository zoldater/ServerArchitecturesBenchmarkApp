package com.example.zoldater.core.configuration.data;


import com.example.zoldater.core.enums.ArgumentTypeEnum;

public abstract class AbstractArgumentData {
    private final ArgumentTypeEnum argumentTypeEnum;

    protected AbstractArgumentData(ArgumentTypeEnum argumentTypeEnum) {
        this.argumentTypeEnum = argumentTypeEnum;
    }

    public ArgumentTypeEnum getArgumentTypeEnum() {
        return argumentTypeEnum;
    }
}
