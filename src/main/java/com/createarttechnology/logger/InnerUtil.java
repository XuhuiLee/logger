package com.createarttechnology.logger;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 工具类，不允许继承
 * Created by lixuhui on 2017/8/14.
 */
abstract class InnerUtil {
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("CTT");
    private static final Locale LOCALE = Locale.CHINA;

    /**
     * 不允许实例化
     */
    private InnerUtil() {}

    /**
     * 直接输出日志到控制台
     */
    static void log(String s) {
        System.out.print(s);
    }

    /**
     * 内部出错时输出到控制台
     */
    static void log(String s, Throwable t) {
        System.out.println("InnerUtil\t" + buildTimeString(getTimestamp()) + s);
        t.printStackTrace(System.err);
    }

    /**
     * 系统当前时间
     */
    static long getTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取格式化日期字符串，YYYY-MM-DD
     */
    static String buildDateString(long timestamp) {
        Calendar calendar = Calendar.getInstance(TIME_ZONE, LOCALE);
        calendar.setTimeInMillis(timestamp);

        int year = calendar.get(Calendar.YEAR);
        int month = 1 + calendar.get(Calendar.MONTH);   // 月少了一天
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        StringBuilder sb = new StringBuilder(10);
        sb.append(year).append('-');
        if (month < 10) {
            sb.append(0);
        }
        sb.append(month).append('-');
        if (day < 10) {
            sb.append(0);
        }
        sb.append(day);

        return sb.toString();
    }

    /**
     * 获取格式化时间字符串，YYYY-MM-DD\tHH:mm:ss.SSS
     */
    static String buildTimeString(long timestamp) {
        Calendar calendar = Calendar.getInstance(TIME_ZONE, LOCALE);
        calendar.setTimeInMillis(timestamp);

        int year = calendar.get(Calendar.YEAR);
        int month = 1 + calendar.get(Calendar.MONTH);   // 月少了一天
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        int sec = calendar.get(Calendar.SECOND);
        int millis = calendar.get(Calendar.MILLISECOND);

        StringBuilder sb = new StringBuilder(23);
        sb.append(year).append('-');
        if (month < 10) {
            sb.append(0);
        }
        sb.append(month).append('-');
        if (day < 10) {
            sb.append(0);
        }
        sb.append(day).append(' ');

        if (hour < 10) {
            sb.append(0);
        }
        sb.append(hour).append(':');
        if (min < 10) {
            sb.append(0);
        }
        sb.append(min).append(':');
        if (sec < 10) {
            sb.append(0);
        }
        sb.append(sec).append('.');
        if (millis < 10) {
            sb.append("00");
        } else if (millis < 100) {
            sb.append('0');
        }
        sb.append(millis);

        return sb.toString();
    }

    private static String deeplyFormatObject(Object o) {
        if (o == null) return null;
        if (o.getClass().isArray()) {
            if (o instanceof int[]) {
                return Arrays.toString((int[]) o);
            } else if (o instanceof long[]) {
                return Arrays.toString((long[]) o);
            } else if (o instanceof char[]) {
                return Arrays.toString((char[]) o);
            } else if (o instanceof boolean[]) {
                return Arrays.toString((boolean[]) o);
            } else if (o instanceof double[]) {
                return Arrays.toString((double[]) o);
            } else if (o instanceof float[]) {
                return Arrays.toString((float[]) o);
            } else if (o instanceof byte[]) {
                return Arrays.toString((byte[]) o);
            } else {
                return Arrays.deepToString((Object[]) o);
            }
        } else {
            return o.toString();
        }
    }

    static String buildMessage(LogItem item) {
        Throwable throwable = item.getThrowable();
        int size = throwable == null ? 128 : 2048;
        StringBuilder sb = new StringBuilder(size);
        String message = item.getMessage();
        Object[] args = item.getArgArray();
        int argIndex = 0;
        int tokenIndex = 0;
        int lastTokenIndex = 0;
        if (message == null && args != null) {
            for (Object arg : args) {
                sb.append(deeplyFormatObject(arg));
            }
            return sb.toString();
        } else if (args == null) {
            if (throwable == null) {
                sb.append(message);
            } else {
                sb.append(message).append('\n').append(buildThrowableMessage(item.getThrowable()));
            }
            return sb.toString();
        }
        while (tokenIndex != -1) {
            tokenIndex = message.indexOf("{}", lastTokenIndex);
            if (tokenIndex != -1) {
                sb.append(message.substring(lastTokenIndex, tokenIndex));
                if (argIndex < args.length) sb.append(deeplyFormatObject(args[argIndex++]));
                lastTokenIndex = tokenIndex + 2;
            }
        }
        if (lastTokenIndex < message.length()) {
            sb.append(message.substring(lastTokenIndex));
        }
        if (throwable != null) {
            sb.append('\n').append(buildThrowableMessage(item.getThrowable()));
        }
        return sb.toString();
    }

    private static String buildThrowableMessage(Throwable th) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append(th.getClass().getName());
        String message = th.getMessage();
        if (message != null && message.trim().length() > 0) {
            sb.append(": ").append(message);
        }
        sb.append('\n');

        StackTraceElement[] stackTraces = th.getStackTrace();
        for (StackTraceElement e : stackTraces) {
            if (e.getLineNumber() < 0) {
                continue;
            }
            sb.append("\tat ").append(e.toString()).append('\n');
        }

        if (th.getCause() != null) {
            sb.append("caused by:\n");
            sb.append(buildThrowableMessage(th.getCause()));
        }
        return sb.toString();
    }

}
