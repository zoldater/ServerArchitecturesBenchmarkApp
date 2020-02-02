package com.example.zoldater.core.enums;

public enum ArchitectureTypeEnum {
    ONLY_THREADS_ARCH(1),
    WITH_EXECUTORS_ARCH(2),
    NON_BLOCKING_ARCH(3);

    public final int code;

    ArchitectureTypeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
