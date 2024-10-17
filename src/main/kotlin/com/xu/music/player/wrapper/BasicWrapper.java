package com.xu.music.player.wrapper;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import cn.hutool.core.util.StrUtil;

import com.xu.music.player.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基础类
 *
 * @param <T>
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class BasicWrapper<T> {

    /**
     * 最后SQL
     */
    protected String last;

    /**
     * 类
     */
    protected Class<T> bean;

    /**
     * 表
     */
    protected String table;

    /**
     * 字段
     */
    protected String[] field;

    /**
     * 条件
     */
    protected List<String> condition = new LinkedList<>();

    /**
     * 填充值
     *
     * @param list 值
     * @return 结果
     * @date 2024年6月6日20点10分
     * @since idea
     */
    protected String dealValue(List<Object> list) {
        return Optional.ofNullable(list).orElse(new ArrayList<>()).stream().map(item -> {
            if (null == item) {
                return "null";
            }
            if (item instanceof Date) {
                SimpleDateFormat format = new SimpleDateFormat(Utils.FORMAT_DATE_TIME);
                return "'" + format.format(item) + "'";
            } else if (item instanceof LocalDateTime) {
                DateTimeFormatter format = DateTimeFormatter.ofPattern(Utils.FORMAT_DATE_TIME);
                return "'" + format.format((LocalDateTime) item) + "'";
            } else if (item instanceof String) {
                return "'" + item + "'";
            }
            return String.valueOf(item);
        }).collect(Collectors.joining(","));
    }

    /**
     * 填充值
     *
     * @param value 值
     * @return 结果
     * @date 2024年6月6日20点10分
     * @since idea
     */
    protected String dealValue(Object value) {
        if (null == value) {
            return "null";
        }

        if (value instanceof Date) {
            SimpleDateFormat format = new SimpleDateFormat(Utils.FORMAT_DATE_TIME);
            return "'" + format.format(value) + "'";
        } else if (value instanceof LocalDateTime) {
            DateTimeFormatter format = DateTimeFormatter.ofPattern(Utils.FORMAT_DATE_TIME);
            return "'" + format.format((LocalDateTime) value) + "'";
        } else if (value instanceof String) {
            return "'" + value + "'";
        }

        return String.valueOf(value);
    }

    /**
     * 字段转换
     *
     * @param name 字段名称
     * @return 结果
     * @date 2024年6月6日20点10分
     * @since idea
     */
    protected String dealField(String name) {
        Matcher matcher = Pattern.compile("[A-Z]").matcher(name);
        while (matcher.find()) {
            name = name.replace(matcher.group(), "_" + matcher.group().toLowerCase(Locale.ROOT));
        }
        if (StrUtil.equalsAnyIgnoreCase(name, "index")) {
            return "`" + name + "`";
        }
        return name;
    }

}
