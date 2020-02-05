package com.example.zoldater.core.configuration.data;

import com.example.zoldater.core.enums.ArgumentTypeEnum;

public class VariableArgumentDataBuilder {
    private ArgumentTypeEnum argumentTypeEnum;
    private int from;
    private int to;
    private int step;

    public VariableArgumentDataBuilder setArgumentTypeEnum(ArgumentTypeEnum argumentTypeEnum) {
        this.argumentTypeEnum = argumentTypeEnum;
        return this;
    }

    public VariableArgumentDataBuilder setFrom(int from) {
        this.from = from;
        return this;
    }

    public VariableArgumentDataBuilder setTo(int to) {
        this.to = to;
        return this;
    }

    public VariableArgumentDataBuilder setStep(int step) {
        this.step = step;
        return this;
    }

    public VariableArgumentData createVariableArgumentData() {
        return new VariableArgumentData(argumentTypeEnum, from, to, step);
    }
}