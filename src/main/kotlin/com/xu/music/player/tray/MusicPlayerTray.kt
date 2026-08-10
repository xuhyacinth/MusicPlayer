package com.xu.music.player.tray

import com.xu.music.player.utils.CommUtils
import javafx.application.Platform
import javafx.stage.Stage
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.ActionEvent
import java.awt.image.BufferedImage

/**
 * 通用托盘
 *
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
object MusicPlayerTray {

    private var trayIcon: TrayIcon? = null

    /**
     * 初始化系统托盘
     *
     * @param stage 主窗口
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    fun tray(stage: Stage?) {
        if (stage == null || !SystemTray.isSupported()) {
            return
        }
        try {
            if (trayIcon != null) {
                return
            }

            val awtImage = CommUtils.getImage("main.png")
                ?.let { fromFxImage(it) }
                ?: BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)

            val popup = PopupMenu()
            val showItem = MenuItem("显示主窗口")
            showItem.addActionListener { _: ActionEvent? ->
                Platform.runLater {
                    stage.show()
                    stage.toFront()
                    stage.isIconified = false
                }
            }
            popup.add(showItem)

            val miniItem = MenuItem("最小化")
            miniItem.addActionListener { _: ActionEvent? ->
                Platform.runLater { stage.isIconified = true }
            }
            popup.add(miniItem)

            popup.addSeparator()

            val closeItem = MenuItem("关闭")
            closeItem.addActionListener { _: ActionEvent? ->
                Platform.runLater { stage.close() }
            }
            popup.add(closeItem)

            val icon = TrayIcon(awtImage, "音乐播放器", popup)
            icon.isImageAutoSize = true
            icon.addActionListener { _: ActionEvent? ->
                Platform.runLater {
                    stage.show()
                    stage.toFront()
                    stage.isIconified = false
                }
            }
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
        } catch (e: Exception) {
            // 托盘初始化失败不影响主程序运行
        }
    }

    /**
     * 移除托盘图标
     *
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    fun dispose() {
        try {
            if (trayIcon != null) {
                SystemTray.getSystemTray().remove(trayIcon)
                trayIcon = null
            }
        } catch (e: Exception) {
            // 忽略
        }
    }

    /**
     * JavaFX Image 转 AWT BufferedImage（托盘图标使用）
     *
     * @param fxImage JavaFX 图片
     * @return AWT 图片
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    private fun fromFxImage(fxImage: javafx.scene.image.Image): BufferedImage {
        val width = fxImage.width.toInt().coerceAtLeast(16)
        val height = fxImage.height.toInt().coerceAtLeast(16)
        val canvas = javafx.scene.canvas.Canvas(width.toDouble(), height.toDouble())
        canvas.graphicsContext2D.drawImage(fxImage, 0.0, 0.0)
        val snapshot = canvas.snapshot(null, null)
        val pixelReader = snapshot.pixelReader
        val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                buffered.setRGB(x, y, pixelReader.getArgb(x, y))
            }
        }
        return buffered
    }
}
