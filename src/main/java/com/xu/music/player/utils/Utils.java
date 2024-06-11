package com.xu.music.player.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.Date;

/**
 * 通用工具
 *
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class Utils {

    public static final String FORMAT_DATE = "yyyy-MM-dd";
    public static final String FORMAT_TIME = "HH:mm:ss";
    public static final String FORMAT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    /**
     * 时间日期转换字符串
     *
     * @param date 日期
     * @return 字符串
     * @date 2024年6月7日12点55分
     * @since idea
     */
    public static String formatDateTime(Object date) {
        if (null == date) {
            return null;
        }
        switch (date.getClass().getSimpleName()) {
            case "Date":
                return ((Date) date).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern(FORMAT_DATE_TIME));
            case "LocalDate":
                return ((LocalDate) date).format(DateTimeFormatter.ofPattern(FORMAT_DATE));
            case "LocalTime":
                return ((LocalTime) date).format(DateTimeFormatter.ofPattern(FORMAT_TIME));
            case "LocalDateTime":
                return ((LocalDateTime) date).format(DateTimeFormatter.ofPattern(FORMAT_DATE_TIME));
            default:
                return null;
        }
    }

}
