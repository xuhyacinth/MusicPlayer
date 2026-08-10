package com.xu.music.player.wrapper;

import com.xu.music.player.wrapper.sql.Helper;
import com.xu.music.player.wrapper.sql.NewHelper;
import com.xu.music.player.wrapper.sql.SqlCommand;

import java.util.List;

/**
 * 参数化查询 Wrapper。
 */
public class QueryWrapper<T> extends BasicWrapper<T> {

    public QueryWrapper() {
    }

    public QueryWrapper(Class<T> bean, String table, String... field) {
        this.bean = bean;
        this.table = table;
        this.field = field == null || field.length == 0 ? new String[]{"*"} : field;
    }

    public SqlCommand command() {
        var sql = "select " + String.join(",", field) + " from " + table + " where 1 = 1";
        return command(sql, List.of());
    }

    public List<T> list() {
        Helper helper = new NewHelper();
        var command = command();
        return helper.select(command.sql(), bean, command.parameterArray());
    }

    public QueryWrapper<T> apply(String sql, Object... values) {
        addCondition("(" + sql + ")", values);
        return this;
    }

    public QueryWrapper<T> apply(boolean condition, String sql, Object... values) {
        return condition ? apply(sql, values) : this;
    }

    public QueryWrapper<T> eq(String field, Object value) {
        addCondition(dealField(field) + " = ?", value);
        return this;
    }

    public QueryWrapper<T> eq(boolean condition, String field, Object value) {
        return condition ? eq(field, value) : this;
    }

    public QueryWrapper<T> last(String sql) {
        this.last = sql;
        return this;
    }

    public QueryWrapper<T> last(boolean condition, String sql) {
        return condition ? last(sql) : this;
    }

    public QueryWrapper<T> like(String field, Object value) {
        addCondition(dealField(field) + " like ?", "%" + value + "%");
        return this;
    }

    public QueryWrapper<T> like(boolean condition, String field, Object value) {
        return condition ? like(field, value) : this;
    }

    public QueryWrapper<T> likeLeft(String field, Object value) {
        addCondition(dealField(field) + " like ?", "%" + value);
        return this;
    }

    public QueryWrapper<T> likeLeft(boolean condition, String field, Object value) {
        return condition ? likeLeft(field, value) : this;
    }

    public QueryWrapper<T> likeRight(String field, Object value) {
        addCondition(dealField(field) + " like ?", value + "%");
        return this;
    }

    public QueryWrapper<T> likeRight(boolean condition, String field, Object value) {
        return condition ? likeRight(field, value) : this;
    }
}
