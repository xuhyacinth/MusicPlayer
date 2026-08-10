package com.xu.music.player.wrapper;

import cn.hutool.core.util.StrUtil;
import com.xu.music.player.hander.DataBaseError;
import com.xu.music.player.wrapper.sql.Helper;
import com.xu.music.player.wrapper.sql.NewHelper;
import com.xu.music.player.wrapper.sql.SqlCommand;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 参数化插入 Wrapper。
 */
public class InsertWrapper<T> extends BasicWrapper<T> {

    private final T data;

    public InsertWrapper(T data, String table) {
        if (data == null || StrUtil.isBlank(table)) {
            throw new DataBaseError("参数错误！");
        }
        this.data = data;
        this.table = table;
    }

    public SqlCommand command() {
        try {
            var fields = new ArrayList<String>();
            var values = new ArrayList<>();
            for (Field field : data.getClass().getDeclaredFields()) {
                if (StrUtil.equals("serialVersionUID", field.getName())) {
                    continue;
                }
                field.setAccessible(true);
                var value = field.get(data);
                if (value != null) {
                    fields.add(dealField(field.getName()));
                    values.add(value);
                }
            }

            var placeholders = String.join(",", Collections.nCopies(fields.size(), "?"));
            var sql = "insert into " + table + "(" + String.join(", ", fields) + ") values(" + placeholders + ")";
            return new SqlCommand(sql, List.copyOf(values));
        } catch (IllegalAccessException exception) {
            throw new DataBaseError(exception.getMessage(), exception);
        }
    }

    public int insert() throws DataBaseError {
        Helper helper = new NewHelper();
        var command = command();
        return helper.insert(command.sql(), command.parameterArray());
    }
}
