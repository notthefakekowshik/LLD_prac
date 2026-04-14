package com.lldprep.foundations.behavioral.chainofresponsibility.good;

public enum LogLevel {
    DEBUG(1), INFO(2), WARN(3), ERROR(4);

    public final int value;
    LogLevel(int value) { this.value = value; }
}
