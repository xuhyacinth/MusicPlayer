package com.xu.music.player.tray

import com.xu.music.player.taskbar.TaskbarLyrics
import com.xu.music.player.utils.CommUtils
import javafx.application.Platform
import javafx.stage.Stage
import java.awt.EventQueue
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
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
    private var trayPopup: SwingTrayPopup? = null

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
        val awtImage = CommUtils.getImage("main.png")
            ?.let { fromFxImage(it) }
            ?: BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        onAwt {
            try {
                if (trayIcon != null) return@onAwt
                val popup = JPopupMenu()
                val showItem = JMenuItem("显示主窗口")
                showItem.addActionListener { _: ActionEvent? ->
                    Platform.runLater {
                        stage.show()
                        stage.toFront()
                        stage.isIconified = false
                    }
                }
                popup.add(showItem)

                val miniItem = JMenuItem("最小化")
                miniItem.addActionListener { _: ActionEvent? ->
                    Platform.runLater { stage.isIconified = true }
                }
                popup.add(miniItem)

                popup.addSeparator()

                if (TaskbarLyrics.supported) {
                    val lyricsItem = JCheckBoxMenuItem("任务栏歌词", false)
                    lyricsItem.addActionListener {
                        TaskbarLyrics.setEnabled(lyricsItem.isSelected) { message ->
                            lyricsItem.isSelected = false
                            trayIcon?.displayMessage("任务栏歌词不可用", message, TrayIcon.MessageType.ERROR)
                        }
                    }
                    popup.add(lyricsItem)
                    val lockItem = JCheckBoxMenuItem("锁定歌词位置（鼠标穿透）", true)
                    lockItem.addActionListener { TaskbarLyrics.setLocked(lockItem.isSelected) }
                    popup.add(lockItem)
                    popup.add(JMenuItem("歌词变窄").apply { addActionListener { TaskbarLyrics.changeWidth(-40) } })
                    popup.add(JMenuItem("歌词变宽").apply { addActionListener { TaskbarLyrics.changeWidth(40) } })
                    popup.add(JMenuItem("重置歌词位置和宽度").apply { addActionListener { TaskbarLyrics.resetPosition() } })
                    popup.addSeparator()
                }

                val closeItem = JMenuItem("关闭")
                closeItem.addActionListener { _: ActionEvent? ->
                    Platform.runLater { stage.close() }
                }
                popup.add(closeItem)

                val menu = SwingTrayPopup(popup)
                trayPopup = menu
                val icon = TrayIcon(awtImage, "音乐播放器")
                icon.addMouseListener(object : MouseAdapter() {
                    override fun mousePressed(event: MouseEvent) {
                        if (event.isPopupTrigger) menu.showAtPointer()
                    }
                    override fun mouseReleased(event: MouseEvent) {
                        if (event.isPopupTrigger) menu.showAtPointer()
                    }
                })
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
                trayPopup?.dispose()
                trayPopup = null
                // 托盘初始化失败不影响主程序运行
            }
        }
    }

    /**
     * 移除托盘图标
     *
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    fun dispose() {
        TaskbarLyrics.dispose()
        onAwt {
            trayPopup?.dispose()
            trayPopup = null
            trayIcon?.let { SystemTray.getSystemTray().remove(it) }
            trayIcon = null
        }
    }

    private fun onAwt(action: () -> Unit) {
        if (EventQueue.isDispatchThread()) action() else EventQueue.invokeLater(action)
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
