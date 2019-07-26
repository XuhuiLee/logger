package com.createarttechnology.logger;

import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

/**
 * 日志记录类
 * Created by lixuhui on 2017/8/14.
 */
public final class Logger {

    /**
     * 日志等级
     */
    private final Level level;

    /**
     * 缓冲队列，在concurrent包中
     */
    private final ConcurrentLinkedQueue<LogItem> logQueue = new ConcurrentLinkedQueue<LogItem>();

    /**
     * 日志名，不包括日期
     */
    private final String name;

    /**
     * 异常数目统计
     */
    private final LongAdder exceptionCount = new LongAdder();

    /**
     * 日志文件大小
     */
    private long size;

    /**
     * 日志标准输出
     */
    private final boolean stdout;

    private final int printSize;

    private final int clearThreshold;

    private static final Logger ALL_EXCEPTION = LoggerFactory.getLogger("_AllException");

    Logger(String name, Level level, boolean stdout, int printSize, int clearThreshold) {
        this.name = name;
        this.level = level;
        this.stdout = stdout;
        this.printSize = printSize;
        this.clearThreshold = clearThreshold;
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
        if (item.getThrowable() != null && !this.equals(ALL_EXCEPTION)) {  // 把所有异常记录到_AllException中
            exceptionCount.increment();
            ALL_EXCEPTION.error(name + '\t' + buildLogString(item));
        }
        boolean success = logQueue.offer(item);
        if (!success) {
            exceptionCount.increment();
            InnerUtil.info("InnerUtil\tlogQueue.offer failed, may be full, queueCount=" + getQueueCount());
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
            synchronized (this) {
                int i = 0;
                // 每次最多打印100条避免日志过多阻塞其他logger
                while (!logQueue.isEmpty() && i++ < printSize) {
                    final LogItem item = logQueue.poll();
                    String logString = buildLogString(item);
                    try {
                        bw.write(logString);
                    } catch (Exception e) {
                        InnerUtil.error("Logger doWriteLog bw.write", e);
                    }
                    long fileSize = logFile.length();
                    if (fileSize < size) {
                        // 说明是新一天的日志文件了，exceptionCount清空
                        exceptionCount.reset();
                    }
                    this.size = fileSize;
                    if (stdout) {
                        InnerUtil.info(name + '\t' + logString);
                    }
                }
                // 避免日志积压过多
                if (logQueue.size() > clearThreshold) {
                    exceptionCount.increment();
                    ALL_EXCEPTION.info("logQueue clear, name={}, size={}", name, getQueueCount());
                    logQueue.clear();
                }
            }
        } finally {
            try {
                bw.flush();
                bw.close();
            } catch (IOException e) {
                exceptionCount.increment();
                InnerUtil.error("Logger doWriteLog bw.flush, bw.close", e);
            }
        }
    }

    /**
     * 格式化日志String
     */
    private String buildLogString(LogItem item) {
        int size = item.getThrowable() == null ? 128 : 2048;
        StringBuilder sb = new StringBuilder(size);

        sb.append(InnerUtil.buildTimeString(item.getTime())).append('\t')
                .append(item.getLevel().name()).append("\t")
                .append(InnerUtil.buildMessage(item)).append("\n");

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

    public long getExceptionCount() {
        return exceptionCount.longValue();
    }

    public int getLogQueueSize() {
        return logQueue.size();
    }
}
