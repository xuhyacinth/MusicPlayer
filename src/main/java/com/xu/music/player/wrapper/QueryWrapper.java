package com.xu.music.player.wrapper;


import cn.hutool.core.collection.CollectionUtil;
import com.xu.music.player.sql.Helper;
import com.xu.music.player.sql.NewHelper;

import java.util.List;


/**
 * 查询
 *
 * @param <T>
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class QueryWrapper<T> extends BasicWrapper<T> {

    public QueryWrapper() {
    }

    public QueryWrapper(Class<T> bean, String table, String... field) {
        super.bean = bean;
        super.table = table;
        super.field = (null == field || field.length == 0) ? new String[]{"*"} : field;
    }

    public List<T> list() {
        String sql = "select " + String.join(",", super.field) + " from " + super.table + " where 1 = 1 ";
        sql += CollectionUtil.isEmpty(super.condition) ? "" : String.join(" ", super.condition);
        sql += null == super.last ? "" : super.last;
        Helper helper = new NewHelper();
        return helper.select(sql, super.bean);
    }


    /**
     * 自定义SQL
     *
     * @param sql sql
     * @return QueryWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public QueryWrapper<T> apply(String sql) {
        super.condition.add(" and (" + sql + ")");
        return this;
    }

    /**
     * 自定义SQL
     *
     * @param cond 条件
     * @param sql  sql
     * @return QueryWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public QueryWrapper<T> apply(boolean cond, String sql) {
        return cond ? apply(sql) : this;
    }

    /**
     * 相等
     *
     * @param filed 字段
     * @param value 值
     * @return QueryWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public QueryWrapper<T> eq(String filed, Object value) {
        super.condition.add(" and " + filed + " = " + value);
        return this;
    }

    /**
     * 相等
     *
     * @param cond  条件
     * @param filed 字段
     * @param value 值
     * @return QueryWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public QueryWrapper<T> eq(boolean cond, String filed, Object value) {
        return cond ? eq(filed, value) : this;
    }

    /**
     * 最后执行SQL
     *
     * @param sql sql
     * @return QueryWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public QueryWrapper<T> last(String sql) {
        super.last = " " + sql;
        return this;
    }

    /**
     * 最后执行SQL
     *
     * @param cond 条件
     * @param sql  sql
     * @return QueryWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public QueryWrapper<T> last(boolean cond, String sql) {
        return cond ? last(sql) : this;
    }

    /**
     * 相似
     *
     * @param filed 字段
     * @param value 值
     * @return QueryWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public QueryWrapper<T> like(String filed, Object value) {
        super.condition.add(" and " + filed + " like %" + value + "%");
        return this;
    }

    /**
     * 相似
     *
     * @param cond  条件
     * @param filed 字段
     * @param value 值
     * @return QueryWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public QueryWrapper<T> like(boolean cond, String filed, Object value) {
        return cond ? like(filed, value) : this;
    }

    /**
     * 左相似
     *
     * @param filed 字段
     * @param value 值
     * @return QueryWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public QueryWrapper<T> likeLeft(String filed, Object value) {
        super.condition.add(" and " + filed + " like %" + value + "%");
        return this;
    }

    /**
     * 左相似
     *
     * @param cond  条件
     * @param filed 字段
     * @param value 值
     * @return QueryWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public QueryWrapper<T> likeLeft(boolean cond, String filed, Object value) {
        return cond ? likeLeft(filed, value) : this;
    }

    /**
     * 右相似
     *
     * @param filed 字段
     * @param value 值
     * @return QueryWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public QueryWrapper<T> likeRight(String filed, Object value) {
        super.condition.add(" and " + filed + " like %" + value + "%");
        return this;
    }

    /**
     * 右相似
     *
     * @param cond  条件
     * @param filed 字段
     * @param value 值
     * @return QueryWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public QueryWrapper<T> likeRight(boolean cond, String filed, Object value) {
        return cond ? likeRight(filed, value) : this;
    }

}
