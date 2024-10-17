package com.xu.music.player.utils

import cn.hutool.core.util.StrUtil
import com.xu.music.player.constant.Constant
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.FontData
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.ImageData
import org.eclipse.swt.graphics.Rectangle
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell
import org.slf4j.LoggerFactory
import java.awt.Toolkit
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 通用工具
 *
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
object Utils {

    val log = LoggerFactory.getLogger("Utils")

    private val FONT: MutableMap<String, Font> = HashMap()
    private val CACHE: MutableMap<String, Image> = HashMap()

    private const val FORMAT_DATE: String = "yyyy-MM-dd"
    private const val FORMAT_TIME: String = "HH:mm:ss"
    const val FORMAT_DATE_TIME: String = "yyyy-MM-dd HH:mm:ss"


    /**
     * 绘制颜色
     *
     * @param gc     GC
     * @param x      x
     * @param y      y
     * @param width  宽
     * @param height 高
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    @JvmStatic
    fun draw(gc: GC, x: Int, y: Int, width: Int, height: Int) {
        // 设置条形的颜色
        gc.background = Constant.SPECTRUM_FOREGROUND_COLOR
        // 绘制条形
        val draw = Rectangle(x, y, width, -height)
        gc.fillRectangle(draw)
    }

    /**
     * 获取图片
     *
     * @param name 图片文件名称
     * @return 图片
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    @JvmStatic
    fun getImage(name: String): Image? {
        val path = Paths.get(StrUtil.format("src/main/kotlin/com/xu/music/player/image/{}", name))
        if (!path.toFile().exists()) {
            return null
        }

        if (CACHE.containsKey(name)) {
            return CACHE[name]
        }

        try {
            val stream = Files.newInputStream(path)
            val display = Display.getCurrent()
            val data = ImageData(stream)

            if (data.transparentPixel > 0) {
                val image = Image(display, data, data.transparencyMask)
                CACHE[name] = image
                return image
            }

            val image = Image(display, data)
            CACHE[name] = image
            return image
        } catch (e: Exception) {
            Utils.log.error("读取图片异常！", e)
        }

        return null
    }

    /**
     * 消息提示
     *
     * @param shell   Shell
     * @param title   题目
     * @param content 内容
     * @return MessageBox
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    @JvmStatic
    fun tips(shell: Shell?, title: String?, content: String?): MessageBox {
        Toolkit.getDefaultToolkit().beep()
        val message = MessageBox(shell, SWT.YES or SWT.ICON_WARNING or SWT.NO)
        message.text = title
        message.message = content
        return message
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
            return StrUtil.format("00:0{}", String.format("%.1f", time))
        }

        if (time < 60) {
            return StrUtil.format("00:{}", String.format("%.1f", time))
        }

        val merchant = time.toInt() / 60
        val remainder = Math.round(time % 60 * 100) / 100.0

        val pre = if (merchant > 9) merchant.toString() else StrUtil.format("0{}", merchant)
        val tail = if (remainder > 9) String.format("%.1f", remainder) else StrUtil.format(
            "0{}",
            String.format("%.1f", remainder)
        )

        return StrUtil.format("{}:{}", pre, tail)
    }

    /**
     * 格式化时间
     *
     * @param time 时间
     * @return 时间
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    @JvmStatic
    fun format(time: Int): String {
        if (time < 10) {
            return StrUtil.format("00:0{}", time)
        }

        if (time < 60) {
            return StrUtil.format("00:{}", time)
        }

        val merchant = time / 60
        val remainder = time % 60

        val pre = if (merchant > 9) merchant.toString() else StrUtil.format("0{}", merchant)
        val tail = if (remainder > 9) remainder.toString() else StrUtil.format("0{}", remainder)

        return StrUtil.format("{}:{}", pre, tail)
    }

    /**
     * 获取颜色
     *
     * @param id 颜色码
     * @return 颜色
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    @JvmStatic
    fun getColor(id: Int): Color {
        val display = Display.getCurrent()
        return display.getSystemColor(id)
    }

    /**
     * 获取字体
     *
     * @param name   名称
     * @param height 高
     * @param style  样式
     * @return 字体
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    @JvmStatic
    fun getFont(name: String, height: Int, style: Int): Font {
        return getFont(name, height, style, false, false)
    }

    /**
     * 获取字体
     *
     * @param name      名称
     * @param size      大小
     * @param style     样式
     * @param strikeout strikeout
     * @param underline 下划线
     * @return 字体
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    fun getFont(name: String, size: Int, style: Int, strikeout: Boolean, underline: Boolean): Font {
        val fontName = "$name|$size|$style|$strikeout|$underline"

        var font = FONT[fontName]
        if (font == null) {
            val fontData = FontData(name, size, style)
            if (strikeout || underline) {
                try {
                    //$NON-NLS-1$
                    val cls = Class.forName("org.eclipse.swt.internal.win32.LOGFONT")
                    //$NON-NLS-1$
                    val logFont = FontData::class.java.getField("data")[fontData]

                    if (logFont != null) {
                        if (strikeout) {
                            //$NON-NLS-1$
                            cls.getField("lfStrikeOut")[logFont] = 1.toByte()
                        }
                        if (underline) {
                            //$NON-NLS-1$
                            cls.getField("lfUnderline")[logFont] = 1.toByte()
                        }
                    }
                } catch (e: Exception) {
                    Utils.log.error("获取字体异常！", e)
                }
            }

            font = Font(Display.getCurrent(), fontData)
            FONT[fontName] = font
        }

        return font
    }

    /**
     * 时间日期转换字符串
     *
     * @param date 日期
     * @return 字符串
     * @date 2024年6月7日12点55分
     * @since idea
     */
    @JvmStatic
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
