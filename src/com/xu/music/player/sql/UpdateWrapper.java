package com.xu.music.player.sql;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import cn.hutool.core.collection.CollectionUtil;

public class UpdateWrapper<T> extends BasicWrapper {

    private T t;

    public UpdateWrapper(T t, String table) {
        if (null == t || StringUtils.isBlank(table)) {
            throw new RuntimeException("参数错误");
        }
        this.t = t;
        this.table = table;
    }

    public int update() throws Exception {
        String sql = dealSQL(false);
        sql += CollectionUtil.isEmpty(super.condition) ? "" : super.condition.stream().collect(Collectors.joining(" "));
        sql += null == super.last ? "" : super.last;
        Helper helper = new Helper();
        return helper.update(sql);
    }

    public int insert() throws Exception {
        String sql = dealSQL(true);
        Helper helper = new Helper();
        return helper.update(sql);
    }

    public int delete(String last) throws Exception {
        String sql = "delete from " + super.table + " where 1 = 1 ";
        sql += CollectionUtil.isEmpty(super.condition) ? "" : super.condition.stream().collect(Collectors.joining(" "));
        sql += null == super.last ? "" : super.last;
        Helper helper = new Helper();
        return helper.update(sql);
    }

    private String dealSQL(boolean insert) throws Exception {
        if (insert) {
            return insertData();
        }
        return updateData();
    }

    /**
     * update
     *
     * @return
     * @throws IllegalAccessException
     */
    private String updateData() throws IllegalAccessException {
        Field[] fields = this.t.getClass().getDeclaredFields();
        String sql = "update " + super.table + " set ";
        List<String> modify = new LinkedList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(t);
            if (null != value) {
                modify.add(dealField(field.getName()) + " = " + dealValue(field.get(t)));
            }
        }
        sql = sql + String.join(", ", modify);
        return sql;
    }

    /**
     * insert
     *
     * @return
     * @throws IllegalAccessException
     */
    private String insertData() throws IllegalAccessException {
        Field[] fields = this.t.getClass().getDeclaredFields();
        String sql = "insert into " + super.table + "";
        List<String> fieldsList = new LinkedList<>();
        List<Object> valuesList = new LinkedList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(t);
            if (null != value) {
                fieldsList.add(dealField(field.getName()));
                valuesList.add(field.get(t));
            }
        }
        sql = sql + "(" + String.join(", ", fieldsList) + ") values(" + dealValue(valuesList) + ")";
        return sql;
    }

    private String dealSQL(List<Object> values) throws IllegalAccessException {
        Field[] fields = this.t.getClass().getDeclaredFields();
        String sql = "insert into " + super.table + "";
        List<String> fieldsList = new LinkedList<>();
        List<String> valuesList = new LinkedList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(this.t);
            if (null != value) {
                fieldsList.add(dealField(field.getName()));
                values.add(field.get(this.t));
                valuesList.add("?");
            }
        }
        sql = sql + "(" + String.join(", ", fieldsList) + ") values(" + String.join(", ", valuesList) + ")";
        return sql;
    }

    private String dealValue(List<Object> valuesList) {
        return Optional.ofNullable(valuesList).orElse(new ArrayList<>()).stream().map(item -> {
            if (null == item) {
                return "null";
            }
            if (item instanceof LocalDateTime) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return "'" + formatter.format((LocalDateTime) item) + "'";
            } else if (item instanceof String) {
                return "'" + item + "'";
            }
            return (String) item;
        }).collect(Collectors.joining(","));
    }

    private String dealValue(Object value) {
        if (null == value) {
            return "null";
        }
        if (value instanceof LocalDateTime) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return "'" + formatter.format((LocalDateTime) value) + "'";
        } else if (value instanceof String) {
            return "'" + value + "'";
        }
        return (String) value;
    }

    private String dealField(String fieldName) {
        Matcher matcher = Pattern.compile("[A-Z]").matcher(fieldName);
        while (matcher.find()) {
            fieldName = fieldName.replace(matcher.group(), "_" + matcher.group().toLowerCase(Locale.ROOT));
        }
        return fieldName;
    }

    public UpdateWrapper eq(String filed, Object value) {
        super.condition.add(" and " + filed + " = " + value);
        return this;
    }

    public UpdateWrapper last(String value) {
        super.last = " " + value;
        return this;
    }

    public UpdateWrapper like(String filed, Object value) {
        super.condition.add(" and " + filed + " like %" + value + "%");
        return this;
    }

    public UpdateWrapper likeLeft(String filed, Object value) {
        super.condition.add(" and " + filed + " like %" + value + "%");
        return this;
    }

    public UpdateWrapper likeRight(String filed, Object value) {
        super.condition.add(" and " + filed + " like %" + value + "%");
        return this;
    }

}
