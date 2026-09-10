package com.xu.music.player.tray

import java.awt.Dimension
import java.awt.GraphicsConfiguration
import java.awt.MouseInfo
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JDialog
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

/** 使用 Swing 绘制中文，避开 AWT 原生菜单的字符集转换。所有操作均在 AWT 事件线程执行。 */
internal class SwingTrayPopup(internal val menu: JPopupMenu) {
    internal val owner = JDialog().apply {
        isUndecorated = true
        type = Window.Type.POPUP
        isAlwaysOnTop = true
    }

    init {
        val items = menu.components.filterIsInstance<JMenuItem>()
        val labels = items.joinToString("") { it.text }
        val systemFont = Toolkit.getDefaultToolkit().getDesktopProperty("win.menu.font") as? Font
        val font = listOfNotNull(systemFont, Font("Microsoft YaHei UI", Font.PLAIN, 12),
            Font(Font.DIALOG, Font.PLAIN, 12)).firstOrNull { it.canDisplayUpTo(labels) == -1 }
            ?: Font(Font.DIALOG, Font.PLAIN, 12)
        menu.font = font
        items.forEach { it.font = font }
        owner.addWindowFocusListener(object : WindowAdapter() {
            override fun windowLostFocus(event: WindowEvent) {
                menu.isVisible = false
                owner.isVisible = false
            }
        })
        menu.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(event: PopupMenuEvent) = Unit
            override fun popupMenuWillBecomeInvisible(event: PopupMenuEvent) { owner.isVisible = false }
            override fun popupMenuCanceled(event: PopupMenuEvent) { owner.isVisible = false }
        })
    }

    fun showAtPointer() {
        val pointer = MouseInfo.getPointerInfo() ?: return
        // 托盘事件可能使用物理像素；鼠标信息与 Swing 窗口均使用逻辑坐标，不能重复缩放。
        showAt(pointer.location, pointer.device.defaultConfiguration)
    }

    fun showAt(point: Point, config: GraphicsConfiguration =
        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
            .map { it.defaultConfiguration }.firstOrNull { it.bounds.contains(point) }
            ?: owner.graphicsConfiguration) {
        if (menu.isVisible) return
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
        val screen = config.bounds
        val available = Rectangle(screen.x + insets.left, screen.y + insets.top,
            screen.width - insets.left - insets.right, screen.height - insets.top - insets.bottom)
        // 空间不足时向左或向上展开，再限制到工作区，保持菜单紧邻点击位置。
        owner.bounds = placement(point, menu.preferredSize, available)
        owner.isVisible = true
        owner.toFront()
        owner.requestFocus()
        menu.show(owner.contentPane, 0, 0)
    }

    companion object {
        internal fun placement(point: Point, size: Dimension, available: Rectangle): Rectangle {
            val x = if (point.x + size.width <= available.x + available.width) point.x else point.x - size.width
            val y = if (point.y + size.height <= available.y + available.height) point.y else point.y - size.height
            return Rectangle(
                x.coerceIn(available.x, maxOf(available.x, available.x + available.width - size.width)),
                y.coerceIn(available.y, maxOf(available.y, available.y + available.height - size.height)),
                size.width, size.height)
        }
    }

    fun dispose() {
        menu.isVisible = false
        owner.dispose()
    }
}
