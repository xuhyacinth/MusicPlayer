package com.xu.music.player.wrapper

import cn.hutool.core.util.StrUtil
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * 基础类
 *
 * @param <T>
 * @date 2024年6月10日15点30分
 * @since V1.0.0.0
</T> */
open class BasicWrapper<T> {
    
    /**
     * 最后SQL
     */
    protected var last: String? = null

    /**
     * 类
     */
    protected var bean: Class<T>? = null

    /**
     * 表
     */
    protected var table: String? = null

    /**
     * 字段
     */
    protected lateinit var field: Array<String>

    /**
     * 条件
     */
    protected var condition: List<String> = LinkedList()

    /**
     * 填充值
     *
     * @param list 值
     * @return 结果
     * @date 2024年6月6日20点10分
     * @since V1.0.0.0
     */
    protected fun dealValue(list: List<Any>): String {
        return Optional.ofNullable(list).orElse(ArrayList()).stream().map { item: Any? ->
            if (null == item) {
                return@map "null"
            }
            if (item is Date) {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                return@map "'" + format.format(item) + "'"
            } else if (item is LocalDateTime) {
                val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                return@map "'" + format.format(item as LocalDateTime?) + "'"
            } else if (item is String) {
                return@map "'$item'"
            }
            item.toString()
        }.collect(Collectors.joining(","))
    }

    /**
     * 填充值
     *
     * @param value 值
     * @return 结果
     * @date 2024年6月6日20点10分
     * @since V1.0.0.0
     */
    protected fun dealValue(value: Any?): String {
        if (null == value) {
            return "null"
        }
        if (value is Date) {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            return "'" + format.format(value) + "'"
        } else if (value is LocalDateTime) {
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            return "'" + format.format(value as LocalDateTime?) + "'"
        } else if (value is String) {
            return "'$value'"
        }
        return value as String
    }

    /**
     * 字段转换
     *
     * @param name 字段名称
     * @return 结果
     * @date 2024年6月6日20点10分
     * @since V1.0.0.0
     */
    protected fun dealField(name: String): String {
        var name = name
        val matcher = Pattern.compile("[A-Z]").matcher(name)
        while (matcher.find()) {
            name = name.replace(matcher.group(), "_" + matcher.group().lowercase())
        }
        if (StrUtil.equalsAnyIgnoreCase(name, "index")) {
            return "`$name`"
        }
        return name
    }

}
