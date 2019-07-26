package com.createarttechnology.logger;

/**
 * 日志内容
 * Created by lixuhui on 2017/8/14.
 */
final class LogItem {

    private Level level;

    private long time;

    private String message;

    private Object[] argArray;

    private Throwable throwable;

    LogItem(Level level, String message, Object[] argArray) {
        this.level = level;
        this.time = InnerUtil.getTimestamp();
        if (message != null && message.trim().length() > 1) {
            this.message = message;
        }

        // 取出最后一个，作为throwable
        if (argArray != null && argArray.length > 0) {
            Object lastObject = argArray[argArray.length - 1];
            if (lastObject != null && lastObject instanceof Throwable) {
                this.argArray = new Object[argArray.length - 1];
                System.arraycopy(argArray, 0, this.argArray, 0, this.argArray.length);
                this.throwable = (Throwable) lastObject;
            } else {
                this.argArray = argArray;
            }
        }
    }

    Level getLevel() {
        return level;
    }

    long getTime() {
        return time;
    }

    String getMessage() {
        return message;
    }

    Object[] getArgArray() {
        return argArray;
    }

    Throwable getThrowable() {
        return throwable;
    }
}
