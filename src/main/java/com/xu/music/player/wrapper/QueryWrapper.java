package com.xu.music.player.wrapper;

import com.xu.music.player.wrapper.sql.Helper;
import com.xu.music.player.wrapper.sql.NewHelper;
import com.xu.music.player.wrapper.sql.SqlCommand;

import java.util.Arrays;
import java.util.List;

/**
 * 参数化查询 Wrapper。
 */
public class QueryWrapper<T> extends BasicWrapper<T> {

    public QueryWrapper(Class<T> bean, String table, String... field) {
        this.bean = bean;
        this.table = requireIdentifier(table);
        this.field = field == null || field.length == 0
                ? new String[]{"*"}
                : Arrays.stream(field)
                .map(value -> "*".equals(value) ? value : dealField(value))
                .toArray(String[]::new);
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

    public QueryWrapper<T> eq(String field, Object value) {
        addCondition(dealField(field) + " = ?", value);
        return this;
    }

    public QueryWrapper<T> eq(boolean condition, String field, Object value) {
        return condition ? eq(field, value) : this;
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
