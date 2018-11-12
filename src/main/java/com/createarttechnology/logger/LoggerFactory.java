package com.createarttechnology.logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志工厂类
 * Created by lixuhui on 2017/8/14.
 */
public class LoggerFactory {

    /**
     * 默认日志等级
     */
    private static Level DEFAULT_LEVEL = Level.INFO;

    /**
     * 默认显示时间
     */
    private static boolean DEFAULT_SHOW_TIME = true;

    /**
     * 默认显示等级
     */
    private static boolean DEFAULT_SHOW_LEVEL = true;

    /**
     * 默认使用控制台输出
     */
    private static boolean DEFAULT_STDOUT = true;

    /**
     * 所有日志放在ConcurrentHashMap中
     */
    private static ConcurrentHashMap<String, Logger> LOGGERS = new ConcurrentHashMap<String, Logger>(20);

    /**
     * 禁止实例化
     */
    private LoggerFactory() {}

    static {
        final LogWorkThread logWorkThread = new LogWorkThread();
        logWorkThread.setDaemon(true);
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
            logger = new Logger(name, DEFAULT_LEVEL, DEFAULT_SHOW_TIME, DEFAULT_SHOW_LEVEL, DEFAULT_STDOUT);
            LOGGERS.putIfAbsent(name, logger);
        }
        return logger;
    }

    static Logger getLogger(Class clazz) {
        return getLogger(clazz.getSimpleName());
    }

    public static Level getDefaultLevel() {
        return DEFAULT_LEVEL;
    }

    public static void setDefaultLevel(Level defaultLevel) {
        DEFAULT_LEVEL = defaultLevel;
    }

    public static boolean isDefaultShowTime() {
        return DEFAULT_SHOW_TIME;
    }

    public static void setDefaultShowTime(boolean defaultShowTime) {
        DEFAULT_SHOW_TIME = defaultShowTime;
    }

    public static boolean isDefaultShowLevel() {
        return DEFAULT_SHOW_LEVEL;
    }

    public static void setDefaultShowLevel(boolean defaultShowLevel) {
        DEFAULT_SHOW_LEVEL = defaultShowLevel;
    }

    /**
     * 获取所有Logger，供LogWorkThread排序，用只读Map包装
     * @return
     */
    static Map<String, Logger> getLoggers() {
        return Collections.unmodifiableMap(LOGGERS);
    }

    public static class LogWorkThread extends Thread {

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
                }

                Map<String, Logger> loggers = LoggerFactory.getLoggers();
                List<Logger> loggerList = new LinkedList<Logger>();
                for (String key : loggers.keySet()) {
                    loggerList.add(loggers.get(key));
                }
                Collections.sort(loggerList, new Comparator<Logger>() {
                    public int compare(Logger logger1, Logger logger2) {
                        if (logger1.getQueueCount() > logger2.getQueueCount()) return 1;
                        else if (logger1.getQueueCount() < logger2.getQueueCount()) return -1;
                        else return 0;
                    }
                });
                for (Logger logger : loggerList) {
                    if (logger.getLogQueueSize() > 0) {
                        try {
                            logger.doWriteLog();
                        } catch (Exception e) {
                            InnerUtil.log("LogWorkThread run logger.doWriteLog", e);
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
