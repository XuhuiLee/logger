package com.createarttechnology.logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志工厂类
 * Created by lixuhui on 2017/8/14.
 */
public final class LoggerFactory {

    /**
     * 默认日志等级
     */
    private static final Level LEVEL;

    /**
     * 输出到控制台
     */
    private static final boolean STDOUT;

    /**
     * 每个logger每次仅打印有限数量，避免阻塞其他logger
     */
    private static final int PRINT_SIZE;

    /**
     * 队列内内容超过一定数量将清理，避免日志积压
     */
    private static final int CLEAR_THRESHOLD;

    /**
     * 所有日志放在ConcurrentHashMap中
     */
    private static ConcurrentHashMap<String, Logger> LOGGERS = new ConcurrentHashMap<String, Logger>(20);

    /**
     * 禁止实例化
     */
    private LoggerFactory() {}

    static {
        try {
            System.getProperties().load(LoggerFactory.class.getResourceAsStream("/logger.properties"));
        } catch (IOException e) {
            InnerUtil.error("LoggerFactory static System.getProperties().load", e);
        }

        String levelStr = System.getProperty("logger.properties.level", "INFO");
        Level level = Level.INFO;
        if (levelStr != null) {
            try {
                level = Level.valueOf(levelStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                InnerUtil.error("LoggerFactory static level Level.valueOf", e);
            }
        }
        LEVEL = level;

        String stdoutStr = System.getProperty("logger.properties.stdout", "false");
        STDOUT = Boolean.valueOf(stdoutStr);

        String printSizeStr = System.getProperty("logger.properties.printSize", "100");
        int printSize = 100;
        try {
            printSize = Integer.valueOf(printSizeStr);
        } catch (NumberFormatException e) {
            InnerUtil.error("LoggerFactory static PRINT_SIZE Level.valueOf", e);
        }
        PRINT_SIZE = printSize;

        String clearThresholdStr = System.getProperty("logger.properties.clearThreshold", "100");
        int clearThreshold = 100;
        try {
            clearThreshold = Integer.valueOf(clearThresholdStr);
        } catch (NumberFormatException e) {
            InnerUtil.error("LoggerFactory static CLEAR_THRESHOLD Level.valueOf", e);
        }
        CLEAR_THRESHOLD = clearThreshold;

        String configInfo = String.format("LoggerFactory\t%s\tINFO\t[level:%s, stdout:%b, printSize:%d, clearThreshold:%d]\n",
                InnerUtil.buildTimeString(System.currentTimeMillis()),
                LEVEL, STDOUT, PRINT_SIZE, CLEAR_THRESHOLD);

        InnerUtil.info(configInfo);

        final LogWorkThread logWorkThread = new LogWorkThread();
        logWorkThread.start();
        //注册退出功能
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                logWorkThread.shutdown();
            }
        }));
    }

    /**
     * 工厂方法获取Logger实例
     */
    static Logger getLogger(String name) {
        Logger logger = LOGGERS.get(name);
        if (logger == null) {
            synchronized (LoggerFactory.class) {
                logger = new Logger(name, LEVEL, STDOUT, PRINT_SIZE, CLEAR_THRESHOLD);
                LOGGERS.putIfAbsent(name, logger);
            }
        }
        return logger;
    }

    static Logger getLogger(Class clazz) {
        return getLogger(clazz.getSimpleName());
    }

    /**
     * 获取所有Logger，供LogWorkThread排序，用只读Map包装
     * @return
     */
    static Map<String, Logger> getLoggers() {
        return Collections.unmodifiableMap(LOGGERS);
    }

    static class LogWorkThread extends Thread {

        private int INTERVAL = 500;

        LogWorkThread() {
            this.setName("LogWorkThread");
            this.setDaemon(true);
        }

        @Override
        public void run() {
            while (!interrupted()) {
                try {
                    sleep(INTERVAL);
                } catch (Exception e) {
                    InnerUtil.error("LogWorkThread run sleep", e);
                    Thread.currentThread().interrupt();
                }

                Map<String, Logger> loggers = LoggerFactory.getLoggers();
                for (Logger logger : loggers.values()) {
                    if (logger.getLogQueueSize() > 0) {
                        try {
                            logger.doWriteLog();
                        } catch (Exception e) {
                            InnerUtil.error("LogWorkThread run logger.doWriteLog", e);
                        }
                    }
                }
            }
        }

        void shutdown() {
            this.interrupt();
        }


    }
}
