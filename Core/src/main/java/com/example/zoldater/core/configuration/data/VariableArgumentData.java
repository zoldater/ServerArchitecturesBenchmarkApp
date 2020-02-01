package com.example.zoldater.core.configuration.data;


import com.example.zoldater.core.enums.ArgumentTypeEnum;

public class VariableArgumentData extends AbstractArgumentData {
    private final int from;
    private final int to;
    private final int step;

    public VariableArgumentData(ArgumentTypeEnum argumentTypeEnum, int from, int to, int step) {
        super(argumentTypeEnum);
        this.from = from;
        this.to = to;
        this.step = step;
    }

    public int getStep() {
        return step;
    }

    public int getTo() {
        return to;
    }

    public int getFrom() {
        return from;
    }
}
