package com.xu.music.player.wrapper.sql;

import java.util.List;
import java.util.Map;

import java.sql.Connection;

/**
 * 数据库操作
 *
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public interface Helper {

    /**
     * 获取连接
     *
     * @return Connection
     * @date 2024年6月4日19点07分
     * @since idea
     */
    Connection getConn();

    /**
     * 插入数据
     *
     * @param sql    全sql
     * @param params 数据
     * @return 结果
     * @date 2024年6月4日19点07分
     * @since idea
     */
    int insert(String sql, Object... params);

    /**
     * 更新数据
     *
     * @param sql    全sql
     * @param params 数据
     * @return 结果
     * @date 2024年6月4日19点07分
     * @since idea
     */
    int update(String sql, Object... params);

    /**
     * 删除数据
     *
     * @param sql    全sql
     * @param params 数据
     * @return 结果
     * @date 2024年6月4日19点07分
     * @since idea
     */
    int delete(String sql, Object... params);

    /**
     * 查询数据
     *
     * @param sql    全sql
     * @param cls    类
     * @param params 数据
     * @return 结果
     * @date 2024年6月4日19点07分
     * @since idea
     */
    <T> List<T> select(String sql, Class<T> cls, Object... params);

    /**
     * 查询数据
     *
     * @param sql    全sql
     * @param params 数据
     * @return 结果
     * @date 2024年6月4日19点07分
     * @since idea
     */
    List<Map<String, Object>> select(String sql, Object... params);

}
