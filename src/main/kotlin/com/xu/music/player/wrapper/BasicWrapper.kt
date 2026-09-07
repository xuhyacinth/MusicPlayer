package com.xu.music.player.wrapper

import java.lang.reflect.Modifier
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/** 数据库操作的字段与值转换。 */
open class BasicWrapper<T> {
    protected var last: String = ""
    protected lateinit var bean: Class<T>
    protected lateinit var table: String
    protected var fields: List<String> = listOf("*")
    protected val condition = mutableListOf<String>()

    protected fun dealValue(values: List<Any?>): String = values.joinToString(",") { dealValue(it) }

    protected fun dealValue(value: Any?): String = when (value) {
        null -> "null"
        is Date -> "'${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value)}'"
        is LocalDateTime -> "'${value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}'"
        is String -> "'${value.replace("'", "''")}'"
        else -> value.toString()
    }

    protected fun dealField(name: String): String {
        val column = Regex("[A-Z]").replace(name) { "_" + it.value.lowercase(Locale.ROOT) }
        return if (column.equals("index", ignoreCase = true)) "`$column`" else column
    }

    /** 忽略静态字段和空值，保留原有仅写入非空属性的约定。 */
    protected fun values(data: Any): List<Pair<String, Any>> = data.javaClass.declaredFields
        .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }
        .mapNotNull { field ->
            field.isAccessible = true
            field.get(data)?.let { dealField(field.name) to it }
        }

    protected fun insertSql(data: Any): String {
        val values = values(data)
        return "insert into $table(${values.joinToString(", ") { it.first }}) " +
            "values(${dealValue(values.map { it.second })})"
    }

    protected fun whereSql(): String = " where 1 = 1 " + condition.joinToString(" ") + last
}
