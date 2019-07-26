package com.createarttechnology.logger;

/**
 * Created by lixuhui on 2018/2/7.
 */
enum Level {
    TRACE(0),
    DEBUG(10),
    INFO(20),
    WARN(30),
    ERROR(40),
    FATAL(50);

    int value;

    Level(int value) {
        this.value = value;
    }

    int getValue() {
        return value;
    }
}
