package com.xu.music.player.wrapper;

import cn.hutool.core.util.StrUtil;
import com.xu.music.player.hander.DataBaseError;
import com.xu.music.player.wrapper.sql.Helper;
import com.xu.music.player.wrapper.sql.NewHelper;
import com.xu.music.player.wrapper.sql.SqlCommand;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 参数化更新 Wrapper。
 */
public class UpdateWrapper<T> extends BasicWrapper<T> {

    private final T data;
    private final Helper helper;

    public UpdateWrapper(T data, String table) {
        this(data, table, new NewHelper());
    }

    public UpdateWrapper(T data, String table, Helper helper) {
        if (data == null || StrUtil.isBlank(table) || helper == null) {
            throw new DataBaseError("参数错误");
        }
        this.data = data;
        this.table = requireIdentifier(table);
        this.helper = helper;
    }

    public SqlCommand updateCommand() {
        try {
            var assignments = new ArrayList<String>();
            var values = new ArrayList<>();
            for (Field field : data.getClass().getDeclaredFields()) {
                if (StrUtil.equals("serialVersionUID", field.getName())) {
                    continue;
                }
                field.setAccessible(true);
                var value = field.get(data);
                if (value != null) {
                    assignments.add(dealField(field.getName()) + " = ?");
                    values.add(value);
                }
            }
            return command("update " + table + " set " + String.join(", ", assignments), values);
        } catch (IllegalAccessException exception) {
            throw new DataBaseError(exception.getMessage(), exception);
        }
    }

    public SqlCommand insertCommand() {
        return new InsertWrapper<>(data, table).command();
    }

    public SqlCommand deleteCommand() {
        return command("delete from " + table + " where 1 = 1", List.of());
    }

    public int update() {
        var command = updateCommand();
        return helper.update(command.sql(), command.parameterArray());
    }

    public int insert() {
        var command = insertCommand();
        return helper.insert(command.sql(), command.parameterArray());
    }

    public int delete() {
        var command = deleteCommand();
        return helper.delete(command.sql(), command.parameterArray());
    }

    public UpdateWrapper<T> eq(String field, Object value) {
        addCondition(dealField(field) + " = ?", value);
        return this;
    }

    public UpdateWrapper<T> eq(boolean condition, String field, Object value) {
        return condition ? eq(field, value) : this;
    }

    public UpdateWrapper<T> like(String field, Object value) {
        addCondition(dealField(field) + " like ?", "%" + value + "%");
        return this;
    }

    public UpdateWrapper<T> like(boolean condition, String field, Object value) {
        return condition ? like(field, value) : this;
    }

    public UpdateWrapper<T> likeLeft(String field, Object value) {
        addCondition(dealField(field) + " like ?", "%" + value);
        return this;
    }

    public UpdateWrapper<T> likeLeft(boolean condition, String field, Object value) {
        return condition ? likeLeft(field, value) : this;
    }

    public UpdateWrapper<T> likeRight(String field, Object value) {
        addCondition(dealField(field) + " like ?", value + "%");
        return this;
    }

    public UpdateWrapper<T> likeRight(boolean condition, String field, Object value) {
        return condition ? likeRight(field, value) : this;
    }
}
