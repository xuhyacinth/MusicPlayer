package com.xu.music.player.wrapper;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.xu.music.player.hander.DataBaseError;
import com.xu.music.player.wrapper.sql.Helper;
import com.xu.music.player.wrapper.sql.NewHelper;
import java.lang.reflect.Field;

import java.util.LinkedList;
import java.util.List;

/**
 * 更新
 *
 * @param <T>
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class UpdateWrapper<T> extends BasicWrapper<T> {

    private final T data;

    public UpdateWrapper(T data, String table) {
        if (null == data || StrUtil.isBlank(table)) {
            throw new DataBaseError("参数错误");
        }
        this.data = data;
        this.table = table;
    }

    public int update() throws Exception {
        String sql = sql(false);
        sql += CollectionUtil.isEmpty(super.condition) ? "" : String.join(" ", super.condition);
        sql += null == super.last ? "" : super.last;
        Helper helper = new NewHelper();
        return helper.update(sql);
    }

    public int insert() throws Exception {
        String sql = sql(true);
        Helper helper = new NewHelper();
        return helper.update(sql);
    }

    public int delete(String last) {
        String sql = "delete from " + super.table + " where 1 = 1 ";
        sql += CollectionUtil.isEmpty(super.condition) ? "" : String.join(" ", super.condition);
        sql += null == super.last ? "" : super.last;
        Helper helper = new NewHelper();
        return helper.update(sql);
    }

    private String sql(boolean insert) throws Exception {
        if (insert) {
            return add();
        }
        return modify();
    }

    /**
     * 更新语句
     *
     * @return 更新SQL
     * @throws Exception 异常
     * @date 2024年6月4日19点07分
     * @since idea
     */
    private String modify() throws Exception {
        Field[] fields = this.data.getClass().getDeclaredFields();
        String sql = "update " + super.table + " set ";
        List<String> modify = new LinkedList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(data);
            if (null != value) {
                modify.add(dealField(field.getName()) + " = " + dealValue(field.get(data)));
            }
        }
        sql = sql + String.join(", ", modify);
        return sql;
    }

    /**
     * 插入语句
     *
     * @return 插入SQL
     * @throws Exception 异常
     * @date 2024年6月4日19点07分
     * @since idea
     */
    private String add() throws Exception {
        Field[] fields = this.data.getClass().getDeclaredFields();
        String sql = "insert into " + super.table;
        List<String> fieldsList = new LinkedList<>();
        List<Object> valuesList = new LinkedList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(data);
            if (null != value) {
                fieldsList.add(dealField(field.getName()));
                valuesList.add(field.get(data));
            }
        }
        sql = sql + "(" + String.join(", ", fieldsList) + ") values(" + dealValue(valuesList) + ")";
        return sql;
    }

    /**
     * 自定义SQL
     *
     * @param sql sql
     * @return UpdateWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public UpdateWrapper<T> apply(String sql) {
        super.condition.add(" and (" + sql + ")");
        return this;
    }

    /**
     * 自定义SQL
     *
     * @param cond 条件
     * @param sql  sql
     * @return UpdateWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public UpdateWrapper<T> apply(boolean cond, String sql) {
        return cond ? apply(sql) : this;
    }

    /**
     * 相等
     *
     * @param filed 字段
     * @param value 值
     * @return UpdateWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public UpdateWrapper<T> eq(String filed, Object value) {
        super.condition.add(" and " + filed + " = " + value);
        return this;
    }

    /**
     * 相等
     *
     * @param cond  条件
     * @param filed 字段
     * @param value 值
     * @return UpdateWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public UpdateWrapper<T> eq(boolean cond, String filed, Object value) {
        return cond ? eq(filed, value) : this;
    }

    /**
     * 最后执行SQL
     *
     * @param sql sql
     * @return UpdateWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public UpdateWrapper<T> last(String sql) {
        super.last = " " + sql;
        return this;
    }

    /**
     * 最后执行SQL
     *
     * @param cond 条件
     * @param sql  sql
     * @return UpdateWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public UpdateWrapper<T> last(boolean cond, String sql) {
        return cond ? last(sql) : this;
    }

    /**
     * 相似
     *
     * @param filed 字段
     * @param value 值
     * @return UpdateWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public UpdateWrapper<T> like(String filed, Object value) {
        super.condition.add(" and " + filed + " like %" + value + "%");
        return this;
    }

    /**
     * 相似
     *
     * @param cond  条件
     * @param filed 字段
     * @param value 值
     * @return UpdateWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public UpdateWrapper<T> like(boolean cond, String filed, Object value) {
        return cond ? like(filed, value) : this;
    }

    /**
     * 左相似
     *
     * @param filed 字段
     * @param value 值
     * @return UpdateWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public UpdateWrapper<T> likeLeft(String filed, Object value) {
        super.condition.add(" and " + filed + " like %" + value + "%");
        return this;
    }

    /**
     * 左相似
     *
     * @param cond  条件
     * @param filed 字段
     * @param value 值
     * @return UpdateWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public UpdateWrapper<T> likeLeft(boolean cond, String filed, Object value) {
        return cond ? likeLeft(filed, value) : this;
    }

    /**
     * 右相似
     *
     * @param filed 字段
     * @param value 值
     * @return UpdateWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public UpdateWrapper<T> likeRight(String filed, Object value) {
        super.condition.add(" and " + filed + " like %" + value + "%");
        return this;
    }

    /**
     * 右相似
     *
     * @param cond  条件
     * @param filed 字段
     * @param value 值
     * @return UpdateWrapper<T>
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public UpdateWrapper<T> likeRight(boolean cond, String filed, Object value) {
        return cond ? likeRight(filed, value) : this;
    }

}
