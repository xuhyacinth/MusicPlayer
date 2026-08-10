package com.xu.music.player.utils

import javafx.scene.image.Image
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.nio.file.Files
import java.nio.file.Paths

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

    private val imageCache = ConcurrentHashMap<String, Image>()

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

    /**
     * 获取图片（带缓存）
     *
     * @param name 图片文件名称
     * @return 图片
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    fun getImage(name: String): Image? {
        imageCache[name]?.let { return it }

        try {
            var stream: InputStream? = CommUtils::class.java.getResourceAsStream("/com/xu/music/player/image/$name")
            if (stream == null) {
                // 回退到物理文件路径加载（便于本地调试）
                val path = Paths.get("src/main/resources/com/xu/music/player/image/$name")
                if (Files.exists(path)) {
                    stream = Files.newInputStream(path)
                }
            }
            if (stream == null) {
                return null
            }
            stream.use { input ->
                val image = Image(input)
                imageCache[name] = image
                return image
            }
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 格式化时间
     *
     * @param time 时间
     * @return 时间
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    fun format(time: Double): String {
        if (time < 10) {
            return "00:0" + String.format("%.1f", time)
        }
        if (time < 60) {
            return "00:" + String.format("%.1f", time)
        }

        val merchant = time.toInt() / 60
        val remainder = Math.round(time % 60 * 100) / 100.0

        val pre = if (merchant > 9) merchant.toString() else String.format("0%d", merchant)
        val tail = if (remainder > 9) String.format("%.1f", remainder)
        else String.format("0%s", String.format("%.1f", remainder))

        return "$pre:$tail"
    }

    /**
     * 格式化时间
     *
     * @param time 时间
     * @return 时间
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    fun format(time: Int): String {
        if (time < 10) {
            return "00:0$time"
        }
        if (time < 60) {
            return "00:$time"
        }

        val merchant = time / 60
        val remainder = time % 60

        val pre = if (merchant > 9) merchant.toString() else String.format("0%d", merchant)
        val tail = if (remainder > 9) remainder.toString() else String.format("0%d", remainder)

        return "$pre:$tail"
    }
}
