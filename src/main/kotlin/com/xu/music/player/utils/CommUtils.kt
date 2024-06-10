package com.xu.music.player.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 通用工具
 *
 * @date 2024年6月10日15点30分
 * @since V1.0.0.0
 */
object CommUtils {

    private const val FORMAT_DATE: String = "yyyy-MM-dd"
    private const val FORMAT_TIME: String = "HH:mm:ss"
    private const val FORMAT_DATE_TIME: String = "yyyy-MM-dd HH:mm:ss"

    /**
     * 时间日期转换字符串
     *
     * @param date 日期
     * @return 字符串
     * @date 2024年6月7日12点55分
     * @since idea
     */
    fun formatDateTime(date: Any?): String? {
        if (null == date) {
            return null
        }
        return when (date.javaClass.simpleName) {
            "Date" -> (date as Date).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                .format(DateTimeFormatter.ofPattern(FORMAT_DATE_TIME))

            "LocalDate" -> (date as LocalDate).format(DateTimeFormatter.ofPattern(FORMAT_DATE))
            "LocalTime" -> (date as LocalTime).format(DateTimeFormatter.ofPattern(FORMAT_TIME))
            "LocalDateTime" -> (date as LocalDateTime).format(DateTimeFormatter.ofPattern(FORMAT_DATE_TIME))
            else -> null
        }
    }
}
