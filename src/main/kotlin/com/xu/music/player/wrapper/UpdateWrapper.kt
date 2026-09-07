package com.xu.music.player.wrapper

import com.xu.music.player.hander.DataBaseError
import com.xu.music.player.sql.SQLiteHelper

/** 保留非空属性更新与链式条件接口。 */
class UpdateWrapper<T : Any>(private val data: T, table: String) : BasicWrapper<T>() {
    init {
        if (table.isBlank()) throw DataBaseError("参数错误")
        this.table = table
    }

    fun update(): Int {
        val assignments = values(data).joinToString(", ") { "${it.first} = ${dealValue(it.second)}" }
        return SQLiteHelper().update("update $table set $assignments" + whereSql())
    }

    fun insert(): Int = SQLiteHelper().insert(insertSql(data))

    // 参数为旧接口兼容保留，尾部 SQL 仍由 last() 配置。
    @Suppress("UNUSED_PARAMETER")
    fun delete(last: String): Int = SQLiteHelper().delete("delete from $table" + whereSql())

    fun apply(sql: String): UpdateWrapper<T> = apply { condition.add(" and ($sql)") }

    fun apply(cond: Boolean, sql: String): UpdateWrapper<T> = if (cond) apply(sql) else this

    fun eq(field: String, value: Any?): UpdateWrapper<T> = apply { condition.add(" and $field = ${dealValue(value)}") }

    fun eq(cond: Boolean, field: String, value: Any?): UpdateWrapper<T> = if (cond) eq(field, value) else this

    fun last(sql: String): UpdateWrapper<T> = apply { last = " $sql" }

    fun last(cond: Boolean, sql: String): UpdateWrapper<T> = if (cond) last(sql) else this

    fun like(field: String, value: Any?): UpdateWrapper<T> = apply { condition.add(" and $field like ${dealValue("%$value%")}") }

    fun like(cond: Boolean, field: String, value: Any?): UpdateWrapper<T> = if (cond) like(field, value) else this

    fun likeLeft(field: String, value: Any?): UpdateWrapper<T> = apply { condition.add(" and $field like ${dealValue("%$value")}") }

    fun likeLeft(cond: Boolean, field: String, value: Any?): UpdateWrapper<T> = if (cond) likeLeft(field, value) else this

    fun likeRight(field: String, value: Any?): UpdateWrapper<T> = apply { condition.add(" and $field like ${dealValue("$value%")}") }

    fun likeRight(cond: Boolean, field: String, value: Any?): UpdateWrapper<T> = if (cond) likeRight(field, value) else this

}
