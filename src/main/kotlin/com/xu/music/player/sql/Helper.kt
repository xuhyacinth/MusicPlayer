package com.xu.music.player.sql

import java.sql.Connection

/**
 * 数据库操作
 *
 * @date 2024年6月10日15点30分
 * @since V1.0.0.0
 */
interface Helper {

    /**
     * 获取连接
     *
     * @return Connection
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    val conn: Connection?

    /**
     * 插入数据
     *
     * @param sql    全sql
     * @param params 数据
     * @return 结果
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    fun insert(sql: String?, vararg params: Any?): Int

    /**
     * 更新数据
     *
     * @param sql    全sql
     * @param params 数据
     * @return 结果
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    fun update(sql: String?, vararg params: Any?): Int

    /**
     * 删除数据
     *
     * @param sql    全sql
     * @param params 数据
     * @return 结果
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    fun delete(sql: String?, vararg params: Any?): Int

    /**
     * 查询数据
     *
     * @param sql    全sql
     * @param cls    类
     * @param params 数据
     * @return 结果
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    fun <T> select(sql: String?, cls: Class<T>?, vararg params: Any?): List<T>?

    /**
     * 查询数据
     *
     * @param sql    全sql
     * @param params 数据
     * @return 结果
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    fun select(sql: String?, vararg params: Any?): List<Map<String?, Any?>?>?

}
