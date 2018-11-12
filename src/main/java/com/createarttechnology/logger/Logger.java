package com.createarttechnology.logger;

import java.io.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 日志记录类
 * Created by lixuhui on 2017/8/14.
 */
public class Logger {

    /**
     * 日志等级
     */
    private Level level;

    /**
     * 日志路径
     */
    private String path;

    /**
     * 缓冲队列，在concurrent包中
     */
    private LinkedBlockingQueue<LogItem> logQueue = new LinkedBlockingQueue<LogItem>(50000);

    /**
     * 日志名，不包括日期
     */
    private String name;

    /**
     * 异常数目统计
     */
    private int exceptionCount;

    /**
     * 日志文件大小
     */
    private long size;

    /**
     * 日志显示level
     */
    private boolean showLevel;

    /**
     * 日志显示时间
     */
    private boolean showTime;

    /**
     * 日志标准输出
     */
    private boolean stdout;

    private static final Logger ALL_EXCEPTION = LoggerFactory.getLogger("_AllException");

    Logger(String name, Level level, boolean showTime, boolean showLevel, boolean stdout) {
        this.name = name;
        this.level = level;
        this.showTime = showTime;
        this.showLevel = showLevel;
        this.path = name;
        this.stdout = stdout;
    }

    /**
     * 从工厂获取实例
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    public static Logger getLogger(Class clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * 判断日志是否输出
     */
    private boolean isWriteInfo() {
        return level.value <= Level.INFO.value;
    }

    private boolean isWriteError() {
        return level.value <= Level.ERROR.value;
    }

    /**
     * 统一入队方法
     */
    private void logWithoutCheckLevel(LogItem item) {
        if (item.getThrowable() != null) {  // 把所有异常记录到_AllException中
            exceptionCount++;
            ALL_EXCEPTION.error(buildLogString(item));
        }
        boolean success = logQueue.offer(item);
        if (!success) {
            exceptionCount++;
            InnerUtil.log("InnerUtil\tlogQueue.offer failed, may be touch MAX_LOG_QUEUE_SIZE 50000, queueCount=" + getQueueCount());
        }
    }

    /**
     * 输出日志到文件系统和命令行
     */
    void doWriteLog() throws Exception {
        String path = System.getProperty("catalina.home");
        if (path == null) {
            path = "/data";
        }
        File dir = new File( path + "/logs");
        if (!dir.exists()) {
            dir.mkdir();
        }
        File logFile = new File(path + "/logs/" + name + "." + InnerUtil.buildDateString(InnerUtil.getTimestamp()));
        if (!logFile.exists()) {
            logFile.createNewFile();
        }
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true)));
        try {
            while (!logQueue.isEmpty()) {
                final LogItem item = logQueue.poll();
                String logString = buildLogString(item);
                try {
                    bw.write(logString);
                } catch (Exception e) {
                    InnerUtil.log("Logger doWriteLog bw.write", e);
                }
                long fileSize = logFile.length();
                if (fileSize < size) {  // 说明是新一天的日志文件了，exceptionCount清空
                    exceptionCount = 0;
                }
                this.size = fileSize;
                if (stdout) {
                    InnerUtil.log(logString);
                }
            }
        } finally {
            try {
                bw.flush();
                bw.close();
            } catch (IOException e) {
                InnerUtil.log("Logger doWriteLog bw.flush, bw.close", e);
            }
        }
    }

    /**
     * 格式化日志String
     */
    private String buildLogString(LogItem item) {
        int size = item.getThrowable() == null ? 128 : 2048;
        StringBuilder sb = new StringBuilder(size);

        sb.append(name).append("\t");

        if (showTime) sb.append(InnerUtil.buildTimeString(item.getTime())).append("\t");

        if (showLevel) sb.append(item.getLevel().name()).append("\t");

        sb.append(InnerUtil.buildMessage(item)).append("\n");

        return sb.toString();
    }

    /**
     * 返回日志队列大小，作为LogWorkThread排序依据
     */
    int getQueueCount() {
        return logQueue.size();
    }

    /**
     * 日志格式化入队
     */
    public void info(Object arg) {
        if (isWriteInfo()) {
            LogItem item = new LogItem(Level.INFO, null, new Object[]{arg});
            logWithoutCheckLevel(item);
        }
    }

    public void info(String pattern) {
        if (isWriteInfo()) {
            LogItem item = new LogItem(Level.INFO, pattern, null);
            logWithoutCheckLevel(item);
        }
    }

    public void info(String pattern, Object arg) {
        if (isWriteInfo()) {
            LogItem item = new LogItem(Level.INFO, pattern, new Object[]{arg});
            logWithoutCheckLevel(item);
        }
    }

    public void info(String pattern, Object arg1, Object arg2) {
        if (isWriteInfo()) {
            LogItem item = new LogItem(Level.INFO, pattern, new Object[]{arg1, arg2});
            logWithoutCheckLevel(item);
        }
    }

    public void info(String pattern, Object ... args) {
        if (isWriteInfo()) {
            LogItem item = new LogItem(Level.INFO, pattern, args);
            logWithoutCheckLevel(item);
        }
    }

    public void error(Object arg) {
        if (isWriteError()) {
            LogItem item = new LogItem(Level.ERROR, null, new Object[]{arg});
            logWithoutCheckLevel(item);
        }
    }

    public void error(String pattern) {
        if (isWriteError()) {
            LogItem item = new LogItem(Level.ERROR, pattern, null);
            logWithoutCheckLevel(item);
        }
    }

    public void error(String pattern, Object arg) {
        if (isWriteError()) {
            LogItem item = new LogItem(Level.ERROR, pattern, new Object[]{arg});
            logWithoutCheckLevel(item);
        }
    }

    public void error(String pattern, Object arg1, Object arg2) {
        if (isWriteError()) {
            LogItem item = new LogItem(Level.ERROR, pattern, new Object[]{arg1, arg2});
            logWithoutCheckLevel(item);
        }
    }

    public void error(String pattern, Object ... args) {
        if (isWriteError()) {
            LogItem item = new LogItem(Level.ERROR, pattern, args);
            logWithoutCheckLevel(item);
        }
    }

    /**
     * getters and setters
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public boolean isShowTime() {
        return showTime;
    }

    public void setShowTime(boolean showTime) {
        this.showTime = showTime;
    }

    public boolean isShowLevel() {
        return showLevel;
    }

    public void setShowLevel(boolean showLevel) {
        this.showLevel = showLevel;
    }

    public boolean isStdout() {
        return stdout;
    }

    public void setStdout(boolean stdout) {
        this.stdout = stdout;
    }

    public int getExceptionCount() {
        return exceptionCount;
    }

    public void setExceptionCount(int exceptionCount) {
        this.exceptionCount = exceptionCount;
    }

    public int getLogQueueSize() {
        return logQueue.size();
    }
}
