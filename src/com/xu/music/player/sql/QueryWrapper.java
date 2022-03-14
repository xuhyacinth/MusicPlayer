package com.xu.music.player.sql;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollectionUtil;

/**
 * @author Administrator
 */
public class QueryWrapper<T> extends BasicWrapper {

    public QueryWrapper() {
    }

    public QueryWrapper(Class<T> bean, String table, String... field) {
        super.bean = bean;
        super.table = table;
        super.field = (null == field || field.length <= 0) ? new String[]{"*"} : field;
    }

    public List<T> list() {
        String sql = "select " + Arrays.asList(super.field).stream().collect(Collectors.joining(",")) + " from " + super.table + " where 1 = 1 ";
        sql += CollectionUtil.isEmpty(super.condition) ? "" : super.condition.stream().collect(Collectors.joining(" "));
        sql += null == super.last ? "" : super.last;
        Helper helper = new Helper();
        return helper.queryBeans(sql, super.bean);
    }

    public QueryWrapper eq(String filed, Object value) {
        super.condition.add(" and " + filed + " = " + value);
        return this;
    }

    public QueryWrapper last(String value) {
        super.last = " " + value;
        return this;
    }

    public QueryWrapper like(String filed, Object value) {
        super.condition.add(" and " + filed + " like %" + value + "%");
        return this;
    }

    public QueryWrapper likeLeft(String filed, Object value) {
        super.condition.add(" and " + filed + " like %" + value + "%");
        return this;
    }

    public QueryWrapper likeRight(String filed, Object value) {
        super.condition.add(" and " + filed + " like %" + value + "%");
        return this;
    }

}
