package com.xu.music.player.utils;

import java.awt.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;

import cn.hutool.core.util.StrUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * 通用工具
 *
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
@Slf4j
public class Utils {

    private static final Map<String, Image> CACHE = new HashMap<>();
    public static final String FORMAT_DATE = "yyyy-MM-dd";
    public static final String FORMAT_TIME = "HH:mm:ss";
    public static final String FORMAT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    public static Image getImage(String name) {
        Path path = Paths.get("src/main/java/com/xu/music/player/image/" + name);
        if (!path.toFile().exists()) {
            return null;
        }

        if (CACHE.containsKey(name)) {
            return CACHE.get(name);
        }

        try {
            InputStream stream = Files.newInputStream(path);
            Display display = Display.getCurrent();
            ImageData data = new ImageData(stream);

            if (data.transparentPixel > 0) {
                Image image = new Image(display, data, data.getTransparencyMask());
                CACHE.put(name, image);
                return image;
            }

            Image image = new Image(display, data);
            CACHE.put(name, image);
            return image;
        } catch (Exception e) {
            log.error("读取图片异常！", e);
        }

        return null;
    }

    public static MessageBox tips(Shell shell, String title, String content) {
        Toolkit.getDefaultToolkit().beep();
        MessageBox message = new MessageBox(shell, SWT.YES | SWT.ICON_WARNING | SWT.NO);
        message.setText(title);
        message.setMessage(content);
        return message;
    }

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
     * 时间日期转换字符串
     *
     * @param date 日期
     * @return 字符串
     * @date 2024年6月7日12点55分
     * @since idea
     */
    public static String formatDateTime(Object date) {
        if (null == date) {
            return null;
        }
        switch (date.getClass().getSimpleName()) {
            case "Date":
                return ((Date) date).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern(FORMAT_DATE_TIME));
            case "LocalDate":
                return ((LocalDate) date).format(DateTimeFormatter.ofPattern(FORMAT_DATE));
            case "LocalTime":
                return ((LocalTime) date).format(DateTimeFormatter.ofPattern(FORMAT_TIME));
            case "LocalDateTime":
                return ((LocalDateTime) date).format(DateTimeFormatter.ofPattern(FORMAT_DATE_TIME));
            default:
                return null;
        }
    }

}
