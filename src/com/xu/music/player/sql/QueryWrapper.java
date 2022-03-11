package com.xu.music.player.sql;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollectionUtil;

/**
 * @author Administrator
 */
public class QueryWrapper<T> {

    private Class<T> bean;

    private String last;

    private String table;

    private String[] field;

    private List<String> condition = new LinkedList<>();

    public QueryWrapper() {
    }

    public QueryWrapper(Class<T> bean, String table, String... field) {
        this.bean = bean;
        this.table = table;
        this.field = (null == field || field.length <= 0) ? new String[]{"*"} : field;
    }

    public QueryWrapper eq(String filed, Object value) {
        this.condition.add(" and " + filed + " = " + value);
        return this;
    }

    public QueryWrapper like(String filed, Object value) {
        this.condition.add(" and " + filed + " = " + value);
        return this;
    }

    public QueryWrapper last(String value) {
        this.last = " " + value;
        return this;
    }

    public List<T> list() {
        String sql = "select " + Arrays.asList(field).stream().collect(Collectors.joining(",")) + " from " + table + " where 1 = 1 ";
        sql += CollectionUtil.isEmpty(condition) ? "" : condition.stream().collect(Collectors.joining(" "));
        sql += null == last ? "" : last;
        Helper helper = new Helper();
        return helper.queryBeans(sql, bean);
    }

}
