package com.xu.music.player.utils;

import java.awt.*;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.core.util.StrUtil;

import com.xu.music.player.constant.Constant;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * 通用工具
 *
 * @since 2024年6月4日19点07分
 * @version swt-java/v1.0.0
 */
public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    static final int CONFIRMATION_STYLE = SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_WARNING;

    private Utils() {

    }

    private static final Map<String, Font> FONT = new HashMap<>();
    private static final Map<String, Image> CACHE = new HashMap<>();

    public static final String FORMAT_DATE = "yyyy-MM-dd";
    public static final String FORMAT_TIME = "HH:mm:ss";
    public static final String FORMAT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";


    /**
     * 绘制颜色
     *
     * @param gc     GC
     * @param x      x
     * @param y      y
     * @param width  宽
     * @param height 高
     * @since 2024年6月4日19点07分
     * @version swt-java/v1.0.0
     */
    public static void draw(GC gc, int x, int y, int width, int height) {
        // 设置条形的颜色
        gc.setBackground(Constant.SPECTRUM_FOREGROUND_COLOR);
        // 绘制条形
        Rectangle draw = new Rectangle(x, y, width, -height);
        gc.fillRectangle(draw);
    }

    /**
     * 获取图片
     *
     * @param name 图片文件名称
     * @return 图片
     * @since 2024年6月4日19点07分
     * @version swt-java/v1.0.0
     */
    public static Image getImage(String name) {
        if (CACHE.containsKey(name)) {
            return CACHE.get(name);
        }

        try {
            InputStream stream = Utils.class.getResourceAsStream("/com/xu/music/player/image/" + name);
            if (stream == null) {
                return null;
            }

            try (InputStream input = stream) {
                Display display = Display.getCurrent();
                ImageData data = new ImageData(input);

                if (data.transparentPixel > 0) {
                    Image image = new Image(display, data, data.getTransparencyMask());
                    CACHE.put(name, image);
                    return image;
                }

                Image image = new Image(display, data);
                CACHE.put(name, image);
                return image;
            }
        } catch (Exception e) {
            log.error("读取图片异常！", e);
        }

        return null;
    }

    /**
     * 消息提示
     *
     * @param shell   Shell
     * @param title   题目
     * @param content 内容
     * @return MessageBox
     * @since 2024年6月4日19点07分
     * @version swt-java/v1.0.0
     */
    public static MessageBox tips(Shell shell, String title, String content) {
        Toolkit.getDefaultToolkit().beep();
        MessageBox message = new MessageBox(shell, CONFIRMATION_STYLE);
        message.setText(title);
        message.setMessage(content);
        return message;
    }

    /**
     * 格式化时间
     *
     * @param time 时间
     * @return 时间
     * @since 2024年6月4日19点07分
     * @version swt-java/v1.0.0
     */
    public static String format(double time) {
        if (time < 10) {
            return StrUtil.format("00:0{}", String.format("%.1f", time));
        }

        if (time < 60) {
            return StrUtil.format("00:{}", String.format("%.1f", time));
        }

        int merchant = (int) time / 60;
        double remainder = Math.round(time % 60 * 100) / 100.0;

        String pre = merchant > 9 ? String.valueOf(merchant) : StrUtil.format("0{}", merchant);
        String tail = remainder > 9 ? String.format("%.1f", remainder)
                : StrUtil.format("0{}", String.format("%.1f", remainder));

        return StrUtil.format("{}:{}", pre, tail);
    }

    /**
     * 格式化时间
     *
     * @param time 时间
     * @return 时间
     * @since 2024年6月4日19点07分
     * @version swt-java/v1.0.0
     */
    public static String format(int time) {
        if (time < 10) {
            return StrUtil.format("00:0{}", time);
        }

        if (time < 60) {
            return StrUtil.format("00:{}", time);
        }

        int merchant = time / 60;
        int remainder = time % 60;

        String pre = merchant > 9 ? String.valueOf(merchant) : StrUtil.format("0{}", merchant);
        String tail = remainder > 9 ? String.valueOf(remainder) : StrUtil.format("0{}", remainder);

        return StrUtil.format("{}:{}", pre, tail);
    }

    /**
     * 获取颜色
     *
     * @param id 颜色码
     * @return 颜色
     * @since 2024年6月4日19点07分
     * @version swt-java/v1.0.0
     */
    public static Color getColor(int id) {
        Display display = Display.getCurrent();
        return display.getSystemColor(id);
    }

    /**
     * 获取字体
     *
     * @param name   名称
     * @param height 高
     * @param style  样式
     * @return 字体
     * @since 2024年6月4日19点07分
     * @version swt-java/v1.0.0
     */
    public static Font getFont(String name, int height, int style) {
        return getFont(name, height, style, false, false);
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
     * @since 2024年6月4日19点07分
     * @version swt-java/v1.0.0
     */
    public static Font getFont(String name, int size, int style, boolean strikeout, boolean underline) {
        String fontName = name + '|' + size + '|' + style + '|' + strikeout + '|' + underline;

        Font font = FONT.get(fontName);
        if (font == null) {
            FontData fontData = new FontData(name, size, style);
            if (strikeout || underline) {
                try {
                    //$NON-NLS-1$
                    Class<?> cls = Class.forName("org.eclipse.swt.internal.win32.LOGFONT");
                    //$NON-NLS-1$
                    Object logFont = FontData.class.getField("data").get(fontData);

                    if (logFont != null) {
                        if (strikeout) {
                            //$NON-NLS-1$
                            cls.getField("lfStrikeOut").set(logFont, Byte.valueOf((byte) 1));
                        }
                        if (underline) {
                            //$NON-NLS-1$
                            cls.getField("lfUnderline").set(logFont, Byte.valueOf((byte) 1));
                        }
                    }
                } catch (Exception e) {
                    log.error("获取字体异常！", e);
                }
            }

            font = new Font(Display.getCurrent(), fontData);
            FONT.put(fontName, font);
        }

        return font;
    }

    /**
     * 时间日期转换字符串
     *
     * @param date 日期
     * @return 字符串
     * @since 2024年6月7日12点55分
     * @since idea
     */
    public static String formatDateTime(Object date) {
        return switch (date) {
            case null -> null;
            case Date d -> d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern(FORMAT_DATE_TIME));
            case LocalDate ld -> ld.format(DateTimeFormatter.ofPattern(FORMAT_DATE));
            case LocalTime lt -> lt.format(DateTimeFormatter.ofPattern(FORMAT_TIME));
            case LocalDateTime ldt -> ldt.format(DateTimeFormatter.ofPattern(FORMAT_DATE_TIME));
            default -> null;
        };
    }

}
