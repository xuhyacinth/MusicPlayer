package com.xu.music.player.wrapper;


import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.xu.music.player.hander.DataBaseError;
import com.xu.music.player.sql.Helper;
import com.xu.music.player.sql.SQLiteHelper;

/**
 * 插入
 *
 * @param <T>
 * @date 2024年6月4日19点07分
 * @since idea
 */
public class InsertWrapper<T> extends BasicWrapper<T> {

    private final T data;

    public InsertWrapper(T t, String table) {
        if (null == t || StrUtil.isBlank(table)) {
            throw new DataBaseError("参数错误！");
        }
        this.data = t;
        this.table = table;
    }

    public int insert() throws DataBaseError {
        String sql = sql();
        Helper helper = new SQLiteHelper();
        return helper.insert(sql);
    }

    private String sql() throws DataBaseError {
        try {
            Field[] fields = this.data.getClass().getDeclaredFields();
            String sql = "insert into " + super.table;
            List<String> fieldsList = new LinkedList<>();
            List<Object> valuesList = new LinkedList<>();
            for (Field field : fields) {
                if (StrUtil.equals("serialVersionUID", field.getName())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(data);
                if (null != value) {
                    fieldsList.add(dealField(field.getName()));
                    valuesList.add(field.get(data));
                }
            }
            sql = sql + "(" + String.join(", ", fieldsList) + ") values(" + dealValue(valuesList) + ")";
            return sql;
        } catch (Exception e) {
            throw new DataBaseError(e.getMessage(), e);
        }
    }

}
