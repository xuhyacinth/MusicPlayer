package com.xu.music.player.wrapper

import com.xu.music.player.sql.SQLiteHelper

/** 保留链式调用方式的 Kotlin 查询封装。 */
class QueryWrapper<T>() : BasicWrapper<T>() {
    constructor(bean: Class<T>, table: String, vararg field: String) : this() {
        this.bean = bean
        this.table = table
        fields = field.toList().ifEmpty { listOf("*") }
    }

    fun list(): List<T> = SQLiteHelper().select(
        "select ${fields.joinToString(",")} from $table" + whereSql(), bean
    )

    fun apply(sql: String): QueryWrapper<T> = apply { condition.add(" and ($sql)") }

    fun apply(cond: Boolean, sql: String): QueryWrapper<T> = if (cond) apply(sql) else this

    fun eq(field: String, value: Any?): QueryWrapper<T> = apply { condition.add(" and $field = ${dealValue(value)}") }

    fun eq(cond: Boolean, field: String, value: Any?): QueryWrapper<T> = if (cond) eq(field, value) else this

    fun last(sql: String): QueryWrapper<T> = apply { last = " $sql" }

    fun last(cond: Boolean, sql: String): QueryWrapper<T> = if (cond) last(sql) else this

    fun like(field: String, value: Any?): QueryWrapper<T> = apply { condition.add(" and $field like ${dealValue("%$value%")}") }

    fun like(cond: Boolean, field: String, value: Any?): QueryWrapper<T> = if (cond) like(field, value) else this

    fun likeLeft(field: String, value: Any?): QueryWrapper<T> = apply { condition.add(" and $field like ${dealValue("%$value")}") }

    fun likeLeft(cond: Boolean, field: String, value: Any?): QueryWrapper<T> = if (cond) likeLeft(field, value) else this

    fun likeRight(field: String, value: Any?): QueryWrapper<T> = apply { condition.add(" and $field like ${dealValue("$value%")}") }

    fun likeRight(cond: Boolean, field: String, value: Any?): QueryWrapper<T> = if (cond) likeRight(field, value) else this

}
